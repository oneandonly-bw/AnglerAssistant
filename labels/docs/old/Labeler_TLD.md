# Labeler Top-Level Design (TLD)

## Overview
The Labeler module extracts sentences containing target terms (labels) from downloaded forum topics. It filters sentences based on language detection, length, and special character ratios, then outputs structured JSON results.

## Architecture

### Flow
```
Topic (from Downloader) → Sentence Splitter → Filter → Term Matcher → Context Extraction → JSON Output
```

### Key Components
1. **IfTopicLabeler** - Main interface for topic processing
2. **SentencesLabeler** - Implementation that extracts sentences containing target terms
3. **LabelingResult** - Result structure with labeled sentences and metadata

## Interface

### IfTopicLabeler
```java
public interface IfTopicLabeler {
    void processTopic(Topic topic);
}
```

### Topic Input
```java
public class Topic {
    private final String sourceName;       // e.g., "israfish"
    private final String siteName;         // e.g., "IsraFish Forum"
    private final String forumName;        // e.g., "Пресноводная Рыбалка"
    private final String topicUrl;        // Full URL
    private final String topicId;         // Numeric ID
    
    private String content;               // Cleaned text content
    private String title;                 // Extracted title
    private String author;               // Author name
    private LocalDateTime creationDate;  // Post date
    private String subcategory;          // e.g., "fresh_water"
    private String language;              // RU, EN, or HE
    
    private ProcessingStatus status;
}
```

## Configuration

### JSON Configuration Structure
Labeling settings are in config JSON under `labeler` section:

```json
{
  "labeler": {
    "enabled": true
  }
}
```

### Additional Labeler Config (future expansion)
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
    "outputDirectory": "output/labeled",
    "outputFileName": "labels.json"
  }
}
```

### Configuration Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `enabled` | boolean | true | Enable labeler |
| `minSentenceLength` | int | 15 | Minimum sentence character length |
| `minLanguageRatio` | number | 0.3 | Min % of target language chars (0.0-1.0) |
| `maxSpecialCharRatio` | number | 0.2 | Max % of special chars allowed (0.0-1.0) |
| `dictionaryPaths` | array | [] | List of dictionary JSON files |
| `outputDirectory` | string | "output" | Output directory path |
| `outputFileName` | string | "labels.json" | Output filename |

### Language-Specific Settings
Hardcoded per language (selected by forum's `language` field):

| Language | Sentence Pattern | Word Pattern | Unicode Range |
|----------|----------------|--------------|---------------|
| RU | `[.!?]+(?=\s+[A-ZА-Я]\|$)` | `\u0400-\u04FF` | Cyrillic |
| EN | `[.!?]+(?=\s+[A-Z]\|$)` | `a-zA-Z` | Latin |
| HE | `[.!?]+(?=\s+[\u0590-\u05FF]\|$)` | `\u0590-\u05FF` | Hebrew |

## Labeling Process

### Step 1: Sentence Splitting
- Split content into sentences using language-specific regex
- Clean text (normalize whitespace)

### Step 2: Language Validation
- Count target language characters in sentence
- Calculate ratio: `languageChars / totalChars`
- Reject if ratio < `minLanguageRatio`

### Step 3: Length Validation
- Reject if length < `minSentenceLength`

### Step 4: Special Character Filter
- Count non-alphanumeric, non-whitespace characters
- Reject if ratio > `maxSpecialCharRatio`

### Step 5: Navigation Menu Filter
Reject sentences containing:
- Navigation keywords: "вернуться в", "перейти", "важные сообщения", "чемпионаты", etc.
- Excessive arrows (↳, →, ←) > 3
- Excessive bullets (•, ·, ▪) > 2
- Many dates (> 5 year patterns)

### Step 6: Target Term Matching
- Load target terms from dictionaries
- Search for terms using word boundary matching (`\bterm\b`)
- Case-insensitive, Unicode-aware

### Step 7: Long Sentence Handling
If sentence > maxSentenceLengthForContext chars:
- Extract context (5 words before + 5 words after each target term)
- Create shorter extracted sentences

### Step 8: Label Prioritization
From dictionary, labels have specificity:
- **MOSTLY_USED** - most common name (highest priority)
- **CANONICAL** - primary name
- **VARIANT** - refinement (lowest priority)

Priority order: MOSTLY_USED > CANONICAL > VARIANT

## Output Format

### JSON Output
```json
{
  "topic": "https://forum.israfish.co.il/viewtopic.php?t=8186",
  "labels": ["тилапия", "карп", "musht"],
  "data": "Text containing the fish species names..."
}
```

### LabelingResult Structure
```java
public class LabelingResult {
    private List<LabeledSentence> sentences;
    private LabelingMetadata metadata;
}

