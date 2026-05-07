package org.example.qasystem.service.impl;


import org.example.qasystem.model.IndexedChunk;
import org.example.qasystem.model.RetrievedChunk;
import org.example.qasystem.service.EmbeddingService;
import org.example.qasystem.service.VectorSearchService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Profile("milvus")
@Primary
@Slf4j
public class VectorSearchServiceImpl implements VectorSearchService {

    /** 略增大窗口，减少块数量与嵌入调用次数，降低内存与耗时 */
    private static final int CHUNK_SIZE = 480;
    private static final int CHUNK_OVERLAP = 96;

    /** 单篇正文参与建索引的最大字符数（超出截断，优先保证进程不 OOM） */
    private static final int MAX_INDEX_TEXT_LENGTH = 280_000;

    private final EmbeddingService embeddingService;
    private final Map<Long, List<IndexedChunk>> fallbackIndex = new ConcurrentHashMap<>();

    private static int lineNumberAt(String text, int charIndex) {
        if (text == null || text.isEmpty()) {
            return 1;
        }
        int idx = Math.max(0, Math.min(charIndex, text.length()));
        int line = 1;
        for (int i = 0; i < idx; i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    static float[] toFloatVector(List<Double> embedding) {
        if (embedding == null || embedding.isEmpty()) {
            return new float[0];
        }
        float[] out = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            out[i] = embedding.get(i).floatValue();
        }
        return out;
    }

    public VectorSearchServiceImpl(@Qualifier("openAiEmbeddingService") EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    /** 流式滑动窗口切块并嵌入，避免先把所有块文本放进一个大 List 再处理（大文档峰值更低）。 */
    @Override
    public void indexFile(org.example.qasystem.domain.File file) {
        if (file == null || file.getContent() == null) {
            return;
        }

        String full = file.getContent();
        if (full.length() > MAX_INDEX_TEXT_LENGTH) {
            full = full.substring(0, MAX_INDEX_TEXT_LENGTH);
        }

        List<IndexedChunk> indexedChunks = new ArrayList<>();
        int start = 0;
        int textLength = full.length();
        int chunkSeq = 0;

        while (start < textLength) {
            int end = Math.min(start + CHUNK_SIZE, textLength);
            if (end < textLength) {
                int lastSpace = full.lastIndexOf(' ', end);
                if (lastSpace > start + CHUNK_SIZE / 2) {
                    end = lastSpace;
                }
            }
            String raw = full.substring(start, end);
            String chunkText = raw.trim();
            if (!chunkText.isEmpty()) {
                int leadWs = 0;
                while (leadWs < raw.length() && Character.isWhitespace(raw.charAt(leadWs))) {
                    leadWs++;
                }
                int spanStart = start + leadWs;
                int spanEndExclusive = spanStart + chunkText.length();

                List<Double> embList = embeddingService.embed(chunkText);
                float[] vec = toFloatVector(embList);
                embList = null;

                if (vec.length == 0) {
                    log.warn("empty embedding for fileId={}, skip chunk", file.getFileId());
                } else {
                    int startLine = lineNumberAt(full, spanStart);
                    int lastChar = Math.max(spanStart, spanEndExclusive - 1);
                    int endLine = lineNumberAt(full, lastChar);

                    IndexedChunk indexedChunk = new IndexedChunk();
                    indexedChunk.setFileId(file.getFileId());
                    indexedChunk.setTitle(file.getTitle());
                    indexedChunk.setCategory(file.getCategory());
                    indexedChunk.setContent(chunkText);
                    indexedChunk.setVector(vec);
                    indexedChunk.setChunkIndex(chunkSeq);
                    indexedChunk.setStartLine(startLine);
                    indexedChunk.setEndLine(Math.max(startLine, endLine));

                    indexedChunks.add(indexedChunk);
                    chunkSeq++;
                }
            }
            start = end - CHUNK_OVERLAP;
            if (start < 0) {
                start = 0;
            }
        }

        fallbackIndex.put(file.getFileId(), indexedChunks);
    }

    @Override
    public void reindexAll(List<org.example.qasystem.domain.File> files) {
        fallbackIndex.clear();
        if (files == null) {
            return;
        }
        for (org.example.qasystem.domain.File file : files) {
            try {
                indexFile(file);
            } catch (Exception e) {
                log.warn("reindex skip fileId={} title={}: {}", file.getFileId(), file.getTitle(), e.getMessage());
            }
        }
    }

    @Override
    public int countIndexedChunks() {
        int n = 0;
        for (List<IndexedChunk> list : fallbackIndex.values()) {
            if (list != null) {
                n += list.size();
            }
        }
        return n;
    }

    @Override
    public List<RetrievedChunk> search(String query, String category, int topK) {
        List<RetrievedChunk> results = new ArrayList<>();

        float[] queryVector = toFloatVector(embeddingService.embed(query));
        if (queryVector.length == 0) {
            return results;
        }

        List<SimilarityResult> similarityResults = new ArrayList<>();

        for (Map.Entry<Long, List<IndexedChunk>> entry : fallbackIndex.entrySet()) {
            List<IndexedChunk> chunks = entry.getValue();
            for (IndexedChunk chunk : chunks) {
                if (hasCategoryFilter(category) && !isCategoryMatched(category, chunk.getCategory())) {
                    continue;
                }
                double similarity = cosineSimilarity(queryVector, chunk.getVector());
                similarityResults.add(new SimilarityResult(chunk, similarity));
            }
        }

        similarityResults.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));

        int count = 0;
        for (SimilarityResult result : similarityResults) {
            if (count >= topK) {
                break;
            }

            IndexedChunk chunk = result.getChunk();
            RetrievedChunk retrievedChunk = new RetrievedChunk();
            retrievedChunk.setFileId(chunk.getFileId());
            retrievedChunk.setTitle(chunk.getTitle());
            retrievedChunk.setCategory(chunk.getCategory());
            retrievedChunk.setContent(chunk.getContent());
            retrievedChunk.setScore(result.getSimilarity());
            retrievedChunk.setChunkIndex(chunk.getChunkIndex());
            retrievedChunk.setStartLine(chunk.getStartLine());
            retrievedChunk.setEndLine(chunk.getEndLine());

            results.add(retrievedChunk);
            count++;
        }

        return results;
    }

    private boolean hasCategoryFilter(String category) {
        return category != null && !category.trim().isEmpty();
    }

    private boolean isCategoryMatched(String filter, String currentCategory) {
        if (currentCategory == null) {
            return false;
        }
        return currentCategory.trim().toLowerCase(Locale.ROOT)
                .equals(filter.trim().toLowerCase(Locale.ROOT));
    }

    private static double cosineSimilarity(float[] vector1, float[] vector2) {
        if (vector1 == null || vector2 == null || vector1.length != vector2.length || vector1.length == 0) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.length; i++) {
            double v1 = vector1[i];
            double v2 = vector2[i];
            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }

        norm1 = Math.sqrt(norm1);
        norm2 = Math.sqrt(norm2);

        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }

        return dotProduct / (norm1 * norm2);
    }

    private static class SimilarityResult {
        private final IndexedChunk chunk;
        private final double similarity;

        SimilarityResult(IndexedChunk chunk, double similarity) {
            this.chunk = chunk;
            this.similarity = similarity;
        }

        IndexedChunk getChunk() {
            return chunk;
        }

        double getSimilarity() {
            return similarity;
        }
    }
}
