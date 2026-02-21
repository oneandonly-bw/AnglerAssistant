package dev.aa.labeling.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LLMProviderConfig {
    private String name;
    private String displayName;
    private String description;
    private String apiUrl;
    private String model;
    private String apiKey;
    @JsonProperty("apiKeyEnvVar")
    private String apiKeyEnvVar;
    private boolean enabled;
    private int priority;
    private int maxTokens;
    private double temperature;
    @JsonProperty("timeoutMs")
    private int timeoutMs;
    private RateLimitConfig rateLimit;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RateLimitConfig {
        @JsonProperty("requestsPerMinute")
        private int requestsPerMinute;
        @JsonProperty("requestsPerDay")
        private int requestsPerDay;
        @JsonProperty("tokensPerDay")
        private int tokensPerDay;

        public int getRequestsPerMinute() { return requestsPerMinute; }
        public int getRequestsPerDay() { return requestsPerDay; }
        public int getTokensPerDay() { return tokensPerDay; }
    }

    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public String getApiUrl() { return apiUrl; }
    public String getModel() { return model; }
    public String getApiKey() { return apiKey; }
    public String getApiKeyEnvVar() { return apiKeyEnvVar; }
    public boolean isEnabled() { return enabled; }
    public int getPriority() { return priority; }
    public int getMaxTokens() { return maxTokens; }
    public double getTemperature() { return temperature; }
    public int getTimeoutMs() { return timeoutMs; }
    public RateLimitConfig getRateLimit() { return rateLimit; }
}
