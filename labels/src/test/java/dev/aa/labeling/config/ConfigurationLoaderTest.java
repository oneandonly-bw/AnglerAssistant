package dev.aa.labeling.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;

class ConfigurationLoaderTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    void testLoadFromResource() throws IOException {
        Configuration config = ConfigurationLoader.loadFromResource("config/israfish_config.json");
        
        assertNotNull(config);
        assertEquals("israfish", config.site().siteId());
        assertEquals("PHPBB", config.forums().get(0).forumType());
    }
    
    @Test
    void testLoadFromPath() throws IOException {
        String configContent = """
            {
              "general": {"loggingLevel": "DEBUG"},
              "site": {"siteId": "test", "name": "Test", "baseUrl": "https://test.com/", "httpTimeout": 30000, "httpUserAgent": "TestBot"},
              "runtime": {"memoryThreshold": 0.5, "maxRetries": 1},
              "labeler": {"enabled": false},
              "forums": []
            }
            """;
        
        Path configFile = tempDir.resolve("test.json");
        java.nio.file.Files.writeString(configFile, configContent);
        
        Configuration config = ConfigurationLoader.load(configFile);
        
        assertNotNull(config);
        assertEquals("test", config.site().siteId());
        assertEquals("DEBUG", config.general().loggingLevel());
        assertEquals(0.5, config.runtime().memoryThreshold());
    }
    
    @Test
    void testLoadFromString() throws IOException {
        String configContent = """
            {
              "general": {"loggingLevel": "INFO"},
              "site": {"siteId": "strtest", "name": "StrTest", "baseUrl": "https://test.com/", "httpTimeout": 30000, "httpUserAgent": "Bot"},
              "runtime": {"memoryThreshold": 0.8, "maxRetries": 3},
              "labeler": {"enabled": true},
              "forums": []
            }
            """;
        
        Path configFile = tempDir.resolve("strtest.json");
        java.nio.file.Files.writeString(configFile, configContent);
        
        Configuration config = ConfigurationLoader.load(configFile.toString());
        
        assertNotNull(config);
        assertEquals("strtest", config.site().siteId());
    }
    
    @Test
    void testLoadInvalidResource() {
        assertThrows(IOException.class, () -> {
            ConfigurationLoader.loadFromResource("nonexistent.json");
        });
    }
    
    @Test
    void testValidationErrorBothIncludeAndExclude() throws IOException {
        String configContent = """
            {
              "general": {"loggingLevel": "INFO"},
              "site": {"siteId": "test", "name": "Test", "baseUrl": "https://test.com/"},
              "runtime": {"memoryThreshold": 0.8, "maxRetries": 3},
              "extractor": {"enabled": true},
              "forums": [
                {
                  "url": "https://test.com/f1",
                  "forumName": "Test Forum",
                  "path": "test",
                  "enabled": true,
                  "forumType": "PHPBB",
                  "include": [{"name": "Topics", "type": "SECTION"}],
                  "exclude": [{"name": "Announcements", "type": "SECTION"}]
                }
              ]
            }
            """;
        
        Path configFile = tempDir.resolve("invalid.json");
        java.nio.file.Files.writeString(configFile, configContent);
        
        assertThrows(IOException.class, () -> {
            ConfigurationLoader.load(configFile);
        });
    }
    
    @Test
    void testValidationErrorMissingRequiredField() throws IOException {
        String configContent = """
            {
              "general": {"loggingLevel": "INFO"},
              "site": {"siteId": "test", "name": "Test"},
              "runtime": {"memoryThreshold": 0.8, "maxRetries": 3},
              "extractor": {"enabled": true},
              "forums": []
            }
            """;
        
        Path configFile = tempDir.resolve("invalid.json");
        java.nio.file.Files.writeString(configFile, configContent);
        
        assertThrows(IOException.class, () -> {
            ConfigurationLoader.load(configFile);
        });
    }
}
