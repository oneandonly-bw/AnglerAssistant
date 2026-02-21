# Multi-Provider LLM Adapter with Checkpoint Support

## Feature: Switchable LLM Providers

### Providers with Real Free Tier

| Priority | Provider | Free Tier | Env Var | Model |
|----------|----------|-----------|---------|-------|
| 1 | Groq | 500k tokens/day | `GROQ_API_KEY` | llama-3.1-70b-versatile |
| 2 | OpenRouter | 50 requests/day | `OPENROUTER_API_KEY` | google/gemma-3-27b-it:free |
| 3 | HuggingFace | free tier | `HF_API_KEY` | meta-llama/Llama-3.2-3B-Instruct |

### Configuration Files

Location: `src/main/resources/llm/`

- `groq.json` - Groq provider config
- `openrouter.json` - OpenRouter provider config  
- `huggingface.json` - HuggingFace provider config

### Switching Logic: Round-Robin with Rate Limiting

```java
public class LLMProviderManager {
    private List<ProviderState> providers;
    private int currentIndex;
    
    // Cycles through providers: 1 -> 2 -> 3 -> 1 -> 2 -> 3...
    // Calculates wait time based on requestsPerMinute config
    // On 429 rate limit: 60s cooldown
    // On other retryable error: 30s cooldown
    
    public LLMResponse chat(String systemPrompt, String userMessage) {
        // Round-robin through available providers
        // Auto-wait for rate limits
    }
}
```

### Usage

```java
Path llmConfigDir = Paths.get("src/main/resources/llm");
LLMProviderManager manager = new LLMProviderManager(llmConfigDir);

LLMResponse response = manager.chat(
    "You are a helpful assistant.", 
    "Is 'карп' in 'Я ловлю карпа'?"
);
```

## Feature: Checkpoint/Snapshot System

### Purpose
Save progress periodically so we can resume from where we stopped if:
- Process killed
- Out of tokens
- Network failure

### Checkpoint Data

```json
{
  "timestamp": "2026-02-17T10:30:00Z",
  "topicIndex": 42,
  "sentenceIndex": 156,
  "cacheState": {
    "positiveCache": "cache/lemmas_positive.json",
    "negativeCache": "cache/lemmas_negative.json"
  },
  "lastProcessedUrl": "https://forum.israfish.com/t=1234"
}
```

### Checkpoint Manager

```java
public class CheckpointManager {
    private Path checkpointPath;
    private int saveIntervalTopics = 10;
    
    public void saveCheckpoint(int topicIndex, int sentenceIndex, String lastUrl);
    public Checkpoint loadCheckpoint();
    public boolean hasCheckpoint();
    public void clearCheckpoint();
}
```

### Resume Flow

```
1. Start process
2. CheckpointManager.hasCheckpoint()?
   │
   ├─▶ YES ──▶ Load checkpoint
   │           Resume from topicIndex, sentenceIndex
   │
   └─▶ NO  ──▶ Start from beginning

3. Process topics

4. Every 10 topics:
   CheckpointManager.saveCheckpoint(...)

5. On interrupt (Ctrl+C):
   Save checkpoint immediately

6. If tokens exhausted:
   - Save checkpoint
   - Log: "Switching provider or stopping. Resume with --resume"
```

## CLI Options

```bash
# Normal run
java -jar labeling-system.jar --config config.json

# Resume from checkpoint
java -jar labeling-system.jar --config config.json --resume

# Force start fresh (ignores checkpoint)
java -jar labeling-system.jar --config config.json --fresh

# Use specific provider
java -jar labeling-system.jar --config config.json --provider groq

# Show checkpoint status
java -jar labeling-system.jar --config config.json --status
```

## Implementation Checklist

- [x] LLMProvider interface
- [x] GroqAdapter implementation (updated model to llama-3.3-70b-versatile)
- [x] OpenRouterAdapter implementation
- [x] HuggingFaceAdapter implementation
- [x] LLMProviderManager with round-robin logic
- [x] CheckpointManager
- [x] Signal handler for graceful shutdown (Main.java)
- [x] Unit tests for CacheManager
- [x] LLMAdapter for labeler (LLMAdapterImpl)
- [ ] Console menu (in-app, low priority)

## Source Files

| File | Purpose |
|------|---------|
| `llm/LLMProvider.java` | Provider interface |
| `llm/LLMResponse.java` | Response model |
| `llm/LLMException.java` | Exception with retry logic |
| `llm/LLMProviderConfig.java` | Config loader |
| `llm/BaseLLMAdapter.java` | HTTP client base class |
| `llm/GroqAdapter.java` | Groq implementation |
| `llm/OpenRouterAdapter.java` | OpenRouter implementation |
| `llm/HuggingFaceAdapter.java` | HuggingFace implementation |
| `llm/LLMProviderManager.java` | Round-robin switcher |
| `llm/CheckpointManager.java` | Save/resume progress |
