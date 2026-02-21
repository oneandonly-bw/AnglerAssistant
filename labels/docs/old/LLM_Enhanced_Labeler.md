# LLM-Enhanced Labeling System - Design Document

## Overview

A hybrid labeling approach that combines:
1. Exact dictionary matching
2. Lemmatization-based matching  
3. LLM fallback for uncertain cases
4. Two rejectors: blockedTerms (persistent) + rejectedTerms (runtime LRU)

## Architecture

### Components

```
┌─────────────────────────────────────────────────────────────────────┐
│                   EnhancedSentencesLabeler                          │
├─────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │  Dictionary  │  │ CacheManager │  │  LLMAdapter  │           │
│  │   (JSON)     │  │ (HashSets)  │  │   (API)      │           │
│  └──────────────┘  └──────────────┘  └──────────────┘           │
│                                                                     │
│  ┌──────────────┐  ┌──────────────┐                              │
│  │ blockedTerms │  │rejectedTerms│                              │
│  │  (HashSet)   │  │ (LRU cache) │                              │
│  └──────────────┘  └──────────────┘                              │
└─────────────────────────────────────────────────────────────────────┘
```

### Two Rejectors

1. **blockedTerms** (persistent, HashSet):
   - Loaded from config file (`blockedTermsPath`)
   - Case-sensitive exact matches
   - For human names that conflict with species: "Карп", "Осётр"
   - Never evicted

2. **rejectedTerms** (runtime LRU, LinkedHashMap):
   - Added during processing when flow rejects a word
   - e.g., "карповик" != "карп" → added to rejectedTerms
   - LRU eviction when limit reached (`blockedTermsLimit`, default 1000)

### CacheManager

Manages two cache files (one entry per line, plain text):
- `output/cache/terms_seen.txt` - Confirmed term matches
- `output/cache/lemmas_seen.txt` - Confirmed lemma matches

**Tests verify:**
- Cache files are created in `output/cache/` directory
- Second run reuses cache (same terms count)
- Cache persists between sessions

## Data Structures

### Cache File Format

**lemmas_positive.json**:
```json
{
  "карп": {
    "exact": ["карпы", "карпа", "карпов"],
    "llm_verified": ["карпики", "карпиков", "карпище", "карпик"]
  },
  "карась": {
    "exact": ["караси", "карася"],
    "llm_verified": ["карасики"]
  }
}
```

**lemmas_negative.json**:
```json
{
  "карп": ["карпятник", "карповик", "карпова", "карповый"],
  "карась": ["карасев", "карасик"]
}
```

## Matching Algorithm

### Process Flow

```
For each sentence:
  │
  ▼
Extract candidate words (tokenize, keep positions)
  │
  ▼
For each candidate word:
  │
  ▼
Check: dictionary.contains(candidate)?
  │
  ├─▶ YES ──▶ EXACT MATCH ──▶ Add label, continue
  │
  ▼
NO
  │
  ▼
For each dictionary key:
  │
  ▼
1. EXACT CHECK: key.equals(candidate)?
   │
   ├─▶ YES ──▶ Add to positive cache (exact) ──▶ Add label ──▶ Continue
   │
   ▼
2. NEGATIVE CACHE CHECK: negativeCache.contains(key, candidate)?
   │
   ├─▶ YES ──▶ Skip ──▶ Continue
   │
   ▼
3. LEMMA CHECK: getLemma(candidate).equals(key)?
   │
   ├─▶ YES ──▶ Add to positive cache (exact) ──▶ Add label ──▶ Continue
   │
   ▼
4. POSITIVE CACHE CHECK: positiveCache.contains(key, candidate)?
   │
   ├─▶ YES ──▶ Add label ──▶ Continue
   │
   ▼
5. LLM FALLBACK ──▶ Send prompt
   │
   ├─▶ LLM returns TRUE:
   │   │
   │   ▼
   │   Add to positive cache (llm_verified)
   │   Add lemma to positive cache if new
   │   Add label
   │
   └─▶ LLM returns FALSE:
       │
       ▼
       Add to negative cache
       (don't label)
```

## Matching Algorithm

### Text Processing Pipeline

1. **Clean**: Remove special characters, keep original case
   - Input: `"Я поймал Карпа на червя! How are you?"`
   - Output: `"Я поймал Карпа на червя How are you"`

2. **Normalize**: Convert to lowercase (for matching)
   - Output: `"я поймал карпа на червя how are you"`

This separation is critical for **blocked terms** handling:
- Blocked terms file contains: `"Карп"` (capitalized, human name)
- Sentence contains: `"Карпа"` (capitalized, fish)
- We extract word from **cleaned** text (preserves case): `"Карпа"`
- Check against blockedTerms: `"Карпа"` NOT in blockedTerms → process
- If sentence had `"Карп"` (person name): `"Карп"` IN blockedTerms → skip

### Word Boundary Check

