# Labeling System - System Design Document

> **Note**: The old `SentencesLabeler` (stemmer-based, no LLM) has been removed. Use `EnhancedSentencesLabeler` (with LLM fallback) instead.

## 1. System Overview

### 1.1 Purpose

The Labeling System extracts labeled sentences from forum topics. Given cleaned text content from forum topics, it:
1. Identifies sentences containing target terms from dictionaries
2. Filters out irrelevant content (navigation, short sentences, wrong language)
3. Outputs structured JSON with topic URL, labels (keywords), and matched sentences

### 1.2 Usage

```bash
# Basic usage
java -jar labeling-system.jar --config config.json

# With specific config file
java -jar labeling-system.jar --config /path/to/config.json

# Process specific topic
java -jar labeling-system.jar --topic "https://forum.example.com/t=123"
```

### 1.3 Top-Level Description

The Labeling System sits after the Downloader in the data pipeline:

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Downloader │────▶│  Labeler   │────▶│   Output   │
│  (Topics)  │     │  (Labels)  │     │   (JSON)   │
└─────────────┘     └─────────────┘     └─────────────┘
```

---

## 2. Main Components

### 2.1 Component Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      Labeling System                              │
├─────────────────────────────────────────────────────────────────┤
│  ┌───────────────┐    ┌───────────────┐    ┌───────────────┐  │
│  │ IfTopicLabeler│    │   Config     │    │  Dictionary   │  │
│  │  (Interface)  │    │  (JSON/Record)│    │   (JSON)      │  │
│  └───────────────┘    └───────────────┘    └───────────────┘  │
│           │                  │                   │              │
│           ▼                  ▼                   ▼              │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │               SentencesLabeler                          │   │
│  │  (Main Implementation)                                  │   │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐  │   │
│  │  │  Sentence  │ │   Label    │ │    Context     │  │   │
│  │  │  Splitter  │ │  Matcher   │ │  Extractor    │  │   │
│  │  └─────────────┘ └─────────────┘ └─────────────────┘  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Component List

| Component | Type | Responsibility |
|-----------|------|----------------|
| IfTopicLabeler | Interface | Defines labeling contract |
| SentencesLabeler | Class | Main implementation, orchestrates flow |
| SentenceFilter | Class | Applies filtering rules |
| LabelMatcher | Class | Finds dictionary terms in text |
| ContextExtractor | Class | Extracts context around labels |
| LanguageConfig | Class | Language-specific patterns |
| DictionaryLoader | Class | Loads and parses dictionaries |
| LabelingResult | Class | Output data structure |
| OutputWriter | Class | Writes results to JSON |

---

## 3. Configuration

### 3.1 JSON Configuration Structure

```json
{
  "labeler": {
    "enabled": true,
    "minSentenceLength": 15,
    "maxSentenceLengthForContext": 200,
    "minLanguageRatio": 0.3,
    "maxSpecialCharRatio": 0.2,
    "dictionaryPaths": [
      "resources/dictionaries/species_dict.json"
    ],
    "blockedTermsPath": "resources/dictionaries/blocked_terms.txt",
    "blockedTermsLimit": 100,
    "outputDirectory": "output/labels",
    "outputFileName": "labels.json"
  }
}
```

### 3.2 Configuration Fields

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `labeler` | Object | Yes | - | Labeler configuration container |
| `enabled` | boolean | Yes | - | Enable or disable labeling |
| `minSentenceLength` | integer | No | 15 | Minimum sentence character length |
| `maxSentenceLengthForContext` | integer | No | 200 | Max sentence length before context extraction |
| `minLanguageRatio` | number | No | 0.3 | Min % of target language chars (0.0-1.0) |
| `maxSpecialCharRatio` | number | No | 0.2 | Max % of special chars allowed (0.0-1.0) |
| `dictionaryPaths` | array | No | [] | List of dictionary JSON file paths |
| `blockedTermsPath` | string | No | null | Path to blocked terms file (LRU bounded cache) |
| `blockedTermsLimit` | integer | No | 1000 | Max size of blocked terms LRU cache |
| `outputDirectory` | string | No | "output/labels" | Output directory path |
| `outputFileName` | string | No | "labels.json" | Output filename |

---

## 4. Component Details

### 4.1 IfTopicLabeler

**Package**: `dev.fisher.downloads.labeler`

```java
public interface IfTopicLabeler {
    void processTopic(Topic topic);
}
```

---

### 4.2 SentencesLabeler

**Package**: `dev.fisher.downloads.labeler`

**Label Matching Algorithm**:
1. Build dictionary root map: stem each dictionary term → store (root → term)
2. For each sentence, search for any dictionary root in text
3. Extract word at found position, stem it
4. Compare stemmed roots using Levenshtein similarity
5. If similarity ≥ DEFAULT_SIMILARITY_THRESHOLD (0.85), accept match
6. Output original dictionary term as label

This ensures:
- "карпик", "карпы" match dictionary "карп" (similarity > 0.85)
- "сомневался" does NOT match "сом" (similarity < 0.85)

#### Word Boundary Detection

Word boundaries are validated on both sides of the matched text:

**Start boundary** (index = start of match):
- Position 0 (start of string)
- Previous char is NOT a letter/digit
  Previous char is space character
- Previous char is punctuation: `, . ! ? ; : - " ' ) ]`

