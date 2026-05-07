package org.example.qasystem.controller;

import org.example.qasystem.model.ApiResponse;
import org.example.qasystem.model.ChatRequest;
import org.example.qasystem.exception.UpstreamServiceException;
import org.example.qasystem.service.ChatService;
import org.example.qasystem.service.impl.LLMServiceFactory;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
@Slf4j

public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/{modelName}")
    public ApiResponse<String> chat(@PathVariable String modelName,
                                    @RequestBody ChatRequest request) {
        try {
            String reply = chatService.chat(request.getMessage(), modelName);
            return ApiResponse.success(reply);
        } catch (IllegalArgumentException ex) {
            log.warn("chat bad request model={} msg={}", modelName, ex.getMessage());
            return ApiResponse.errro(400, ex.getMessage());
        } catch (IllegalStateException ex) {
            log.warn("chat embedding or state failed model={} msg={}", modelName, ex.getMessage());
            return ApiResponse.errro(503, ex.getMessage());
        } catch (UpstreamServiceException ex) {
            log.warn("chat upstream failed model={} status={} msg={}", modelName, ex.getStatusCode(), ex.getSafeMessage());
            return ApiResponse.errro(ex.getStatusCode(), ex.getSafeMessage());
        } catch (Exception ex) {
            log.error("chat failed model={}", modelName, ex);
            return ApiResponse.errro(500, "聊天服务调用失败");
        }
    }

    @GetMapping("/models")
    public List<String> listSupportedModels() {
        return LLMServiceFactory.getSupportedModels();
    }
}