# Top Level Design

## 1. Functional Requirements

### 1.1 Core Functions

| Function | Input | Output | Description |
|----------|-------|--------|-------------|
| Download topics | Forum URL | Raw HTML | Fetch forum topic pages |
| Clean HTML | Raw HTML | Clean text | Remove HTML, extract text |
| Extract metadata | Raw HTML | Topic object | Parse author, date, title |
| Split sentences | Text | Sentence list | Segment by language rules |
| Filter sentences | Sentences | Filtered list | Remove noise, navigation |
| Match labels | Sentence | Labeled sentence | Dictionary + lemma + LLM |
| Write output | Labeled data | JSON file | JSON Lines format |

### 1.2 Label Matching

Algorithm in `SentencesLabeler.findLabels()`:
1. For each dictionary entry
2. For each value (canonical + variants)
3. Scan **entire sentence** - label **all occurrences**
4. Check: exact match → lemma contains key → lemma length == key → LLM fallback
5. Create label for **all** candidates (accepted + rejected with isValid flag)
6. Add to validLabels (isValid=true) or invalidLabels (isValid=false)
7. Cache results (terms + lemmas)

## 2. Module Design

### 2.1 Configuration Module

```
ConfigurationFacade.getConfiguration(path)
       │
       ▼
ConfigurationLoader.load()
       │
       ├─▶ Validate against schema
       │
       ▼
Configuration (Record)
       │
       ├─▶ MetaConfiguration
       ├─▶ GeneralConfiguration  
       ├─▶ SiteConfiguration
       ├─▶ RuntimeConfiguration
       ├─▶ LabelerConfiguration
       └─▶ List<ForumConfiguration>
```

### 2.2 Downloader Module

```
BaseDownloader
       │
       ├─▶ TopicsListExtractor
       │      ├─▶ getTopicsList(ForumType.VBULLETIN, url)
       │      └─▶ getTopicsList(ForumType.PHPBB, url)
       │
       ├─▶ TopicMetadataExtractor
       │      ├─▶ VBulletinTopicMetadataExtractor
       │      └─▶ PhpBBTopicMetadataExtractor
       │
       └─▶ HtmlCleaner.cleanHtml()
```

### 2.3 Labeler Module

```
SentencesLabeler.processTopic(topic)
       │
       ├─▶ LanguageConfig.forLanguage(lang)
       │
       ├─▶ Split content by sentence pattern
       │
       ├─▶ For each sentence:
       │      ├─▶ Length check (≥ minSentenceLength)
       │      ├─▶ Language ratio check (≥ minLanguageRatio)
       │      ├─▶ Special char check (≤ maxSpecialCharRatio)
       │      │
       │      ▼
       │      findLabels(originalText, cleanedText)
       │      │
       │      ├─▶ For each dictionary entry
       │      │      ├─▶ Exact match in cache? → validLabels
       │      │      ├─▶ Exact match word? → validLabels
       │      │      ├─▶ Lemma contains key? → if no → invalidLabels
       │      │      ├─▶ Lemma length == key length? → validLabels
       │      │      └─▶ LLM fallback → validLabels / invalidLabels
       │      │
       │      ▼
       │      OutputWriter.writeData()
       │
       └─▶ CacheManager.save() (every 5 sentences)
```

### 2.4 LLM Module

```
LLMProviderManager
       │
       ├─▶ GroqAdapter
       ├─▶ OpenRouterAdapter
       └─▶ HuggingFaceAdapter

LLMAdapter.isFormOf(base, candidate, lang)
       │
       ▼
Prompt: "TRUE if form of fish, FALSE otherwise"
       │
       ▼
Parse response (TRUE/FALSE)
       │
       ▼
Cache result
```

## 3. Data Structures

### 3.1 Configuration

```java
record Configuration(
    MetaConfiguration meta,
    GeneralConfiguration general,
    SiteConfiguration site,
    RuntimeConfiguration runtime,
    LabelerConfiguration labeler,
    List<ForumConfiguration> forums
)
```

### 3.2 Topic

```java
class Topic {
    // Immutable identification
    private final String sourceName;    // siteId
    private final String siteName;
    private final String forumName;
    private final String topicUrl;
    private final String topicId;
    
    // Content
    private String content;              // Raw HTML
    private String cleanedContent;       // Clean text
    
    // Metadata
    private String author;
    private LocalDateTime creationDate;
    private String language;
    private ProcessingStatus status;
    private Set<ErrorFlag> errorFlags;
}
```

### 3.3 ForumConfiguration

**Note**: Output is defined at labeler level, not per-forum.

```java
record ForumConfiguration(
    String url,           // Forum URL
    String forumName,    // Display name
    String path,         // Storage path category
    boolean enabled,     // Enable/disable
    String forumType,    // PHPBB, VBULLETIN
    String language,     // RU, EN, HE
    List<FilterConfig> include,
    List<FilterConfig> exclude
)
```

### 3.4 LabeledSentence

```java
record LabeledSentence(
    String forumUrl,       // Source forum
    String topicUrl,       // Source topic
    String lang,           // Language code
    String text,           // Cleaned sentence
    List<LabelEntry> validLabels,    // All accepted labels (isValid = true)
    List<LabelEntry> invalidLabels   // All rejected labels (isValid = false)
)
```

### 3.5 LabelEntry

```java
record LabelEntry(
    String surface,     // Exact word from text (e.g., "карпик")
    String canonical,  // Dictionary canonical (e.g., "карп")
    String variant,    // If VARIANT/MOSTLY_USED
    int start,        // Char position in cleaned text
    int end,          // Char position in cleaned text
    boolean isValid   // TRUE if accepted, FALSE if rejected
)
```

