package dev.aa.labeling.labeler;

import dev.aa.labeling.Constants;
import dev.aa.labeling.llm.LLMProviderManager;
import dev.aa.labeling.llm.LLMResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class LLMAdapterImpl implements LLMAdapter {
    private static final Logger logger = LoggerFactory.getLogger(LLMAdapterImpl.class);
    
    private final LLMProviderManager manager;
    private final String systemPrompt;
    
    public LLMAdapterImpl(Path llmConfigDir) throws Exception {
        this.manager = new LLMProviderManager(llmConfigDir);
        this.systemPrompt = Constants.LLM_PROMPT;
    }
    
    @Override
    public boolean isFormOf(String key, String candidate, String language) {
        if (!manager.hasProviders()) {
            logger.warn("No LLM providers available");
            return false;
        }
        
        try {
            String userPrompt = String.format("Base: %s\nCandidate: %s", key, candidate);
            LLMResponse response = manager.chat(systemPrompt, userPrompt);
            
            String content = response.getContent().trim().toUpperCase();
            boolean result = "TRUE".equals(content) || content.startsWith("TRUE");
            
            logger.debug("LLM: key='{}', candidate='{}' -> {}", key, candidate, result);
            return result;
            
        } catch (Exception e) {
            logger.error("LLM call failed for key='{}', candidate='{}': {}", 
                key, candidate, e.getMessage());
            return false;
        }
    }
    
    public void close() {
        manager.close();
    }
}
