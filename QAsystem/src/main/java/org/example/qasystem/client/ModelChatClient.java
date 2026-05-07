package org.example.qasystem.client;

import org.example.qasystem.model.ModelType;

public interface ModelChatClient {
    ModelType getModelName();
    String chatWithSystem(String systemPrompt, String userMessage);
}
