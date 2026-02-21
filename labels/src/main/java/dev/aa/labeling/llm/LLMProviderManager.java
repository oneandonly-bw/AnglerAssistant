package dev.aa.labeling.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class LLMProviderManager {
    private static final Logger logger = LoggerFactory.getLogger(LLMProviderManager.class);
    private static final long MINUTE_MS = 60_000L;
    private static final long DAY_MS = 86_400_000L;
    
    private final List<ProviderState> providers;
    private final Map<String, ProviderState> providerMap;
    private int roundRobinIndex;
    private final Object lock = new Object();

    public LLMProviderManager(Path configDir) throws IOException {
        this.providers = new ArrayList<>();
        this.providerMap = new HashMap<>();
        loadProviders(configDir);
        sortByPriority();
        
        logger.info("Loaded {} LLM providers", providers.size());
        for (var p : providers) {
            logger.info("  - {} ({}): priority={}, RPM={}, RPD={}", 
                p.config.getName(), p.config.getModel(), p.config.getPriority(), 
                getRpm(p), getRpd(p));
        }
    }

    private int getRpm(ProviderState p) {
        var rl = p.config.getRateLimit();
        return rl != null ? rl.getRequestsPerMinute() : 0;
    }
    
    private int getRpd(ProviderState p) {
        var rl = p.config.getRateLimit();
        return rl != null ? rl.getRequestsPerDay() : 0;
    }

    private void loadProviders(Path configDir) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        
        if (!Files.exists(configDir)) {
            logger.warn("LLM config directory not found: {}", configDir);
            return;
        }
        
        logger.info("Loading LLM providers from: {}", configDir.toAbsolutePath());
        
        try (var files = Files.list(configDir)) {
            for (var file : files.collect(Collectors.toList())) {
                if (file.toString().endsWith(".json") && !file.toString().endsWith("_key.json")) {
                    try {
                        LLMProviderConfig config = mapper.readValue(file.toFile(), LLMProviderConfig.class);
                        injectKey(config, configDir);
                        LLMProvider adapter = createAdapter(config);
                        if (adapter != null) {
                            providers.add(new ProviderState(adapter, config));
                            providerMap.put(config.getName(), providers.get(providers.size() - 1));
                        }
                    } catch (Exception e) {
                        logger.error("Failed to load provider config from {}: {}", file, e.getMessage());
                    }
                }
            }
        }
        
        logger.info("Total providers loaded: {}", providers.size());
    }
    
    private void injectKey(LLMProviderConfig config, Path configDir) {
        String providerName = config.getName();
        Path keyFile = configDir.resolve(providerName + "_key.json");
        
        if (Files.exists(keyFile)) {
            try {
                ObjectMapper keyMapper = new ObjectMapper();
                Map<String, String> keyData = keyMapper.readValue(keyFile.toFile(), 
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
                
                String key = keyData.get("key");
                if (key != null && !key.isEmpty()) {
                    config.setApiKey(key);
                    logger.info("Injected API key for provider: {}", providerName);
                }
            } catch (Exception e) {
                logger.warn("Failed to load key file for {}: {}", providerName, e.getMessage());
            }
        }
    }

    private LLMProvider createAdapter(LLMProviderConfig config) {
        if (!config.isEnabled()) {
            logger.debug("Provider {} is disabled, skipping", config.getName());
            return null;
        }
        
        String apiKey = config.getApiKey();
        if ((apiKey == null || apiKey.isEmpty()) && config.getApiKeyEnvVar() != null) {
            apiKey = System.getenv(config.getApiKeyEnvVar());
        }
        
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("API key not found for {} (config or env: {}), skipping", 
                config.getName(), config.getApiKeyEnvVar());
            return null;
        }
        
        return switch (config.getName()) {
            case "groq" -> new GroqAdapter(config);
            case "openrouter" -> new OpenRouterAdapter(config);
            case "huggingface" -> new HuggingFaceAdapter(config);
            default -> {
                logger.warn("Unknown provider type: {}", config.getName());
                yield null;
            }
        };
    }

    private void sortByPriority() {
        providers.sort(Comparator.comparingInt(p -> p.config.getPriority()));
        roundRobinIndex = 0;
    }

    public LLMResponse chat(String systemPrompt, String userMessage) throws LLMException {
        if (providers.isEmpty()) {
            throw new LLMException("No LLM providers available", "none", LLMException.ErrorType.UNKNOWN);
        }

        int maxAttempts = providers.size() * 3;
        int attempts = 0;
        LLMException lastException = null;
        
        while (attempts < maxAttempts) {
            attempts++;
            
            ProviderState state = getNextProviderRoundRobin();
            if (state == null) {
                throw new LLMException("No providers available (all rate limited or unavailable)", 
                    "all", LLMException.ErrorType.RATE_LIMIT);
            }

            try {
                long waitTime = calculateWaitTime(state);
                if (waitTime > 0) {
                    logger.debug("Provider {} rate limited, waiting {}ms", state.config.getName(), waitTime);
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new LLMException("Interrupted while waiting for rate limit", 
                            state.config.getName(), LLMException.ErrorType.UNKNOWN, e);
                    }
                }
                
                logger.debug("Calling provider: {} ({})", state.provider.getName(), state.provider.getModel());
                LLMResponse response = state.provider.chat(systemPrompt, userMessage);
                
                recordSuccess(state);
                
                return response;
                
            } catch (LLMException e) {
                lastException = e;
                logger.warn("Provider {} failed: {} (type: {})", 
                    state.config.getName(), e.getMessage(), e.getErrorType());
                
                if (e.getErrorType() == LLMException.ErrorType.RATE_LIMIT) {
                    handleRateLimit(state, e);
                } else if (!e.isRetryable()) {
                    markProviderUnavailable(state, 30000);
                }
            }
        }
        
        throw lastException != null ? lastException : 
            new LLMException("All providers exhausted after " + maxAttempts + " attempts", "all", LLMException.ErrorType.UNKNOWN);
    }

    private void handleRateLimit(ProviderState state, LLMException e) {
        Integer retryAfter = e.getRetryAfterSeconds();
        
        if (retryAfter != null && retryAfter > 0) {
            long cooldownMs = retryAfter * 1000L;
            state.coolDownUntil = System.currentTimeMillis() + cooldownMs;
            logger.info("Provider {} rate limited, cooling down for {}s (from Retry-After header)", 
                state.config.getName(), retryAfter);
        } else {
            state.coolDownUntil = System.currentTimeMillis() + MINUTE_MS;
            logger.info("Provider {} rate limited, cooling down for 60s", state.config.getName());
        }
        
        state.minuteRequestCount = 0;
        state.dayRequestCount = 0;
    }

    private void markProviderUnavailable(ProviderState state, long cooldownMs) {
        state.coolDownUntil = System.currentTimeMillis() + cooldownMs;
    }

    private void recordSuccess(ProviderState state) {
        long now = System.currentTimeMillis();
        
        state.lastRequestTime = now;
        state.minuteRequestCount++;
        state.dayRequestCount++;
        
        if (state.minuteWindowStart + MINUTE_MS < now) {
            state.minuteWindowStart = now;
            state.minuteRequestCount = 1;
        }
        
        if (state.dayWindowStart + DAY_MS < now) {
            state.dayWindowStart = now;
            state.dayRequestCount = 1;
        }
    }

    private ProviderState getNextProviderRoundRobin() {
        synchronized (lock) {
            long now = System.currentTimeMillis();
            int checked = 0;
            
            while (checked < providers.size()) {
                int idx = roundRobinIndex % providers.size();
                roundRobinIndex++;
                
                ProviderState state = providers.get(idx);
                
                if (state.coolDownUntil > now) {
                    checked++;
                    continue;
                }
                
                if (!state.provider.isAvailable()) {
                    checked++;
                    continue;
                }
                
                if (isRateLimitExceeded(state)) {
                    long waitTime = calculateWaitTime(state);
                    if (waitTime > 0 && waitTime < 1000) {
                        checked++;
                        continue;
                    }
                    if (waitTime >= 1000) {
                        state.coolDownUntil = now + waitTime;
                        checked++;
                        continue;
                    }
                }
                
                return state;
            }
            
            return null;
        }
    }

    private boolean isRateLimitExceeded(ProviderState state) {
        var rateLimit = state.config.getRateLimit();
        if (rateLimit == null) {
            return false;
        }
        
        if (rateLimit.getRequestsPerMinute() > 0 && 
            state.minuteRequestCount >= rateLimit.getRequestsPerMinute()) {
            return true;
        }
        
        if (rateLimit.getRequestsPerDay() > 0 && 
            state.dayRequestCount >= rateLimit.getRequestsPerDay()) {
            return true;
        }
        
        return false;
    }

    private long calculateWaitTime(ProviderState state) {
        var rateLimit = state.config.getRateLimit();
        if (rateLimit == null) {
            return 0;
        }
        
        long now = System.currentTimeMillis();
        
        if (rateLimit.getRequestsPerMinute() > 0 && 
            state.minuteRequestCount >= rateLimit.getRequestsPerMinute()) {
            long timeInWindow = now - state.minuteWindowStart;
            if (timeInWindow < MINUTE_MS) {
                return MINUTE_MS - timeInWindow;
            }
        }
        
        if (rateLimit.getRequestsPerDay() > 0 && 
            state.dayRequestCount >= rateLimit.getRequestsPerDay()) {
            long timeInWindow = now - state.dayWindowStart;
            if (timeInWindow < DAY_MS) {
                return DAY_MS - timeInWindow;
            }
        }
        
        return 0;
    }

    public boolean hasProviders() {
        return !providers.isEmpty();
    }

    public List<LLMProvider> getProviders() {
        return providers.stream().map(p -> p.provider).collect(Collectors.toList());
    }
    
    public List<LLMProviderConfig> getProviderConfigs() {
        return providers.stream().map(p -> p.config).collect(Collectors.toList());
    }

    public void close() {
        for (var state : providers) {
            try {
                state.provider.close();
            } catch (Exception e) {
                logger.error("Error closing provider {}: {}", state.provider.getName(), e.getMessage());
            }
        }
    }

    private static class ProviderState {
        final LLMProvider provider;
        final LLMProviderConfig config;
        long lastRequestTime = 0;
        long minuteWindowStart = 0;
        long dayWindowStart = 0;
        int minuteRequestCount = 0;
        int dayRequestCount = 0;
        long coolDownUntil = 0;

        ProviderState(LLMProvider provider, LLMProviderConfig config) {
            this.provider = provider;
            this.config = config;
            this.minuteWindowStart = System.currentTimeMillis();
            this.dayWindowStart = System.currentTimeMillis();
        }
    }
}
