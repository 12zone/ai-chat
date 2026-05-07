package org.example.qasystem.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spring.ai.deepseek")
public class DeepseekProperties {
    private String apiKey;
    private String baseUrl = "https://api.deepseek.com";

    public String getBaseUrl(){ return baseUrl; }
    public String getApiKey(){ return apiKey; }

    public void setApiKey(String apiKey){ this.apiKey = apiKey; }
    public void setBaseUrl(String baseUrl){ this.baseUrl = baseUrl; }

}
