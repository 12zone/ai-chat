package org.example.qasystem.service;

import org.example.qasystem.model.RetrievedChunk;

import org.example.qasystem.domain.File;
import java.util.List;

public interface VectorSearchService {
    void indexFile(File file);
    void reindexAll(List<File> files);
    List<RetrievedChunk> search(String query, String category, int topK);

    /**
     * 当前进程内存索引中的向量块总数（{@code milvus} profile 实现有效；其它实现可返回 0）。
     */
    default int countIndexedChunks() {
        return 0;
    }
}