**End boundary** (index = end of match):
- Position >= length (end of string)
- Next char is NOT a letter/digit
- Next char is punctuation: `, . ! ? ; : - " ' ) ]`

This handles variations like:
- ` карп ` (spaces on both sides)
- `карп,` (comma after word)
- `,карп` (comma before word)
- `Карп.` (capital after period)

#### Output Format

The output JSON uses **cleaned text** (no double spaces, no special characters) for both `text` field and `labels[].start/end` positions:

```json
{
  "topicUrl": "https://forum.example.com/t=123",
  "lang": "ru",
  "text": "Я ловлю карпа на червя",
  "labels": [
    { "surface": "карпа", "canonical": "карп", "variant": null, "start": 12, "end": 16 }
  ]
}
```

- `text`: Cleaned string with single spaces only
- `labels[].start/end`: Character positions (0-based) in the cleaned text

#### Context Extraction

When sentence exceeds `maxSentenceLengthForContext`, context windows are extracted:
- Uses **cleaned text** (not raw) to ensure position consistency
- Window: 5 words before + 5 words after the label

```java
public class SentencesLabeler implements IfTopicLabeler {
    
    public SentencesLabeler(LabelerConfig config);
    
    @Override
    public void processTopic(Topic topic);
    
    public LabelingResult getResult();
    
    public void saveResult() throws IOException;
}
```

---

### 4.3 SentenceFilter

**Package**: `dev.fisher.downloads.labeler`

```java
public class SentenceFilter {
    
    /**
     * Creates a SentenceFilter with given thresholds.
     * @param minSentenceLength Minimum character length
     * @param minLanguageRatio Minimum target language character ratio
     * @param maxSpecialCharRatio Maximum special character ratio
     */
    public SentenceFilter(int minSentenceLength, double minLanguageRatio, double maxSpecialCharRatio);
    
    /**
     * Test if a sentence passes all filters.
     * Applies length, language ratio, special char, and navigation filters.
     * @param sentence Sentence to test
     * @param languageConfig Language-specific configuration
     * @return true if sentence passes all filters
     */
    public boolean test(String sentence, LanguageConfig languageConfig);
    
    /**
     * Check if sentence meets minimum length requirement.
     * @param sentence Sentence to check
     * @return true if length >= minSentenceLength
     */
    private boolean passesLengthFilter(String sentence);
    
    /**
     * Check if sentence has sufficient target language characters.
     * @param sentence Sentence to check
     * @param languageConfig Language configuration with word pattern
     * @return true if language ratio >= minLanguageRatio
     */
    private boolean passesLanguageFilter(String sentence, LanguageConfig languageConfig);
    
    /**
     * Check if sentence has acceptable special character ratio.
     * @param sentence Sentence to check
     * @return true if special char ratio <= maxSpecialCharRatio
     */
    private boolean passesSpecialCharFilter(String sentence);
    
    /**
     * Check if sentence is a navigation menu item.
     * @param sentence Sentence to check
     * @return true if sentence appears to be navigation content
     */
    private boolean isNavigationMenu(String sentence);
}
```

