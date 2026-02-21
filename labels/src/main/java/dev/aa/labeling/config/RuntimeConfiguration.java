package dev.aa.labeling.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.aa.labeling.Constants;

public record RuntimeConfiguration(
    @JsonProperty(value = "memoryThreshold", defaultValue = "0.8") double memoryThreshold,
    @JsonProperty(value = "maxRetries", defaultValue = "3") int maxRetries
) {
    public static RuntimeConfiguration defaults() {
        return new RuntimeConfiguration(
            Constants.DEFAULT_MEMORY_THRESHOLD,
            Constants.DEFAULT_MAX_RETRIES
        );
    }
}
