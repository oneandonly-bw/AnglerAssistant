# System Architecture

## 1. Component Diagram

```
                    LabelerMain
                          │
                          ▼
              ConfigurationFacade
                          │
                          ▼
              ┌────────────────────┐
              │   DownloaderFactory │
              └─────────┬──────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                      BaseDownloader                               │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │TopicsListExtractor│  │TopicMetadata    │  │ HtmlCleaner │  │
│  │  (vBulletin,phpBB)│  │ Extractor       │  │   (Jsoup)   │  │
│  └──────────────────┘  └──────────────────┘  └──────────────┘  │
│                              │                                    │
│                              ▼                                    │
│              ┌──────────────────────────────┐                    │
│              │    SentencesLabeler         │                    │
│              │  ┌────────────┐ ┌─────────┐ │                    │
│              │  │Dictionary  │ │Cache    │ │                    │
│              │  │ Loader     │ │Manager  │ │                    │
│              │  └────────────┘ └─────────┘ │                    │
│              │  ┌────────────┐ ┌─────────┐ │                    │
│              │  │LLMAdapter  │ │Language │ │                    │
│              │  │ (optional) │ │ Config  │ │                    │
│              │  └────────────┘ └─────────┘ │                    │
│              └──────────────────────────────┘                    │
│                              │                                    │
│                              ▼                                    │
│                        OutputWriter                               │
│                    (JSON Lines)                                  │
└─────────────────────────────────────────────────────────────────┘
```

## 2. Packages

### 2.1 config (12 classes)

| Class | Type | Description |
|-------|------|-------------|
| `Configuration` | Record | Root config: meta, general, site, runtime, labeler, forums |
| `ConfigurationFacade` | Class | Entry point for loading config |
| `ConfigurationLoader` | Class | JSON parsing |
| `ConfigurationValidator` | Class | Schema validation |
| `LabelerConfiguration` | Record | Labeler settings |
| `ForumConfiguration` | Record | Forum settings (url, name, type, language) |
| `SiteConfiguration` | Record | Site settings (siteId, baseUrl) |
| `RuntimeConfiguration` | Record | Runtime settings (memory, retries) |
| `MetaConfiguration` | Record | Allowed types, filters, languages |
| `GeneralConfiguration` | Record | Logging level |
| `FilterConfig` | Record | Filter settings |

### 2.2 engine (1 class)

| Class | Description |
|-------|-------------|
| `BaseDownloader` | Main download orchestrator |

### 2.3 extractors (5 classes)

| Class | Description |
|-------|-------------|
| `ForumType` | Enum: PHPBB, VBULLETIN |
| `TopicsListExtractor` | Extract topic URLs from forum pages |
| `TopicMetadataExtractor` | Interface |
| `PhpBBTopicMetadataExtractor` | phpBB metadata extraction |
| `VBulletinTopicMetadataExtractor` | vBulletin metadata extraction |

### 2.4 factory (1 class)

| Class | Description |
|-------|-------------|
| `DownloaderFactory` | Creates IfDownloader with dependencies |

### 2.5 interfaces (2 classes)

| Class | Description |
|-------|-------------|
| `IfDownloader` | download(), bindAdapter(), bindExtractor() |
| `IfTopicLabeler` | processTopic(Topic) |

### 2.6 labeler (18 classes)

| Class | Description |
|-------|-------------|
| `SentencesLabeler` | Main labeling implementation |
| `OutputWriter` | JSON Lines output |
| `CountersManager` | Counters for dictionary/surface values |
| `CacheManager` | Term/lemma caching |
| `DictionaryLoader` | Dictionary JSON parsing |
| `LanguageConfig` | Language patterns (RU/EN/HE) |
| `LLMAdapter` | Interface for LLM validation |
| `LLMAdapterImpl` | LLM implementation |
| `LabeledSentence` | Output record |
| `LabelEntry` | Label entry record (with isValid) |
| `LabelingResult` | Result container |
| `LabelingMetadata` | Result metadata |
| `DictionaryEntry` | Dictionary entry |
| `DictValue` | Dictionary value with specificity |
| `Lemmatizer` | Interface |
| `RussianLemmatizer` | HTTP-based (Flask service) |
| `EnglishLemmatizer` | Passthrough |
| `HebrewLemmatizer` | Passthrough |
| `NoOpLemmatizer` | Passthrough |
| `LemmatizerFactory` | Creates lemmatizer by language |
| `MaxSentencesReachedException` | Exception |