---

### 4.4 LabelMatcher

**Package**: `dev.fisher.downloads.labeler`

```java
public class LabelMatcher {
    
    /**
     * Creates a LabelMatcher for given labels.
     * @param labels Dictionary entries to search for
     */
    public LabelMatcher(List<DictionaryEntry> labels);
    
    /**
     * Find all labels present in a sentence.
     * Sorts labels by priority before matching.
     * Uses word boundary regex, case-insensitive.
     * @param sentence Sentence to search
     * @return List of matched label values
     */
    public List<String> findLabels(String sentence);
    
    /**
     * Sort labels by priority order.
     * Priority: MOSTLY_USED > CANONICAL > VARIANT
     */
    private void sortLabelsByPriority();
    
    /**
     * Check if a label matches in the sentence.
     * @param label Label value to search
     * @param sentence Sentence to search in
     * @return true if label found as whole word
     */
    private boolean matches(String label, String sentence);
}
```

---

### 4.5 ContextExtractor

**Package**: `dev.fisher.downloads.labeler`

**Policy**: Sentences longer than `maxSentenceLengthForContext` characters are processed differently - context (surrounding words) is extracted around each label rather than using the full sentence. This ensures extracted sentences are concise and focused.

```java
public class ContextExtractor {
    
    /**
     * Creates a ContextExtractor with given context window size.
     * @param contextWindow Number of words to extract before/after label
     */
    public ContextExtractor(int contextWindow);
    
    /**
     * Extract context around labels in long sentences.
     * For each label position, extracts words before and after.
     * @param sentence Long sentence to extract context from
     * @param labels Labels found in sentence
     * @param source Source URL for tracking
     * @param minLength Minimum context length required
     * @return List of context sentences
     */
    public List<LabeledSentence> extract(String sentence, List<String> labels, String source, int minLength);
    
    /**
     * Find positions of a term in a sentence.
     * @param sentence Sentence to search in
     * @param term Term to find
     * @return List of word positions
     */
    private List<Integer> findWordPositions(String sentence, String term);
    
    /**
     * Extract words around a position.
     * @param sentence Source sentence
     * @param position Word position to center on
     * @return Context string
     */
    private String extractWindow(String sentence, int position);
}
```

---

### 4.6 LanguageConfig

**Package**: `dev.fisher.downloads.labeler`

```java
public class LanguageConfig {
    
    public enum Language {
        RU("ru", "[.!?]+(?=\\s+[A-ZА-Я]|$)", "[\\u0400-\\u04FF]+"),
        EN("en", "[.!?]+(?=\\s+[A-Z]|$)", "[a-zA-Z]+"),
        HE("he", "[.!?]+(?=\\s+[\\u0590-\\u05FF]|$)", "[\\u0590-\\u05FF]+");
        
        private final String code;
        private final String sentencePattern;
        private final String wordPattern;
        
        Language(String code, String sentencePattern, String wordPattern);
    }
    
    /**
     * Get LanguageConfig for a specific language.
     * @param lang Language code (ru, en, he)
     * @return LanguageConfig for the language
     */
    public static LanguageConfig forLanguage(String lang);
    
    public String getLanguageCode();
    public Pattern getSentencePattern();
    public Pattern getWordPattern();
}
```

---

### 4.7 DictionaryLoader

**Package**: `dev.fisher.downloads.labeler`

