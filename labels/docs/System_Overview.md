# System Overview

## 1. Project

**Name**: AnglerAssistant Labels  
**Package**: `dev.aa.labeling`  
**Language**: Java 17  
**Build**: Maven

## 2. Purpose

Forum data extraction and labeling system for collecting fishing-related content. Downloads forum topics, extracts clean text, labels sentences with domain terminology, outputs structured JSON for ML training.

## 3. What It Does

```
Forum (HTML) → Download → Clean → Label → Output (JSON)
```

1. **Download** - Fetch forum topics from vBulletin/phpBB forums
2. **Clean** - Remove HTML, extract plain text
3. **Label** - Find sentences with dictionary terms (species, methods, baits)
4. **Output** - Write labeled JSON Lines

## 4. Usage

```bash
# Default config
java -jar app.jar

# Custom config (relative → src/main/resources/config/)
java -jar app.jar -config israfish.json

# Absolute path
java -jar app.jar -config C:/config/my.json

# Resume from checkpoint
java -jar app.jar -config israfish.json -resume
```

## 5. Package Structure

```
dev.aa.labeling/
├── config/          # Configuration (12 classes)
├── engine/          # BaseDownloader
├── extractors/      # HTML parsing (TopicsListExtractor, TopicMetadataExtractor)
├── factory/         # DownloaderFactory
├── interfaces/     # IfDownloader, IfTopicLabeler
├── labeler/        # SentencesLabeler, CacheManager, CountersManager, Lemmatizers
├── llm/            # LLM providers (Groq, OpenRouter, HuggingFace)
├── mains/          # Entry points (LabelerMain, PromptTester)
├── model/          # Topic
└── util/           # Utilities
```

## 6. Configuration

Configuration files located in `src/main/resources/config/`:
- `israfish.json` - IsraFish forum configuration
- `israfish_test.json` - Test configuration
- `config_template.json` - Template
- `config_schema.json` - JSON schema

Loaded via `ConfigurationFacade`:
- Absolute path → filesystem
- Relative path → `src/main/resources/config/`

**Output**: Auto-generated from siteId + dictionary name → `{siteId}_{dictName}.json`

Config JSON structure:
```json
{
  "meta": { ... },
  "general": { ... },
  "site": { "siteId", "baseUrl", ... },
  "runtime": { "memoryThreshold", "maxRetries" },
  "labeler": { "dictionaryPaths", "outputDirectory", ... },
  "forums": [ { "url", "forumName", "forumType", "language" } ]
}
```

## 7. Build & Test

```bash
# Compile
mvn compile

# Test
mvn test

# Package
mvn package
```

**Tests**: 148 passing

## 8. Long Sentence Handling

Sentences exceeding `maxSentenceLengthForContext` (default: 200 chars) undergo context extraction:
- For each valid label found, extract 5 words before + label + 5 words after
- Create separate LabeledSentence for each label
- Mark with "(context)" suffix in topicUrl

This ensures ML training data has focused, relevant text windows around labels.

## 8. Entry Points

| Class | Purpose |
|-------|---------|
| `LabelerMain` | Main CLI application |
| `PromptTester` | Test LLM prompts |
