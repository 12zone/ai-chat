package org.example.qasystem.service.impl;

import org.example.qasystem.service.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("openAiEmbeddingService")
public class OpenAiEmbeddingService implements EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiEmbeddingService.class);

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${app.embedding.openai.model:text-embedding-ada-002}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    private String embeddingsUrl() {
        String raw = baseUrl == null ? "" : baseUrl.trim();
        if (raw.isEmpty()) {
            raw = "https://api.openai.com/v1";
        }
        if (raw.endsWith("/")) {
            raw = raw.substring(0, raw.length() - 1);
        }
        return raw + "/embeddings";
    }

    @Override
    public List<Double> embed(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("未配置 spring.ai.openai.api-key，无法调用 OpenAI 嵌入接口");
        }

        String url = embeddingsUrl();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey.trim());
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("input", text);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("data")) {
                List<?> dataList = (List<?>) responseBody.get("data");
                if (dataList != null && !dataList.isEmpty()) {
                    Object firstItemObj = dataList.get(0);
                    if (firstItemObj instanceof Map<?, ?> firstItem) {
                        if (firstItem.containsKey("embedding")) {
                            Object embeddingObj = firstItem.get("embedding");
                            if (embeddingObj instanceof List<?> embeddingList) {
                                List<Double> embedding = new ArrayList<>();
                                for (Object item : embeddingList) {
                                    if (item instanceof Number number) {
                                        embedding.add(number.doubleValue());
                                    }
                                }
                                if (!embedding.isEmpty()) {
                                    return embedding;
                                }
                            }
                        }
                    }
                }
            }
            logger.error("OpenAI embedding response missing or empty embedding field");
            throw new IllegalStateException("OpenAI 嵌入响应无效：缺少 data[0].embedding 或为空");
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error calling OpenAI embedding API: {}", e.getMessage());
            throw new IllegalStateException("OpenAI 向量化失败，请检查 api-key、base-url、网络与模型名", e);
        }
    }
}
