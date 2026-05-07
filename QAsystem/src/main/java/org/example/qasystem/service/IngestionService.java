package org.example.qasystem.service;

import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

public interface IngestionService {
    List<org.example.qasystem.domain.File> ingest(String resourcePath);
    org.example.qasystem.domain.File ingest(MultipartFile file, String category);
    List<org.example.qasystem.domain.File> ingestBatch(MultipartFile[] files, String category);

    /**
     * 从数据库读取未删除文件，全量重建内存向量索引（需 OpenAI 嵌入可用，见配置）。
     */
    Map<String, Object> rebuildVectorIndexFromDatabase();

    /** 当前向量索引统计（内存块数、库中文件数等） */
    Map<String, Object> vectorIndexStats();
}
