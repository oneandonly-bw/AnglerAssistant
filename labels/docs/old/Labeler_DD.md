# Labeler Detailed Design (DD)

## 1. Overview

The Labeler extracts labeled sentences from forum topics. Given a topic's cleaned text content, it identifies sentences containing target terms from dictionaries and outputs structured JSON with labels.

## 2. Class Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                     <<interface>>                                │
│                      IfTopicLabeler                             │
├─────────────────────────────────────────────────────────────────┤
│ + processTopic(Topic): void                                    │
└─────────────────────────────────────────────────────────────────┘
                              ▲
                              │ implements
          ┌───────────────────┴───────────────────┐
          │                                      
 ┌─────────┴─────────┐               ┌───────────┴───────────┐
 │   SentencesLabeler    │               
 │   (main impl)         │               
 ├─────────────────────┤               ├───────────────────────┤
 │ - config              │               │                       │
 │ - dictionaryLoader    │               │                       │
 │ - languageConfig     │               │                       │
 │ + processTopic()    │               │                       │
 │ - extractSentences()│               │                       │
 │ - filterSentence()  │               │                       │
 │ - findLabels()      │               │                       │
 │ - extractContext()  │               │                       │
 └─────────────────────┘               └───────────────────────┘
```

## 3. Interface: IfTopicLabeler

```java
package dev.fisher.downloads.labeler;

import model.dev.aa.labeling.Topic;

/**
 * Interface for labeling topics with terms from dictionaries.
 */
public interface IfTopicLabeler {

    /**
     * Process a topic and extract labeled sentences.
     * @param topic Topic to process
     */
    void processTopic(Topic topic);
}
```

## 4. Main Implementation: SentencesLabeler

```java
package dev.fisher.downloads.labeler;

import dev.aa.labeling.labeler.DictionaryEntry;
import dev.aa.labeling.labeler.LabeledSentence;
import dev.aa.labeling.labeler.LabelingResult;
import dev.aa.labeling.labeler.LanguageConfig;
import dev.aa.labeling.model.Topic;
import interfaces.dev.aa.labeling.IfTopicLabeler;

public class SentencesLabeler implements IfTopicLabeler {

    // Configuration fields
    private final int minSentenceLength;
    private final double minLanguageRatio;
    private final double maxSpecialCharRatio;
    private final List<String> dictionaryPaths;
    private final Path outputDirectory;
    private final String outputFileName;

    // Runtime fields
    private final Map<String, List<DictionaryEntry>> dictionary;
    private LanguageConfig languageConfig;
    private final List<LabeledSentence> results;

    /**
     * Constructor - initializes labeler with configuration.
     * @param config LabelerConfig containing all settings
     */
    public SentencesLabeler(LabelerConfig config);

    /**
     * Process a single topic and extract labeled sentences.
     * Loads dictionaries if not already loaded, splits content into sentences,
     * filters sentences, finds labels, and extracts contexts for long sentences.
     * @param topic Topic to process
     */
    @Override
    public void processTopic(Topic topic);

    /**
     * Extract sentences from topic content that contain labels.
     * Splits content using language-specific sentence pattern, applies filters,
     * finds labels, and extracts context for long sentences.
     * @param content Raw text content from topic
     * @param source Source URL for tracking
     * @param labels List of dictionary entries to search for
     * @return List of labeled sentences
     */
    private List<LabeledSentence> extractSentences(String content, String source, List<DictionaryEntry> labels);

    /**
     * Filter a sentence based on multiple criteria.
     * Applies length, language ratio, special char ratio, and navigation menu filters.
     * @param sentence Sentence to filter
     * @param labels Dictionary entries (for language-specific processing)
     * @return true if sentence passes all filters
     */
    private boolean filterSentence(String sentence, List<DictionaryEntry> labels);

    /**
     * Check if sentence contains enough target language characters.
     * Uses language-specific word pattern to count target language chars.
     * @param sentence Sentence to check
     * @return true if language ratio exceeds minLanguageRatio threshold
     */
    private boolean isTargetLanguage(String sentence);

