package org.example.qasystem.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import org.example.qasystem.domain.File;
import java.util.List;
import org.springframework.data.domain.Page;

public interface FileService {
    File storeFile(MultipartFile file) throws IOException;
    List<File> getAllFiles();

    /** 未删除文件总数（用于统计，避免 {@link #getAllFiles()} 一次性加载全部正文导致 OOM） */
    long countNonDeletedFiles();
    String buildSystemPrompt(String category);

    File createFile(File file);
    File updateFile(Long fileID, File fileDetails);
    void deleteFile(Long fileID);
    List<File> findFilesByCategory(String category);
    List<File> getDeletedFiles();
    List<String> getCategory();
    List<Long> getFileIds();
    List<String> getTitles();
    File findFileById(Long fileId);
    Page<File> getFilesByPage(int page, int size);
    File findOneById(Long id);
    List<File> findOneByCategory(String category);
    Page<File> findTwoByCategory(String category, int page, int size);
    Page<File> searchFilesByPage(int type, String value, int page, int size);
    void deleteAllFiles();

}