Before any matching, verify candidate is a standalone word:
- Use regex: `\b{candidate}\b` 
- Boundaries: space, punctuation (.,!?,:;), start/end of string
- This prevents "карп" from matching inside "карпятник"

## LLM Integration

### Prompt Template

```
System:
You are a Russian linguist.

TRUE if grammatical or size-modified form of the fish.
FALSE if derived word (equipment, profession, adjective).
Answer TRUE or FALSE only.

User:
Base: {key}
Candidate: {candidate}
```

### Response Parsing

- Accept: "TRUE", "TRUE.", "TRUE " (starts with TRUE)
- Default on parse error: FALSE

### LLM Configuration

```json
{
  "labeler": {
    "llm": {
      "enabled": true,
      "provider": "openai",  // or "anthropic", "local"
      "model": "gpt-4o-mini",
      "temperature": 0,
      "max_tokens": 10,
      "timeout_ms": 10000,
      "retry_attempts": 3,
      "retry_delay_ms": 1000
    }
  }
}
```

## Configuration

### JSON Configuration

```json
{
  "labeler": {
    "enabled": true,
    "minSentenceLength": 15,
    "minLanguageRatio": 0.3,
    "maxSpecialCharRatio": 0.2,
    "dictionaryPaths": [
      "resources/dictionaries/species_dict.json"
    ],
    "blockedTermsPath": "resources/dictionaries/blocked_terms.txt",
    "blockedTermsLimit": 100,
    "outputDirectory": "output/labels",
    "outputFileName": "labels.json",
    "cache": {
      "lemmasPositiveFile": "cache/lemmas_positive.json",
      "lemmasNegativeFile": "cache/lemmas_negative.json",
      "saveOnExit": true,
      "loadOnStartup": true
    },
    "llm": {
      "enabled": true,
      "provider": "openai",
      "model": "gpt-4o-mini",
      "temperature": 0,
      "apiKeyEnvVar": "OPENAI_API_KEY"
    }
  }
}
```

## Class Design

Based on `new_approach.txt`:

### CacheManager.java

Simple HashSets stored on disk (one entry per line):

```java
public class CacheManager {
    private final Path termsSeenPath;    // confirmed term matches
    private final Path lemmasSeenPath;   // confirmed lemma matches
    
    private Set<String> termsSeen;
    private Set<String> lemmasSeen;
    
    public CacheManager(Path termsSeenPath, Path lemmasSeenPath);
    public void load() throws IOException;
    public void save() throws IOException;
    
    public boolean containsTerm(String term);
    public boolean containsLemma(String lemma);
    public void addTerm(String term);
    public void addLemma(String lemma);
}
```

### LLMAdapter.java

```java
public interface LLMAdapter {
    boolean isFormOf(String key, String candidate, String language);
}
```

### LLMAdapterImpl.java

Uses existing LLMProviderManager (Groq, OpenRouter, HuggingFace):

```java
public class LLMAdapterImpl implements LLMAdapter {
    private final LLMProviderManager manager;
    
    public LLMAdapterImpl(Path llmConfigDir) throws Exception;
    
    @Override
    public boolean isFormOf(String key, String candidate, String language);
}
```

### EnhancedSentencesLabeler.java

Implements flow from `new_approach.txt`:

```java
public class EnhancedSentencesLabeler implements IfTopicLabeler, AutoCloseable {
    private List<String> dictionary;
    private HashSet<String> termsSeen;
    private HashSet<String> lemmasSeen;
    private LLMAdapter adapter;
    
    public EnhancedSentencesLabeler(LabelerConfiguration config, OutputWriter writer, Path llmConfigDir);
    
    @Override
    public void processTopic(Topic topic);
    
    private List<LabelPosition> findLabels(String normalized);
}
```

### Matching Flow

```
For each sentence:
  normalized = normalize(sentence)
  
  for key in dictionary:
    if normalized.contains(key):
      candidate = extract(word at key position)
      
      if termsSeen.contains(candidate):
        add label, continue
      
      if key.equals(candidate):
        addTerm(candidate), add label, continue
      
      lemma = getLemma(candidate)
      if lemmasSeen.contains(lemma):
        addTerm(candidate), add label, continue
      
      if key.equals(lemma):
        addTerm(candidate), addLemma(lemma), add label, continue
      
      if llmAdapter:
        if adapter.isFormOf(key, candidate):
          addTerm(candidate), addLemma(lemma), add label
```
     
## Checkpoint & Resume (Simplified)

No checkpoint files needed - state tracked through output markers.

**Implementation:** `CheckpointResolver.java` (dev.fisher.downloads.util)

Output Format (JSON Lines - one JSON per line):
```json
{"type": "forum_start", "forumUrl": "...", "timestamp": "..."}
{"type": "topic_start", "forumUrl": "...", "topicUrl": "..."}
{"type": "data", "forumUrl": "...", "topicUrl": "...", "lang": "ru", "text": "...", "labels": [...]}
{"type": "topic_end", "forumUrl": "...", "topicUrl": "..."}
{"type": "forum_end", "forumUrl": "..."}
```