public class LabeledSentence {
    private String sentence;           // The extracted sentence
    private String language;          // Language code (ru/en/he)
    private List<String> labels;     // Prioritized labels found
    private String source;           // Source topic URL
}

public class LabelingMetadata {
    private String language;
    private String extractionDate;
    private int totalSentences;
    private int totalLabelsLoaded;
    private int totalTopicsProcessed;
    private String sourceDescription;
}
```

## Dictionary Format
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
        "venomous": "The organism INJECTS toxin via spines/stings. Dangerous to touch",
        "toxic": "The organism IS poisonous if INGESTED (eaten)",
        "toxic_roe": "Specific parts (eggs/caviar) are poisonous during spawning"
      }
    },
    "global_definitions": {
      "specificity_definitions": {
        "MOSTLY_USED": "Name most commonly used by users.",
        "CANONICAL": "Primary name per language; exactly one per UID",
        "VARIANT": "Refinement of canonical; same UID, preserves user intent"
      }
    }
  },
  "data": [
    {
      "uid": "species_1769442340783",
      "type": "SPECIES",
      "en": [
        { "value": "musht", "specificity": "VARIANT" },
        { "value": "tilapia", "specificity": "CANONICAL" },
        { "value": "st. peter's fish", "specificity": "VARIANT" }
      ],
      "ru": [
        { "value": "тилапия", "specificity": "CANONICAL" },
        { "value": "мушт", "specificity": "MOSTLY_USED" }
      ],
      "he": [
        { "value": "אמנון הירדן", "specificity": "VARIANT" }
      ]
    }
  ]
}
```

## Deduplication
Not implemented - handled externally if needed.

## HTML Cleaning (In Downloader)
The HtmlCleaner is part of the Downloader module, not Labeler. It cleans raw HTML before passing to labeler.

### HtmlCleaner Features
- **Content Extraction**: Extracts post content using phpBB selectors (`div.postbody > div.content`)
- **BBCode Removal**: Removes `[img]`, `[url]`, `[table]`, `[td]` tags
- **Navigation Filtering**: Removes navbar, header, footer, pagination
- **Image Galleries**: Removes PhotoAlbums links, thumbnails
- **Short Content**: Skips posts < 50 characters
- **Arrow Symbols**: Removes ⇧, ⇩, ↧, etc.
- **Quote/Signature**: Removes blockquote, signature, notice elements

```java
String cleaned = HtmlCleaner.cleanHtml(rawHtml, topicUrl);
// Adds topic URL and download timestamp to output
```

## File Structure
```
src/main/java/dev/fisher/downloads/
├── extractors/
│   ├── IfTopicLabeler.java           # Interface
│   └── TopicsListExtractor.java      # Topic URL extraction
├── labeler/
│   ├── SentencesLabeler.java        # Main implementation
│   ├── LabelingResult.java          # Result structure
│   └── DictionaryLoader.java        # Loads dictionaries
├── metadata/
│   ├── TopicMetadataExtractor.java  # Interface
│   ├── PhpBBTopicMetadataExtractor.java
│   └── VBulletinTopicMetadataExtractor.java
└── model/
    └── Topic.java                   # Topic model

src/main/resources/
├── config/
│   └── forums/
│       └── israfish.json           # Configuration
└── dictionaries/
    └── species_dict.json           # Target terms dictionary
```

## Status
- ✅ Interface defined (IfTopicLabeler)
- ✅ Topic model ready
- ✅ Configuration in JSON
- ✅ Language-specific patterns (hardcoded)
- ✅ HtmlCleaner updated
- Implementation: TODO