### 2.7 llm (10 classes)

| Class | Description |
|-------|-------------|
| `LLMProvider` | Interface |
| `LLMProviderManager` | Manages multiple providers |
| `LLMProviderConfig` | Provider config |
| `LLMResponse` | Response record |
| `LLMException` | Exception |
| `GroqAdapter` | Groq provider |
| `OpenRouterAdapter` | OpenRouter provider |
| `HuggingFaceAdapter` | HuggingFace provider |
| `BaseLLMAdapter` | Base class |

### 2.8 mains (3 classes)

| Class | Description |
|-------|-------------|
| `LabelerMain` | Main CLI entry point |
| `PromptTester` | Test LLM prompts |

### 2.9 model (1 class)

| Class | Description |
|-------|-------------|
| `Topic` | Topic data model |

### 2.10 util (7+ classes)

| Class | Description |
|-------|-------------|
| `HtmlCleaner` | HTML to text |
| `PathsManager` | Path management |
| `SimilarityUtil` | String similarity |
| `FingerprintUtil` | Fingerprinting |
| `EnumUtil` | Enum utilities |
| `PrettyJsonReader` | JSON reading |

## 3. Data Flow

```
main(args)
    │
    ▼
LabelerMain.main()
    │
    ├─▶ Parse args: -config
    │
    ▼
ConfigurationFacade.getConfiguration(path)
    │
    ▼
for each forum:
    │
    ▼
DownloaderFactory.create(config, labeler)
    │
    ▼
BaseDownloader.download()
    │
    ├─▶ TopicsListExtractor.getTopicsList(forumUrl)
    │       │
    │       ▼
    │   for each topic:
    │       │
    │       ▼
    │   Download HTML (Jsoup)
    │       │
    │       ▼
    │   HtmlCleaner.cleanHtml()
    │       │
    │       ▼
    │   TopicMetadataExtractor.extractMetadata()
    │       │
    │       ▼
    │   SentencesLabeler.processTopic(topic)
    │       │
    │       ▼
    │   OutputWriter.write()
    │
    ▼
CheckpointResolver (cleanup on crash)
```

## 4. Key Interfaces

### IfDownloader
```java
public interface IfDownloader {
    void bindAdapter(TopicsListExtractor topicsListExtractor);
    void bindExtractor(IfTopicLabeler extractor);
    void download();
}
```

### IfTopicLabeler
```java
public interface IfTopicLabeler {
    void processTopic(Topic topic);
}
```

### LLMProvider
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

## 5. Configuration Record

```java
public record Configuration(
    MetaConfiguration meta,
    GeneralConfiguration general,
    SiteConfiguration site,
    RuntimeConfiguration runtime,
    LabelerConfiguration labeler,
    List<ForumConfiguration> forums
) {}
```

## 6. Output Format

### 6.1 Main Output (JSON Lines)

JSON Lines with markers:
```json
{"type": "forum_start", "forumUrl": "...", "timestamp": "..."}
{"type": "topic_start", "forumUrl": "...", "topicUrl": "..."}
{"type": "data", "forumUrl": "...", "topicUrl": "...", "lang": "ru", "text": "...", "labels": [...]}
{"type": "topic_end", "forumUrl": "...", "topicUrl": "..."}
{"type": "forum_end", "forumUrl": "..."}
```

### 6.2 Output Filename Generation

Auto-generated: `{siteId}_{dictionaryBaseName}.json`

Example:
- site: `israfish`, dict: `species_dict.json` → `output/israfish_species.json`
- site: `israfish`, dict: `methods_dict.json` → `output/israfish_methods.json`

### 6.3 Counters File

Auto-generated with timestamp: `{siteId}_{dictionaryBaseName}_{timestamp}.json`

Example: `output/israfish_species_2026-02-19_23-11-08.json`

## 7. Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Jsoup | 1.17.2 | HTML parsing |
| Jackson | 2.16.1 | JSON processing |
| Apache Commons Text | 1.12.0 | String similarity |
| Flask + pymorphy3 | - | Russian lemmatization (external service) |
| SLF4J | 2.0.7 | Logging |
| JUnit | 5.10.2 | Testing |
| Mockito | 5.11.0 | Mocking |