    /**
     * Check if sentence has too many special characters.
     * Counts non-alphanumeric, non-whitespace characters.
     * @param sentence Sentence to check
     * @return true if special char ratio exceeds maxSpecialCharRatio
     */
    private boolean hasTooManySpecialChars(String sentence);

    /**
     * Check if sentence appears to be a navigation menu item.
     * Rejects sentences with navigation keywords, excessive arrows/bullets, or many dates.
     * @param sentence Sentence to check
     * @return true if sentence appears to be navigation content
     */
    private boolean isNavigationMenu(String sentence);

    /**
     * Find all dictionary labels present in a sentence.
     * Sorts labels by priority (MOSTLY_USED > CANONICAL > VARIANT) before matching.
     * Uses word boundary regex for whole-word matching, case-insensitive.
     * @param sentence Sentence to search in
     * @param labels Dictionary entries to search for
     * @return List of matched label values
     */
    private List<String> findLabels(String sentence, List<DictionaryEntry> labels);

    /**
     * Extract shorter context around labels in long sentences.
     * For each label position, extracts 5 words before and 5 words after.
     * @param sentence Long sentence to extract context from
     * @param labels Labels found in sentence
     * @param source Source URL
     * @return List of context sentences
     */
    private List<LabeledSentence> extractContext(String sentence, List<String> labels, String source);

    /**
     * Find word positions of a term in a sentence.
     * Handles multiple occurrences and returns word indices.
     * @param sentence Sentence to search in
     * @param term Term to find
     * @return List of word positions (0-based word indices)
     */
    private List<Integer> findWordPositions(String sentence, String term);

    /**
     * Count words in text.
     * @param text Text to count words in
     * @return Word count
     */
    private int countWords(String text);

    /**
     * Load dictionaries from configured paths.
     * Parses JSON and populates dictionary map by language.
     */
    private void loadDictionaries();

    /**
     * Get the labeling result.
     * @return LabelingResult containing all sentences and metadata
     */
    public LabelingResult getResult();

    /**
     * Save result to JSON file.
     * @throws IOException if file write fails
     */
    public void saveResult() throws IOException;
}
```

## 5. Supporting Classes

### 5.1 DictionaryEntry
```java
public class DictionaryEntry {
    public final String value;
    public final String specificity;  // MOSTLY_USED, CANONICAL, VARIANT
    
    public DictionaryEntry(String value, String specificity);
}
```

### 5.2 LanguageConfig
```java
public class LanguageConfig {
    
