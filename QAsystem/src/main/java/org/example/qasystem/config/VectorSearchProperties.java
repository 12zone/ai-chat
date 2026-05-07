package org.example.qasystem.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.vector-search")
public class VectorSearchProperties {
    private String provider = "memory";
    private int topK = 3;
    private int dimension = 64;
    private String milvusUrl = "./data/milvus-lite.db";
    private String collectionName = "file-chunks";
}
