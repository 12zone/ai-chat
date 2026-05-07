package org.example.qasystem.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.qasystem.domain.File;
import org.example.qasystem.model.IngestionJsonItem;
import org.example.qasystem.service.FileService;
import org.example.qasystem.service.IngestionService;
import org.example.qasystem.service.VectorSearchService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

import java.io.FileReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class IngestionServiceImpl implements IngestionService {

    /** 每批只加载 1 个文件，峰值内存最低 */
    private static final int REINDEX_PAGE_SIZE = 1;

    /** 与启动引导任务互斥，避免两次全量重建并发把堆打爆 */
    private final Object fullReindexLock = new Object();

    private final FileService fileService;
    private final VectorSearchService vectorSearchService;
    private final ObjectMapper objectMapper;

    public IngestionServiceImpl(FileService fileService, VectorSearchService vectorSearchService) {
        this.fileService = fileService;
        this.vectorSearchService = vectorSearchService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Map<String, Object> rebuildVectorIndexFromDatabase() {
        synchronized (fullReindexLock) {
            vectorSearchService.reindexAll(Collections.emptyList());

            long dbFileCount = fileService.countNonDeletedFiles();
            int filesProcessed = 0;
            int filesSkippedEmptyContent = 0;
            int filesSkippedException = 0;
            int page = 0;
            while (true) {
                Page<File> batch = fileService.getFilesByPage(page, REINDEX_PAGE_SIZE);
                if (!batch.hasContent()) {
                    break;
                }
                for (File file : batch.getContent()) {
                    filesProcessed++;
                    String content = file.getContent();
                    if (content == null || content.isBlank()) {
                        filesSkippedEmptyContent++;
                        file.setContent(null);
                        continue;
                    }
                    try {
                        vectorSearchService.indexFile(file);
                    } catch (Exception e) {
                        filesSkippedException++;
                        log.warn("reindex skip fileId={} title={}: {}", file.getFileId(), file.getTitle(), e.getMessage());
                    }
                    file.setContent(null);
                }
                if (!batch.hasNext()) {
                    break;
                }
                page++;
            }

            int indexedChunkCount = vectorSearchService.countIndexedChunks();
            log.info(
                    "向量索引重建完成 dbFileCount={} indexedChunkCount={} filesProcessed={} skippedEmptyContent={} skippedException={}",
                    dbFileCount, indexedChunkCount, filesProcessed, filesSkippedEmptyContent, filesSkippedException);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("dbFileCount", dbFileCount);
            m.put("indexedChunkCount", indexedChunkCount);
            m.put("filesProcessed", filesProcessed);
            m.put("filesSkippedEmptyContent", filesSkippedEmptyContent);
            m.put("filesSkippedException", filesSkippedException);
            m.put("message", "已从数据库全量重建内存索引（单文件分页 + 互斥锁 + float 向量）；见 filesSkipped* 与日志 reindex skip。");
            return m;
        }
    }

    @Override
    public Map<String, Object> vectorIndexStats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("dbFileCount", fileService.countNonDeletedFiles());
        m.put("indexedChunkCount", vectorSearchService.countIndexedChunks());
        return m;
    }

    @Override
    public List<File> ingest(String resourcePath){
        try {
            // 读取JSON文件
            List<IngestionJsonItem> items = objectMapper.readValue(
                    new FileReader(resourcePath),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, IngestionJsonItem.class)
            );

            List<File> importedFiles = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            // 处理每个JSON项
            for (IngestionJsonItem item : items) {
                File file = new File();
                file.setTitle(item.getTitle());
                file.setCategory(item.getCategory());
                file.setContent(item.getContent().toString());
                
                // 解析日期
                if (item.getDate() != null) {
                    file.setDate(LocalDate.parse(item.getDate(), formatter));
                } else {
                    file.setDate(LocalDate.now());
                }

                // 存储文件并索引
                File savedFile = fileService.createFile(file);
                importedFiles.add(savedFile);
            }

            return importedFiles;
        }catch(Exception e){
            throw new IllegalStateException("读文件失败" + resourcePath, e);
        }
    }

    @Override
    public org.example.qasystem.domain.File ingest(MultipartFile file, String category){
        try{
            String originalFilename = file.getOriginalFilename() == null ? "uploaded-file" : file.getOriginalFilename();
            String content = extractTextFromFile(file, originalFilename);
            // 创建File对象
            org.example.qasystem.domain.File newFile = new org.example.qasystem.domain.File();
            newFile.setTitle(originalFilename);
            newFile.setContent(content);
            newFile.setCategory(category);
            newFile.setDate(java.time.LocalDate.now());
            
            // 存储文件并索引
            return fileService.createFile(newFile);
        }catch(Exception e){
            throw new IllegalStateException("文件导入失败", e);
        }
    }

    @Override
    public List<File> ingestBatch(MultipartFile[] files, String category) {
        List<File> results = new ArrayList<>();
        if (files == null || files.length == 0) {
            return results;
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            results.add(ingest(file, category));
        }
        return results;
    }



    private String extractTextFromFile(MultipartFile file, String fileName) {
        try (java.io.InputStream fis = file.getInputStream()) {
            int dotIndex = fileName.lastIndexOf('.');
            String extension = dotIndex >= 0 ? fileName.substring(dotIndex).toLowerCase() : "";
            
            switch (extension) {
                case ".pdf":
                    return extractTextFromPdf(fis);
                case ".docx":
                    return extractTextFromDocx(fis);
                case ".xlsx":
                    return extractTextFromXlsx(fis);
                default:
                    // 默认为文本文件
                    byte[] bytes = file.getBytes();
                    return new String(bytes);
            }
        } catch (Exception e) {
            throw new IllegalStateException("文本提取失败", e);
        }
    }

    private String extractTextFromPdf(java.io.InputStream inputStream) throws Exception {
        try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(inputStream)) {
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractTextFromDocx(java.io.InputStream inputStream) throws Exception {
        try (org.apache.poi.xwpf.usermodel.XWPFDocument document = new org.apache.poi.xwpf.usermodel.XWPFDocument(inputStream);
             org.apache.poi.xwpf.extractor.XWPFWordExtractor extractor = new org.apache.poi.xwpf.extractor.XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractTextFromXlsx(java.io.InputStream inputStream) throws Exception {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(inputStream)) {
            StringBuilder text = new StringBuilder();
            
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                org.apache.poi.xssf.usermodel.XSSFSheet sheet = workbook.getSheetAt(i);
                text.append("Sheet: ").append(sheet.getSheetName()).append("\n");
                
                for (org.apache.poi.ss.usermodel.Row row : sheet) {
                    for (org.apache.poi.ss.usermodel.Cell cell : row) {
                        switch (cell.getCellType()) {
                            case STRING -> text.append(cell.getStringCellValue()).append("\t");
                            case NUMERIC -> text.append(cell.getNumericCellValue()).append("\t");
                            case BOOLEAN -> text.append(cell.getBooleanCellValue()).append("\t");
                            default -> {}
                        }
                    }
                    text.append("\n");
                }
                text.append("\n");
            }
            
            return text.toString();
        }
    }

}
