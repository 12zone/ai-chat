package org.example.qasystem.config;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.example.qasystem.service.IngestionService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 启动时从数据库加载未删除文件，重建内存向量索引（与 {@code VectorSearchServiceImpl} 的 fallback 一致）。
 * 在后台线程执行，避免阻塞应用就绪后的首个 HTTP 请求（如登录）导致代理 ECONNRESET。
 */
@Component
@Profile("milvus")
@Slf4j
public class VectorIndexBootstrap implements ApplicationListener<ApplicationReadyEvent> {

    private static final Executor INDEX_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "vector-index-bootstrap");
        t.setDaemon(true);
        return t;
    });

    private final IngestionService ingestionService;

    public VectorIndexBootstrap(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        CompletableFuture.runAsync(this::reindexFromDb, INDEX_EXECUTOR);
    }

    private void reindexFromDb() {
        try {
            Map<String, Object> r = ingestionService.rebuildVectorIndexFromDatabase();
            log.info("向量索引已从数据库重建（内存），文件数={}，向量块数={}",
                    r.get("dbFileCount"), r.get("indexedChunkCount"));
        } catch (Exception e) {
            log.warn("启动时向量索引重建失败（OpenAI 嵌入或库无文件等）: {}", e.getMessage());
        }
    }
}