**Dictionary Format**:
```json
{
  "metadata": {
    "version": "0.1",
    "last_update": "2026-01-22",
    "dictionary_specific": {
      "type_enum": {
        "SPECIES": "Fish and aquatic species"
      },
      "water_type_enum": {
        "FRESHWATER": "Freshwater type",
        "SALTWATER": "Saltwater type"
      },
      "property_definitions": {
        "venomous": "Organism INJECTS toxin via spines/stings",
        "toxic": "Organism IS poisonous if INGESTED"
      }
    },
    "global_definitions": {
      "specificity_definitions": {
        "MOSTLY_USED": "Name most commonly used by users",
        "CANONICAL": "Primary name per language; exactly one per UID",
        "VARIANT": "Refinement of canonical; same UID"
      }
    }
  },
  "data": [
    {
      "uid": "species_001",
      "type": "SPECIES",
      "en": [
        { "value": "carp", "specificity": "CANONICAL" },
        { "value": "common carp", "specificity": "VARIANT" }
      ],
      "ru": [
        { "value": "карп", "specificity": "CANONICAL" }
      ],
      "he": [
        { "value": "קרפיון", "specificity": "MOSTLY_USED" }
      ]
    }
  ]
}
```

```java
public class DictionaryLoader {
    
    /**
     * Creates a DictionaryLoader with object mapper for JSON parsing.
     * @param objectMapper Jackson ObjectMapper for JSON parsing
     */
    public DictionaryLoader(ObjectMapper objectMapper);
    
    /**
     * Load dictionaries from multiple paths.
     * @param paths List of dictionary file paths
     * @return Map of language code to list of dictionary entries
     */
    public Map<String, List<DictionaryEntry>> loadDictionaries(List<String> paths);
    
    /**
     * Load a single dictionary file.
     * @param path Path to dictionary JSON file
     * @return List of dictionary entries from file
     */
    private List<DictionaryEntry> loadDictionary(String path);
    
    /**
     * Parse dictionary entry from JSON node.
     * @param node JSON node containing entry data
     * @return DictionaryEntry
     */
    private DictionaryEntry parseEntry(JsonNode node);
}
```

---

### 4.8 LabelingResult

**Package**: `dev.fisher.downloads.labeler`

```java
public class LabelingResult {
    
    public LabelingResult(List<LabeledSentence> sentences, LabelingMetadata metadata);
    
    public List<LabeledSentence> getSentences();
    
    public LabelingMetadata getMetadata();
}
```

### 4.8.1 LabeledSentence

```java
public record LabeledSentence(
    String topicUrl,
    String lang,
    String text,           // Cleaned text (no special chars, single spaces)
    List<LabelPosition> labels
) {}
```

### 4.8.1.1 LabelPosition

```java
public record LabelPosition(
    String label,  // Dictionary term found (e.g., "карп")
    int start,    // Character start index in cleaned text
    int end       // Character end index in cleaned text
) {}
```

### 4.8.2 LabelingMetadata

```java
public class LabelingMetadata {
    
    public LabelingMetadata(String language, int totalSentences, int totalLabelsLoaded, int totalTopicsProcessed);
    
    public String getLanguage();
    public int getTotalSentences();
    public int getTotalLabelsLoaded();
    public int getTotalTopicsProcessed();
}
```

---

### 4.9 OutputWriter

**Package**: `dev.fisher.downloads.labeler`

**Output Format** (JSON Lines - one JSON per line, with markers for checkpoint/resume):
```json
{"type": "forum_start", "forumUrl": "https://forum.example.com/f=1", "timestamp": "2026-02-18T10:00:00Z"}
{"type": "topic_start", "forumUrl": "https://forum.example.com/f=1", "topicUrl": "https://forum.example.com/t=123"}
{"type": "data", "forumUrl": "https://forum.example.com/f=1", "topicUrl": "https://forum.example.com/t=123", "lang": "ru", "text": "Я ловлю карпа на червя", "labels": [{"surface":"карпа","canonical":"карп","variant":null,"start":12,"end":16}]}
{"type": "topic_end", "forumUrl": "https://forum.example.com/f=1", "topicUrl": "https://forum.example.com/t=123"}
{"type": "forum_end", "forumUrl": "https://forum.example.com/f=1"}
```

