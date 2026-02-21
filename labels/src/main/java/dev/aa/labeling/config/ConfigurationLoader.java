package dev.aa.labeling.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigurationLoader {
    
    private static final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    
    public static Configuration load(Path configPath) throws IOException {
        String jsonContent = Files.readString(configPath);
        return loadFromJson(jsonContent);
    }
    
    public static Configuration load(String configPath) throws IOException {
        return load(Path.of(configPath));
    }
    
    public static Configuration loadFromResource(String resourceName) throws IOException {
        var inputStream = ConfigurationLoader.class.getClassLoader().getResourceAsStream(resourceName);
        if (inputStream == null) {
            throw new IOException("Resource not found: " + resourceName);
        }
        String jsonContent = new String(inputStream.readAllBytes());
        return loadFromJson(jsonContent);
    }

    public static Configuration loadFromJson(String jsonContent) throws IOException {
        ConfigurationValidator validator = new ConfigurationValidator();
        ConfigurationValidator.ValidationResult result = validator.validate(jsonContent);

        if (!result.isValid()) {
            throw new IOException("Configuration validation failed:\n" + String.join("\n", result.errors()));
        }

        return mapper.readValue(jsonContent, Configuration.class);
    }
}
