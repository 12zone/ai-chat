package org.example.qasystem.service.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.example.qasystem.domain.File;
import org.example.qasystem.repository.FileRepository;
import org.example.qasystem.service.FileService;
import org.example.qasystem.service.VectorSearchService;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityNotFoundException;
import java.util.Collections;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileServiceImpl implements FileService {

    private static final Sort FILE_SORT_ASC = Sort.by(Sort.Direction.ASC, "fileId");

    private final FileRepository repo;
    private final VectorSearchService vectorSearchService;

    public FileServiceImpl(FileRepository repo, VectorSearchService vectorSearchService) {
        this.repo = repo;
        this.vectorSearchService = vectorSearchService;
    }

    @Override
    public File storeFile(MultipartFile file) throws IOException{
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        File newFile = new File();
        newFile.setTitle(fileName);
        newFile.setContent(new String(file.getBytes()));
        newFile.setDate(LocalDate.now());
        newFile.setCategory("default");

        File savedFile = repo.save(newFile);
        vectorSearchService.indexFile(savedFile);
        return savedFile;
    }

    @Override
    public List<File> getAllFiles(){
        return repo.findByIsDeletedFalse(FILE_SORT_ASC);
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public long countNonDeletedFiles() {
        return repo.countByIsDeletedFalse();
    }

    @Override
    public File createFile(File file){
        if(file.getDate()==null){
            file.setDate(LocalDate.now());
        }
        File saved = repo.save(file);
        vectorSearchService.indexFile(saved);
        return saved;
    }

    @Override
    public File updateFile(Long fileID,File fileDetails){
        File existing = repo.findById(fileID)
                .orElseThrow(()->new EntityNotFoundException("File Not Found"));
        if (fileDetails.getTitle() != null) {
            existing.setTitle(fileDetails.getTitle());
        }
        if (fileDetails.getContent() != null) {
            existing.setContent(fileDetails.getContent());
        }
        if (fileDetails.getCategory() != null) {
            existing.setCategory(fileDetails.getCategory());
        }
        existing.setDate(fileDetails.getDate() == null ? LocalDate.now() : fileDetails.getDate());
        File saved = repo.save(existing);
        vectorSearchService.indexFile(saved);
        return saved;
    }

    @Override
    public void deleteFile(Long fileID){
        File existing = repo.findById(fileID)
                .orElseThrow(()->new EntityNotFoundException("File not found"));
        existing.setDeleted(true);
        repo.save(existing);
    }
    //有问题

    @Override
    public List<File> findFilesByCategory(String category){
        return repo.findByCategoryAndIsDeletedFalse(category, FILE_SORT_ASC);
    }

    @Override
    public List<File> getDeletedFiles(){
        return repo.findByIsDeletedTrue(FILE_SORT_ASC);
    }

    @Override
    public List<String> getCategory(){
        return repo.findDistinctCategories();
    }

    @Override
    public List<Long> getFileIds() {
        return repo.findDistinctFileIds();
    }

    @Override
    public List<String> getTitles() {
        return repo.findDistinctTitles();
    }

    @Override
    public File findFileById(Long fileId) {
        return repo.findById(fileId)
                .orElseThrow(() -> new EntityNotFoundException("File Not Found"));
    }

    @Override
    public String buildSystemPrompt(String category) {
        // 构建系统提示，根据文件类别
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的问答助手，根据以下文件内容回答问题。\n");
        
        if (category != null && !category.isEmpty()) {
            prompt.append("文件类别：").append(category).append("\n");
        }
        
        prompt.append("请基于文件内容提供准确、专业的回答。\n");
        prompt.append("如果问题超出文件内容范围，请明确说明无法回答。");
        
        return prompt.toString();
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Page<File> getFilesByPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, FILE_SORT_ASC);
        return repo.findByIsDeletedFalse(pageable);
    }

    @Override
    public File findOneById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("File Not Found"));
    }

    @Override
    public List<File> findOneByCategory(String category) {
        return repo.findByCategoryAndIsDeletedFalse(category, FILE_SORT_ASC);
    }

    @Override
    public Page<File> findTwoByCategory(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, FILE_SORT_ASC);
        return repo.findByCategoryAndIsDeletedFalse(category, pageable);
    }

    @Override
    public Page<File> searchFilesByPage(int type, String value, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, FILE_SORT_ASC);
        String trimmedValue = value == null ? null : value.trim();
        if (type == 1) {
            if (trimmedValue == null || trimmedValue.isEmpty()) {
                return Page.empty(pageable);
            }
            try {
                Long fileId = Long.parseLong(trimmedValue);
                return repo.findByFileIdAndIsDeletedFalse(fileId)
                        .map(file -> new PageImpl<>(Collections.singletonList(file), pageable, 1))
                        .orElseGet(() -> new PageImpl<>(Collections.emptyList(), pageable, 0));
            } catch (NumberFormatException ex) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
        }
        if (type == 2) {
            if (trimmedValue == null || trimmedValue.isEmpty()) {
                return Page.empty(pageable);
            }
            return repo.findByCategoryAndIsDeletedFalse(trimmedValue, pageable);
        }
        if (type == 3) {
            if (trimmedValue == null || trimmedValue.isEmpty()) {
                return Page.empty(pageable);
            }
            return repo.findByTitleAndIsDeletedFalse(trimmedValue, pageable);
        }
        return repo.findByIsDeletedFalse(pageable);
    }

    @Override
    public void deleteAllFiles() {
        repo.deleteAll();
    }

}
