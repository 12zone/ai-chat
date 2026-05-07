package org.example.qasystem.service.impl;


import org.example.qasystem.client.ModelChatClient;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LLMServiceFactory implements ApplicationContextAware {
    private static HashMap<String, ModelChatClient> chatClientHashMap;

    public static ModelChatClient getLLMService(String modelName){
        if (modelName == null || chatClientHashMap == null) {
            return null;
        }
        ModelChatClient direct = chatClientHashMap.get(modelName);
        if (direct != null) {
            return direct;
        }
        return chatClientHashMap.get(modelName.toUpperCase());
    }

    public static List<String> getSupportedModels() {
        if (chatClientHashMap == null) {
            return List.of();
        }
        return chatClientHashMap.keySet()
                .stream()
                .map(String::toLowerCase)
                .sorted()
                .collect(Collectors.toList());
    }
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException{
        Map<String, ModelChatClient> map = applicationContext.getBeansOfType(ModelChatClient.class);
        chatClientHashMap = new HashMap<>(10);
        for(ModelChatClient item : map.values()){
            chatClientHashMap.put(item.getModelName().name(),item);
        }
    }
}
