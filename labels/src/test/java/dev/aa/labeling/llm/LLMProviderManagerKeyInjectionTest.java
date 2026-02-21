package dev.aa.labeling.llm;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LLMProviderManagerKeyInjectionTest {

    @Test
    void testInjectKey() throws Exception {
        Path configDir = Files.createTempDirectory("llm-test");
        
        String configJson = """
            {
              "name": "groq",
              "displayName": "Groq",
              "apiUrl": "https://api.groq.com",
              "model": "test-model",
              "apiKey": "PLACEHOLDER_KEY",
              "enabled": true,
              "priority": 1,
              "timeoutMs": 30000
            }
            """;
        
        String keyJson = """
            {
              "key": "injected_key_123"
            }
            """;
        
        Path configFile = configDir.resolve("groq.json");
        Path keyFile = configDir.resolve("groq_key.json");
        
        Files.writeString(configFile, configJson);
        Files.writeString(keyFile, keyJson);
        
        System.out.println("Config dir: " + configDir);
        System.out.println("Config file exists: " + Files.exists(configFile));
        System.out.println("Key file exists: " + Files.exists(keyFile));
        
        LLMProviderManager manager = new LLMProviderManager(configDir);
        
        System.out.println("Providers loaded: " + manager.getProviderConfigs().size());
        
        assertEquals(1, manager.getProviderConfigs().size());
        assertEquals("injected_key_123", manager.getProviderConfigs().get(0).getApiKey());
        
        Files.walk(configDir).sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
            try { Files.delete(p); } catch (IOException ignored) {}
        });
    }
}