### 3.5 DictionaryEntry

```java
class DictionaryEntry {
    private final String uid;
    private final String type;           // SPECIES, METHOD, etc.
    private final Map<String, List<DictValue>> valuesByLanguage;
}

record DictValue(
    String value,           // e.g., "карп"
    String specificity     // CANONICAL, VARIANT, MOSTLY_USED
)
```

## 4. Interface Definitions

### 4.1 IfDownloader

```java
public interface IfDownloader {
    void bindAdapter(TopicsListExtractor topicsListExtractor);
    void bindExtractor(IfTopicLabeler extractor);
    void download();
}
```

### 4.2 IfTopicLabeler

```java
public interface IfTopicLabeler {
    void processTopic(Topic topic);
}
```

### 4.3 LLMProvider

```java
public interface LLMProvider {
    String getName();
    String getModel();
    boolean isEnabled();
    int getPriority();
    LLMResponse chat(String systemPrompt, String userMessage);
    boolean isAvailable();
    void close();
}
```

### 4.4 LLMAdapter

```java
public interface LLMAdapter {
    boolean isFormOf(String baseWord, String candidate, String language);
}
```

## 5. Configuration Parameters

### 5.1 LabelerConfiguration

| Field | Default | Description |
|-------|---------|-------------|
| enabled | true | Enable labeling |
| minSentenceLength | 15 | Min characters |
| maxSentenceLengthForContext | 200 | Max before context extraction |
| minLanguageRatio | 0.3 | Target language char ratio |
| maxSpecialCharRatio | 0.2 | Max special chars |
| dictionaryPaths | [] | Dictionary JSON files |
| blockedTermsLimit | 1000 | LRU cache size |
| language | null | Target language |
| forumName | null | Forum identifier |
| maxSentences | 0 | Limit (0 = unlimited) |

### 5.2 RuntimeConfiguration

| Field | Default | Description |
|-------|---------|-------------|
| memoryThreshold | 0.8 | Max memory usage |
| maxRetries | 3 | HTTP retry attempts |

## 6. Checkpoint & Resume

### 6.1 CheckpointResolver

```java
public class CheckpointResolver {
    public record CompletedTopics(Set<String>, Set<String>) {}
    
    public static CompletedTopics loadCompleted(Path outputFile)
    public static Set<String> findIncompleteTopics(Path outputFile)
    public static void cleanupIncomplete(Path outputFile)
}
```

### 6.2 Resume Flow

```
LabelerMain(args)
       │
       ├─▶ -resume flag = true?
       │
       ▼
CheckpointResolver.loadCompleted(outputFile)
       │
       ▼
BaseDownloader.setSkipTopicUrls(completed)
       │
       ▼
Skip topics in skipTopicUrls
```

### 6.3 Markers

| Type | Description |
|------|-------------|
| forum_start | Starting forum |
| topic_start | Starting topic |
| data | Labeled sentence |
| topic_end | Completed topic |
| forum_end | Completed forum |

## 7. Error Handling

| Error | Handling |
|-------|----------|
| HTTP timeout | Retry (max 3) |
| HTTP 429 | Wait, retry |
| Dictionary missing | Log warning, continue |
| LLM unavailable | Skip, continue without |
| Output write fail | Abort |
| Invalid config | Abort |

## 8. Caching

### 8.1 CacheManager

```java
public class CacheManager {
    private final Path termsPath;    // terms_seen.txt
    private final Path lemmasPath;   // lemmas_seen.txt
    
    public boolean containsTerm(String term)
    public boolean containsLemma(String lemma)
    public void addTerm(String term)
    public void addLemma(String lemma)
    public void load() / save()
}
```

### 8.2 CountersManager

Tracks counts of dictionary values and surface forms found during labeling.

**Output filename format**: `{siteId}_{dictionaryBaseName}_{timestamp}.json`

Example:
- site: `israfish`, dict: `species_dict.json` → `israfish_species_2026-02-19_23-11-08.json`

```json
{
  "dictionary": [
    { "value": "карп", "found": 100 },
    { "value": "мушт", "found": 50 }
  ],
  "text": [
    { "value": "карпы", "found": 15 },
    { "value": "мушты", "found": 100 }
  ]
}
```

- **dictionary**: Tracks dictionary values (canonical, variants, mostly_used)
- **text**: Tracks surface forms (actual words found in text)
- **Unique filename**: If file exists, increments timestamp (sec → min → hour)
- Saves on `SentencesLabeler.close()`

```java
public class CountersManager {
    public CountersManager(Path outputDirectory, String outputFileName)
    public void incrementDictionary(String value)
    public void incrementSurface(String value)
    public void save()
    public int getDictionaryTotal()
    public int getSurfaceTotal()
    public Path getCountersPath()
}
```

### 8.3 Two-Level Rejection

1. **blockedTerms** (persistent, HashSet)
   - Loaded from config file
   - Case-sensitive
   - Human names that conflict with species

2. **rejectedTerms** (runtime, LRU)
   - Added during processing
   - LinkedHashMap with eviction
   - Default limit: 1000

## 9. Build

```bash
mvn clean compile    # Compile
mvn test            # Run tests (173)
mvn package         # Build JAR
```

## 10. Run

```bash
# Default
java -jar app.jar

# Custom config
java -jar app.jar -config israfish.json

# Resume
java -jar app.jar -config israfish.json -resume
```
