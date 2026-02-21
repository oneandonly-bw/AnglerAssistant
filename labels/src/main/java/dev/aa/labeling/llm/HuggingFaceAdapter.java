package dev.aa.labeling.llm;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HuggingFaceAdapter extends BaseLLMAdapter {
    
    public HuggingFaceAdapter(LLMProviderConfig config) {
        super(config);
    }

    @Override
    protected Map<String, Object> buildRequestBody(String systemPrompt, String userMessage) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", config.getModel());
        body.put("temperature", config.getTemperature());
        body.put("max_tokens", config.getMaxTokens());
        
        List<Map<String, String>> messages = new ArrayList<>();
        
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);
        }
        
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);
        
        body.put("messages", messages);
        
        return body;
    }

    @Override
    protected String extractContentFromResponse(JsonNode responseJson) {
        JsonNode choices = responseJson.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            throw new IllegalStateException("No choices in response");
        }
        
        JsonNode message = choices.get(0).get("message");
        if (message == null) {
            throw new IllegalStateException("No message in first choice");
        }
        
        return message.get("content").asText();
    }

    @Override
    protected String extractErrorMessage(String body) {
        try {
            JsonNode json = objectMapper.readTree(body);
            if (json.has("error")) {
                return json.get("error").asText();
            }
        } catch (Exception ignored) {}
        return body;
    }
}
