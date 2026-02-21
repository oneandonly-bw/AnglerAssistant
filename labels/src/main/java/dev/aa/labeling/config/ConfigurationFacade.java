package dev.aa.labeling.config;

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigurationFacade {
    
    public static Configuration getConfiguration(String configPath) throws ConfigurationException {
        if (configPath == null || configPath.isBlank()) {
            throw new ConfigurationException("Configuration path cannot be null or empty");
        }
        
        Path path = Paths.get(configPath);
        
        try {
            if (path.isAbsolute()) {
                return ConfigurationLoader.load(path);
            } else {
                return ConfigurationLoader.loadFromResource(configPath);
            }
        } catch (IOException e) {
            throw new ConfigurationException("Failed to load configuration: " + e.getMessage(), e);
        }
    }
    
    public static class ConfigurationException extends Exception {
        @Serial
        private static final long serialVersionUID = 1L;
        
        public ConfigurationException(String message) {
            super(message);
        }
        
        public ConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