    public enum Language {
        RU("ru", "[.!?]+(?=\\s+[A-ZА-Я]|$)", "[\\u0400-\\u04FF]+"),
        EN("en", "[.!?]+(?=\\s+[A-Z]|$)", "[a-zA-Z]+"),
        HE("he", "[.!?]+(?=\\s+[\\u0590-\\u05FF]|$)", "[\\u0590-\\u05FF]+");
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

### 5.3 LabeledSentence
```java
public class LabeledSentence {
    private final String sentence;
    private final String language;
    private final List<String> labels;
    private final String source;
    
    public LabeledSentence(String sentence, String language, List<String> labels, String source);
    
    public String getSentence();
    public String getLanguage();
    public List<String> getLabels();
    public String getSource();
}
```

### 5.4 LabelingResult
```java
public class LabelingResult {
    private final List<LabeledSentence> sentences;
    private final LabelingMetadata metadata;
    
    public LabelingResult(List<LabeledSentence> sentences, LabelingMetadata metadata);
    
    public List<LabeledSentence> getSentences();
    public LabelingMetadata getMetadata();
}
```

### 5.5 LabelingMetadata
```java
public class LabelingMetadata {
    private final String language;
    private final String extractionDate;
    private final int totalSentences;
    private final int totalLabelsLoaded;
    private final int totalTopicsProcessed;
}
```

### 5.6 LabelerConfig
```java
public record LabelerConfig(
    boolean enabled,
    int minSentenceLength,
    double minLanguageRatio,
    double maxSpecialCharRatio,
    List<String> dictionaryPaths,
    Path outputDirectory,
    String outputFileName
) {}
```

## 6. Algorithm: Sentence Filtering

```
filterSentence(sentence, labels):
    
    1. IF length(sentence) < minSentenceLength:
         RETURN false
         
    2. IF NOT isTargetLanguage(sentence):
         RETURN false
         
    3. IF hasTooManySpecialChars(sentence):
         RETURN false
         
    4. IF isNavigationMenu(sentence):
         RETURN false
         
    5. RETURN true
```

## 7. Algorithm: Label Finding

```
findLabels(sentence, labels):
    
    1. SORT labels by priority (MOSTLY_USED > CANONICAL > VARIANT)
    
    2. found = empty list
    3. lowerSentence = toLowerCase(sentence)
    
    4. FOR each label IN labels:
         pattern = "\\b" + escapeRegex(label.value) + "\\b"
         IF pattern matches in lowerSentence (case-insensitive, unicode):
              found.add(label.value)
              
    5. RETURN found
```

## 8. Algorithm: Context Extraction

```
extractContext(longSentence, labels, source):
    
    1. words = split(longSentence, " ")
    2. contexts = empty list
    
    3. FOR each label IN labels:
         positions = findWordPositions(longSentence, label)
         
         FOR each pos IN positions:
              start = max(0, pos - 5)
              end = min(words.length, pos + 6)
              context = join(words[start:end], " ")
              
              IF length(context) >= minSentenceLength:
                   contexts.add(LabeledSentence(context, language, [label], source + "(context)"))
                   
    9. RETURN contexts
```

## 9. Data Flow

```
Topic
   │
   ▼
┌────────────────────────┐
│ SentencesLabeler      │
│ .processTopic()       │
└──────────┬─────────────┘
           │
           ▼
┌────────────────────────┐
│ LanguageConfig         │
│ .sentencePattern.split()│ → sentences[]
└──────────┬─────────────┘
           │
           ▼
    ┌──────┴──────┐
    │  FOR each   │
    │  sentence  │
    └──────┴──────┘
           │
           ▼
┌────────────────────────┐
│ filterSentence()      │ ←── checks: length, language,
└──────────┬─────────────┘          special chars, navigation
           │
    ┌──────┴──────┐
    │  PASSED?    │
    └──────┴──────┘
      YES  │  NO
           ▼
┌────────────────────────┐
│ findLabels()          │ ←── dictionary lookup
└──────────┬─────────────┘
           │
    ┌──────┴──────┐
    │  LABELS?     │
    └──────┴──────┘
      YES  │  NO
           ▼       SKIP
┌────────────────────────┐
│ length > maxSentenceLengthForContext?         │
└──────────┬─────────────┘
      YES  │  NO
           ▼
┌────────────────────────┐
│ extractContext()      │
│ (5 words ± term)     │
└──────────┬─────────────┘
           │
           ▼
┌────────────────────────┐
│ LabeledSentence        │ ──► results list
└────────────────────────┘
```

## 10. JSON Output Format

```json
{
  "topic": "https://forum.israfish.co.il/viewtopic.php?t=8186",
  "labels": ["тилапия", "карп", "musht"],
  "data": "Text containing the fish species names..."
}
```

## 11. Dependencies

| Class | Dependencies |
|-------|-------------|
| SentencesLabeler | DictionaryLoader, LanguageConfig, LabelingResult |
| LanguageConfig | java.util.regex.Pattern |

## 12. Error Handling

- **Dictionary not found**: Log warning, continue with empty dictionary
- **Invalid JSON in dictionary**: Log error, skip invalid entries
- **No matching sentences**: Return empty result (not an error)
- **IO errors on save**: Throw IOException

## 13. Thread Safety

- **Not thread-safe**: Single instance per labeling run
- For parallel processing: Create separate instances

## 14. Testing Strategy

| Test | Description |
|------|-------------|
| testFilterByLength | Sentences < minLength filtered |
| testFilterByLanguageRatio | Non-target language filtered |
| testFilterBySpecialChars | High special char ratio filtered |
| testFilterNavigation | Navigation sentences filtered |
| testFindLabels | Labels found in sentence |
| testPriorityOrder | MOSTLY_USED before CANONICAL before VARIANT |
| testContextExtraction | 5 words extracted around term |
| testLongSentenceHandling | Long sentences split into contexts |
