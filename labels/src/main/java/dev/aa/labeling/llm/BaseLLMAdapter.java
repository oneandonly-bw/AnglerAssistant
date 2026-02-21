package dev.aa.labeling.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public abstract class BaseLLMAdapter implements LLMProvider {
    protected final LLMProviderConfig config;
    protected final HttpClient httpClient;
    protected final ObjectMapper objectMapper;
    protected String apiKey;

    public BaseLLMAdapter(LLMProviderConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();
        this.objectMapper = new ObjectMapper();
        
        this.apiKey = config.getApiKey();
        if ((apiKey == null || apiKey.isEmpty()) && config.getApiKeyEnvVar() != null) {
            this.apiKey = System.getenv(config.getApiKeyEnvVar());
        }
        
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("API key not found for " + config.getName());
        }
    }

    @Override
    public String getName() { return config.getName(); }
    
    @Override
    public String getModel() { return config.getModel(); }
    
    @Override
    public boolean isEnabled() { return config.isEnabled(); }
    
    @Override
    public int getPriority() { return config.getPriority(); }
    
    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }

    protected abstract Map<String, Object> buildRequestBody(String systemPrompt, String userMessage);
    
    protected abstract String extractContentFromResponse(JsonNode responseJson);

    @Override
    public LLMResponse chat(String systemPrompt, String userMessage) throws LLMException {
        try {
            long startTime = System.currentTimeMillis();
            
            Map<String, Object> requestBody = buildRequestBody(systemPrompt, userMessage);
            String requestJson = objectMapper.writeValueAsString(requestBody);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getApiUrl()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofMillis(config.getTimeoutMs()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long responseTime = System.currentTimeMillis() - startTime;

            return parseResponse(response, responseTime);

        } catch (java.net.http.HttpTimeoutException e) {
            throw new LLMException("Request timeout", config.getName(), LLMException.ErrorType.NETWORK_ERROR, e);
        } catch (IOException e) {
            throw new LLMException("Network error: " + e.getMessage(), config.getName(), LLMException.ErrorType.NETWORK_ERROR, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LLMException("Request interrupted", config.getName(), LLMException.ErrorType.UNKNOWN, e);
        }
    }

    protected LLMResponse parseResponse(HttpResponse<String> response, long responseTime) throws LLMException {
        int statusCode = response.statusCode();
        
        if (statusCode == 429) {
            String retryAfter = response.headers().firstValue("Retry-After").orElse(null);
            String rateLimitInfo = response.headers().firstValue("X-RateLimit-Remaining").orElse("unknown");
            
            String message = "Rate limit exceeded";
            if (retryAfter != null) {
                message += ", retry after " + retryAfter + "s";
            }
            message += " (remaining: " + rateLimitInfo + ")";
            
            LLMException ex = new LLMException(message, config.getName(), LLMException.ErrorType.RATE_LIMIT);
            if (retryAfter != null) {
                try {
                    ex.setRetryAfterSeconds(Integer.parseInt(retryAfter));
                } catch (NumberFormatException ignored) {}
            }
            throw ex;
        }
        if (statusCode == 401) {
            throw new LLMException("Invalid API key", config.getName(), LLMException.ErrorType.AUTH_ERROR);
        }
        if (statusCode == 400) {
            String msg = extractErrorMessage(response.body());
            throw new LLMException("Invalid request: " + msg, config.getName(), LLMException.ErrorType.INVALID_REQUEST);
        }
        if (statusCode >= 500) {
            throw new LLMException("Server error: " + statusCode, config.getName(), LLMException.ErrorType.SERVER_ERROR);
        }
        if (statusCode != 200) {
            throw new LLMException("Unexpected response: " + statusCode, config.getName(), LLMException.ErrorType.UNKNOWN);
        }

        try {
            JsonNode json = objectMapper.readTree(response.body());
            
            if (json.has("error")) {
                String errorMsg = json.get("error").asText();
                if (errorMsg.contains("rate_limit") || errorMsg.contains("Rate limit")) {
                    throw new LLMException("Rate limit: " + errorMsg, config.getName(), LLMException.ErrorType.RATE_LIMIT);
                }
                if (errorMsg.contains("token") || errorMsg.contains("context")) {
                    throw new LLMException("Token limit: " + errorMsg, config.getName(), LLMException.ErrorType.TOKEN_LIMIT);
                }
                throw new LLMException("API error: " + errorMsg, config.getName(), LLMException.ErrorType.UNKNOWN);
            }

            String content = extractContentFromResponse(json);
            JsonNode usage = json.has("usage") ? json.get("usage") : null;
            int inputTokens = usage != null && usage.has("prompt_tokens") ? usage.get("prompt_tokens").asInt() : 0;
            int outputTokens = usage != null && usage.has("completion_tokens") ? usage.get("completion_tokens").asInt() : 0;

            return new LLMResponse(content, config.getModel(), inputTokens, outputTokens, responseTime);

        } catch (LLMException e) {
            throw e;
        } catch (Exception e) {
            throw new LLMException("Failed to parse response: " + e.getMessage(), config.getName(), LLMException.ErrorType.UNKNOWN);
        }
    }

    protected String extractErrorMessage(String body) {
        try {
            JsonNode json = objectMapper.readTree(body);
            if (json.has("error")) {
                JsonNode error = json.get("error");
                if (error.has("message")) {
                    return error.get("message").asText();
                }
                return error.asText();
            }
        } catch (Exception ignored) {}
        return body;
    }

    @Override
    public void close() {
    }
}
