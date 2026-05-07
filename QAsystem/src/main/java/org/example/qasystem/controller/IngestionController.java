package org.example.qasystem.controller;


import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.example.qasystem.domain.File;
import org.example.qasystem.service.IngestionService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ingestion")
@CrossOrigin(origins = "*")
public class IngestionController {
    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/json")
    public List<org.example.qasystem.domain.File> importFromJson(@RequestParam("resourcePath") String resourcePath) {
        return ingestionService.ingest(resourcePath);
    }

    @PostMapping("/upload")
    public File uploadFile(@RequestParam("file") MultipartFile file, @RequestParam(defaultValue = "default") String category) throws IOException {
        return ingestionService.ingest(file, category);
    }

    @PostMapping("/upload/batch")
    public List<File> uploadFiles(@RequestParam("files") MultipartFile[] files,
                                  @RequestParam(defaultValue = "default") String category) {
        return ingestionService.ingestBatch(files, category);
    }

    /** 管理员：从数据库全量重建内存向量索引（依赖 OpenAI 嵌入，见 spring.ai.openai） */
    @PostMapping("/reindex")
    public Map<String, Object> reindexFromDatabase() {
        return ingestionService.rebuildVectorIndexFromDatabase();
    }

    /** 管理员：查看当前内存向量块数量与库中文件数 */
    @GetMapping("/index-stats")
    public Map<String, Object> indexStats() {
        return ingestionService.vectorIndexStats();
    }

}