**Marker Types:**
| Type | Description |
|------|-------------|
| `forum_start` | Starting to process a forum |
| `topic_start` | Starting to process a topic |
| `data` | A labeled sentence |
| `topic_end` | Topic fully processed |
| `forum_end` | Forum fully processed |

**On Start:**
1. Read output file (line-by-line)
2. Extract all `topicUrl` where `type == "topic_end"` → completed topics
3. Log completed topics count

**On Clean Shutdown:**
1. Nothing special needed - all topics have `topic_end`

**On Crash/Interrupt:**
1. On restart: CheckpointResolver.cleanupIncomplete() reads output
2. Finds blocks with `topic_start` but no `topic_end`
3. Deletes those incomplete lines
4. Resume starts fresh from remaining completed topics
2. Find blocks with `topic_start` but no matching `topic_end`
3. Delete those lines (incomplete blocks)
4. Resume will automatically skip completed topics

---

## Workflow

### First Run (Cold Start)

1. Load empty caches
2. For sentence: "На озере плавают карпики и карпы"
3. Candidate "карпики":
   - Not in dictionary
   - Not in negative cache
   - Lemma("карпики") = "карпи" ≠ "карп"
   - Not in positive cache
   - **LLM call**: "Is карпики a form of карп?" → TRUE
   - Add to positive cache (llm_verified)
4. Candidate "карпы":
   - Not in dictionary
   - Lemma("карпы") = "карп" = key
   - Add to positive cache (exact)
5. Labels: ["карп", "карп"]

### Second Run (Warm Cache)

Same sentence:
- "карпики" → found in positive cache (llm_verified) → no LLM call
- "карпы" → found in positive cache (exact) → no LLM call
- **Zero LLM calls needed**

### Run with New Word

Sentence: "поймал огромного карпища"
1. "карпища" → not in cache
2. Lemma = "карпищ" ≠ "карп"
3. LLM call → TRUE (augmentative form)
4. Add to positive cache

## Bounded Cache: Blocked Terms (LRU Eviction)

### Purpose
Handle ambiguous terms where dictionary word and proper noun are identical in form but differ in meaning.

Examples:
- `карп` (fish species) vs `Карп` (human name)
- `осётр` (fish) vs `Осётр` (place name)

### Behavior
1. Terms loaded from config file are pre-populated in blocked cache
2. During sentence processing, each candidate is checked (case-sensitive) against blocked cache
3. If found → skip processing that candidate, continue with sentence
4. If not found → process normally, then add to blocked cache
5. When limit reached (default: number of dictionary terms) → oldest entry evicted

### Implementation: LinkedHashMap with LRU

```java
private LinkedHashMap<String, Long> blockedTerms;
private int blockedTermsLimit;

blockedTerms = new LinkedHashMap<>(16, 0.75f, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
        return size() > blockedTermsLimit;
    }
};
```

### Configuration

```json
{
  "labeler": {
    "blockedTermsPath": "resources/dictionaries/blocked_terms.txt",
    "blockedTermsLimit": 100
  }
}
```

### File Format
Same as skip terms - one term per line, lines starting with `#` are comments:
```
# Human names that conflict with species
Карп
Осётр
```

| Scenario | Handling |
|----------|----------|
| Cache file missing | Create new empty cache |
| Cache file corrupted | Log warning, create new cache |
| LLM unavailable | Fail fast, don't process |
| LLM returns invalid response | Retry (up to 3x), then skip |
| Dictionary missing | Log error, exit |

## Performance Considerations

1. **Cache hit = O(1)** - No processing needed
2. **Lemma check = O(1)** - MyStem is fast
3. **LLM call = ~500ms** - Only for unknown words
4. **Expected LLM calls** - First run: ~10-20 per dictionary word; subsequent runs: ~0-2

## Testing Strategy

1. **Unit tests**:
   - CacheManager load/save
   - Word boundary regex
   - LLM prompt generation

2. **Integration tests**:
   - Full pipeline with mock LLM
   - Cache persistence

3. **E2E tests**:
   - Run on real forum data
   - Verify cache growth
   - Verify labeling accuracy

## Migration from Current System

1. Create new EnhancedSentencesLabeler class
2. Add CacheManager and LLMAdapter
3. Update Main.java to use enhanced labeler
4. Keep old stemmer as fallback
5. Run parallel to verify results
6. Switch over when stable

## Future Enhancements

1. **Batch LLM processing**: Send multiple candidates in one prompt
2. **Lemma caching**: Cache MyStem results too
3. **Multiple languages**: LLM handles language-specific rules
4. **Confidence scores**: Store LLM confidence, skip low-confidence
5. **Human review queue**: Flag uncertain matches for review