**Marker Types:**
| Type | Description |
|------|-------------|
| `forum_start` | Starting to process a forum |
| `topic_start` | Starting to process a topic |
| `data` | A labeled sentence |
| `topic_end` | Topic fully processed |
| `forum_end` | Forum fully processed |

**Resume Logic:**
- On start: read output (line-by-line), extract topicUrl where type=="topic_end" → completed
- On crash: find blocks with topic_start but no topic_end → delete them

**Benefits:**
- Line-by-line reading (no memory issues with huge files)
- Self-describing output
- Easy cleanup: delete lines with incomplete topicUrl

**Legacy Format** (without markers):
```json
{
  "topicUrl": "https://forum.example.com/t=123",
  "lang": "ru",
  "text": "Я ловлю карпа на червя",
  "labels": [
    { "surface": "карпа", "canonical": "карп", "variant": null, "start": 12, "end": 16 }
  ]
}
```

- `text`: Original sentence with all special characters removed, double spaces collapsed
- `labels`: Array of label objects with character positions (start/end indices)
- Positions are 0-based character indices in the cleaned text

```java
public class OutputWriter {
    
    /**
     * Creates an OutputWriter for given output path.
     * @param outputDirectory Directory for output files
     * @param outputFileName Base filename for output
     */
    public OutputWriter(Path outputDirectory, String outputFileName);
    
    /**
     * Write a single labeled sentence to output.
     * Each line is a separate JSON object (JSON Lines format).
     * @param topicUrl Source topic URL
     * @param lang Language code (ru/en/he)
     * @param sentence Labeled sentence text
     * @param labels List of labels found in sentence
     * @throws IOException if write fails
     */
    public void writeSentence(String topicUrl, String lang, String sentence, List<String> labels) throws IOException;
    
    /**
     * Flush and close output streams.
     */
    public void close() throws IOException;
}
```

---

## 5. Supporting Classes

### 5.1 DictionaryEntry

```java
public class DictionaryEntry {
    public final String value;
    public final String specificity;
    
    public DictionaryEntry(String value, String specificity);
}
```

### 5.2 LabelerConfig

```java
public record LabelerConfig(
    boolean enabled,
    int minSentenceLength,
    int maxSentenceLengthForContext,
    double minLanguageRatio,
    double maxSpecialCharRatio,
    List<String> dictionaryPaths,
    Path outputDirectory,
    String outputFileName
) {}
```

---

## 6. Data Flow Summary

```
INPUT: Topic with cleaned content
  │
  ▼
LanguageConfig.forLanguage(language)
  │
  ▼
SentenceSplitter.split(content) → sentences[]
  │
  ▼
FOR each sentence
  │
  ▼
SentenceFilter.test(sentence, languageConfig)
  │
  ├─▶ REJECTED ──▶ Next sentence
  │
  ▼
PASSED
  │
  ▼
LabelMatcher.findLabels(sentence)
  │
  ├─▶ NO LABELS ──▶ Next sentence
  │
  ▼
HAS LABELS
  │
  ├─▶ length > maxSentenceLengthForContext ──▶ ContextExtractor.extract()
  │                              │
  │◀─────────────────────────────┤
  │
  ▼
LabeledSentence created
  │
  ▼
OutputWriter.write(result)
  │
  ▼
OUTPUT: JSON file
```

---

## 7. Error Handling

| Error | Handling |
|-------|----------|
| Dictionary not found | Log warning, continue with empty dictionary |
| Invalid JSON in dictionary | Log error, skip invalid entries |
| No matching sentences | Return empty result |
| IO errors on save | Throw IOException |

---

## 8. Threading Model

- **Single-threaded**: Processes topics sequentially
- One topic at a time, no parallel processing
