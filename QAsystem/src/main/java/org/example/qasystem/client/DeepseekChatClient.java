package org.example.qasystem.client;

import org.example.qasystem.config.DeepseekProperties;
import org.example.qasystem.exception.UpstreamServiceException;
import org.example.qasystem.model.ModelType;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DeepseekChatClient implements ModelChatClient {

    private final DeepseekProperties props;

    public DeepseekChatClient(DeepseekProperties properties) {
        this.props = properties;
    }


    @Override
    public ModelType getModelName() {
        return ModelType.deepseek;
    }


    @Override
    public String chatWithSystem(String systemPrompt, String userMessage) {

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(normalizeBaseUrl(props.getBaseUrl()))
                .apiKey(props.getApiKey())
                .build();

        ChatModel chatModel = org.springframework.ai.openai.OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("deepseek-chat")
                        .build())
                .build();

        ChatClient chatClient = ChatClient.builder(chatModel).build();

        try {
            // 发送消息并获取响应
            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .content();

            return response;
        } catch (Exception e) {
            log.error("Error calling Deepseek API: {}", e.getMessage());
            throw mapToUpstreamException(e);
        }
    }

    private UpstreamServiceException mapToUpstreamException(Exception e) {
        int statusCode = extractStatusCode(e);
        String safeMessage = switch (statusCode) {
            case 401, 403 -> "模型服务鉴权失败，请检查 API Key 配置";
            case 402 -> "模型服务账户余额不足，请充值后重试";
            case 408 -> "模型服务响应超时，请稍后重试";
            case 413 -> "请求内容过长，请缩短提问后重试";
            case 429 -> "模型服务限流，请稍后重试";
            case 500, 502, 503, 504 -> "模型服务暂时不可用，请稍后重试";
            default -> "模型调用失败，请稍后重试";
        };
        int safeStatusCode = statusCode > 0 ? statusCode : 500;
        return new UpstreamServiceException(safeStatusCode, safeMessage, e);
    }

    private int extractStatusCode(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof WebClientResponseException webClientEx) {
                return webClientEx.getStatusCode().value();
            }
            if (current instanceof RestClientResponseException restClientEx) {
                return restClientEx.getStatusCode().value();
            }
            current = current.getCause();
        }
        return 0;
    }

    private String normalizeBaseUrl(String rawBaseUrl) {
        if (rawBaseUrl == null || rawBaseUrl.isBlank()) {
            return "https://api.deepseek.com";
        }
        String normalized = rawBaseUrl.trim();
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("/v1")) {
            normalized = normalized.substring(0, normalized.length() - 3);
        }
        return normalized;
    }
}

