package dev.aa.labeling.labeler;

import dev.aa.labeling.Constants;
import dev.aa.labeling.llm.LLMProviderManager;
import dev.aa.labeling.llm.LLMResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LLMAdapterImpl implements LLMAdapter {
    private static final Logger logger = LoggerFactory.getLogger(LLMAdapterImpl.class);
    private static final Path LOG_FILE = Paths.get("output", "LLM_log.txt");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final LLMProviderManager manager;
    
    public LLMAdapterImpl(Path llmConfigDir) throws Exception {
        this.manager = new LLMProviderManager(llmConfigDir);
    }
    
    @Override
    public boolean isFormOf(String key, String candidate, String language, String entryType) {
        if (!manager.hasProviders()) {
            logger.warn("No LLM providers available");
            return false;
        }
        
        String promptTemplate;
        if ("ru".equals(language)) {
            promptTemplate = Constants.RU_IS_FORM_OF_PROMPT;
        } else {
            throw new IllegalArgumentException("Language not supported: " + language + ". Only Russian (ru) is currently supported.");
        }
        
        try {
            String prompt = String.format(promptTemplate, entryType, key, candidate);
            LLMResponse response = manager.chat(null, prompt);
            
            String content = response.getContent().trim().toUpperCase();
            boolean result = "TRUE".equals(content) || content.startsWith("TRUE");
            
            logToFile("isFormOf", key, candidate, null, prompt, result ? "TRUE" : "FALSE");
            
            if (result) {
                logger.info("LLM accept: candidate '{}' is {}", candidate, key);
            } else {
                logger.info("LLM reject: candidate '{}' is not {}", candidate, key);
            }
            return result;
            
        } catch (Exception e) {
            logger.error("LLM call failed for key='{}', candidate='{}': {}", 
                key, candidate, e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean isRelevantType(String term, String sentence, String entryType, int start, int end) {
        if (!manager.hasProviders()) {
            logger.warn("No LLM providers available");
            return false;
        }
        
        try {
            String prompt = Constants.buildDualityCheckPrompt(entryType, sentence, term);
            
            LLMResponse response = manager.chat(null, prompt);
            
            String content = response.getContent().trim().toUpperCase();
            boolean result = "TRUE".equals(content) || content.startsWith("TRUE");
            
            logToFile("isRelevantType", term, entryType, sentence, prompt, result ? "TRUE" : "FALSE");
            
            if (result) {
                logger.info("LLM accept: candidate '{}' is {}", term, entryType);
            } else {
                logger.info("LLM reject: candidate '{}' is not {}", term, entryType);
            }
            return result;
            
        } catch (Exception e) {
            logger.error("LLM duality check failed for term='{}': {}", term, e.getMessage());
            return false;
        }
    }
    
    private void logToFile(String method, String candidate, String key, String context, String prompt, String result) {
        try {
            Files.createDirectories(LOG_FILE.getParent());
            StringBuilder sb = new StringBuilder();
            sb.append("=== ").append(LocalDateTime.now().format(FORMATTER)).append(" ===\n");
            sb.append("Method: ").append(method).append("\n");
            sb.append("Prompt:\n").append(prompt).append("\n");
            sb.append("Result: ").append(result).append("\n\n");
            Files.writeString(LOG_FILE, sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            logger.error("Failed to write to log file: {}", e.getMessage());
        }
    }
    
    public void close() {
        manager.close();
    }
}
