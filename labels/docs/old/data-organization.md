# Data Organization and Confidence Levels

## Directory Structure

```
fine-tuning/
├── data/                       # Runtime data (per site)
│   └── <site_id>/              # e.g., israfish/
│       ├── terms_seen.txt      # Cached dictionary terms
│       ├── lemmas_seen.txt     # Cached lemmas
│       └── blocked_terms.txt   # Blocked terms (human names)
├── output/                     # Generated files
│   └── labeled/               # Labeled output (JSON Lines)
│       └── <site_id>/         # e.g., israfish/
│           ├── species_labeled.json
│           └── ...
├── src/
│   └── main/
│       ├── java/               # Source code (dev.fisher.downloads)
│       └── resources/
│           ├── config/         # Forum configurations
│           ├── dictionaries/    # Dictionary JSON files
│           └── llm/            # LLM provider configs
├── config/                    # (reserved for future use)
└── pom.xml
```

## Data Flow

1. **Configuration**: `config/forums/<site>.json` defines forums, language, dictionaries
2. **Runtime Data**: `data/<site_id>/` - shared cache per site (terms, lemmas, blocked terms)
3. **Output**: `output/labeled/<site_id>/` - labeled sentences (JSON Lines)

## Language-Specific Dictionary Loading

- Dictionaries loaded per-forum based on `language` field in forum config
- Only specified language entries loaded (e.g., "ru" loads only Russian entries)

## Confidence Levels

Fixed confidence values per data type:

| Data Type | Confidence | Description |
|-----------|------------|-------------|
| USER_FREE_TEXT | 1.0 | Free-text fishing reports (Phase 1) |
| USER_VOICE | 0.9 | Voice reports (future phase) |
| API_DATA | 0.8 | Weather/wind from external APIs |
| FORUM_POSTS | 0.7 | Extracted from forums |
| INFERRED | 0.5 | System-inferred values |
| SYNTHETIC | 0.3 | Generated for training |

## Confidence Usage

1. **LoRA Training Priority**: Higher confidence data used first
2. **Query Results**: Weight responses by confidence scores  
3. **Error Analysis**: Track low-confidence extraction failures
4. **Data Validation**: Flag entries below confidence threshold
5. **User Prompts**: Request clarification for low-confidence data

## User Input Types

**Phase 1**: Free-text fishing reports
- "Caught 2 carp at Kinneret using corn bait"
- "Fishing at Alexander stream, no bites today"
- "Great day at Sea of Galilee, 3 bass on spinner"

**Future Phase**: Voice input support

## Data Sources

1. **User-provided fishing session reports** (confidence: 1.0)
2. **Data collected from online forums and fishing groups** (confidence: 0.7)
3. **Environmental data from external APIs** (confidence: 0.8)

## Storage Strategy

- **Raw HTML**: Downloaded and cleaned in memory
- **Provenance**: Track source attribution and confidence throughout pipeline
- **Scalability**: Easy addition of new forum sources via configuration

## Forum Downloader Architecture

**Clean Modular Architecture (Day 2 Refactoring & Cleanup Complete)**

### Architecture Components

**Base Class:**
- `BaseDownloader` - Common functionality for HTTP/HTTPS, cleaning, saving

**Adapters (Forum Type Specific):**
- `VBulletinAdapter` - vBulletin forum handling with proper selectors and pagination
- `PHPBBAdapter` - Future adapter for phpBB forums
- `XenForoAdapter` - Future adapter for XenForo forums  
- `InvisionAdapter` - Future adapter for Invision forums
- `CustomAdapter` - Future adapter for custom forum types

**Engines (Implementation Layer):**
- `VBulletinForumEngine` - Common engine for vBulletin-based forums
- `UniversalForumEngine` - Fallback engine for unknown/custom forum types

### **✅ REMOVED UNNECESSARY COMPONENTS:**
- ❌ `IsraFishForumEngine` - IsraFish works with standard VBulletingForumEngine
- ❌ `IsraFishForumDownloader` - IsraFish works with standard VBulletingAdapter
- ❌ `IsrafishMain` - Old standalone approach

### **✅ CONFIGURATION-DRIVEN APPROACH:**
All forums use the same architecture:

```json
{
  "siteId": "israfish",
  "baseUrl": "https://forum.israfish.co.il/",
  "forumType": "vbulletin",
  "forums": [
    {
      "url": "https://forum.israfish.co.il/viewforum.php?f=128",
      "forumName": "Пресноводная Рыбалка в Израиле",
      "path": "fresh_water"
    },
    {
      "url": "https://forum.israfish.co.il/viewforum.php?f=129", 
      "forumName": "Морская рыбалка в Израиле",
      "path": "salt_water"
    }
  ]
}
```

**Works with:**
- ✅ `VBulletinAdapter` - Handles vBulletin structure and pagination
- ✅ `VBulletinForumEngine` - Generic vBulletin functionality
- ✅ Configuration system - Forum-specific URLs and settings in JSON

### Forum Downloader Configuration

Each forum source uses a JSON configuration with automatic type-based engine selection:

```json
{
  "siteId": "israfish",
  "baseUrl": "https://forum.israfish.co.il/",
  "forumType": "vbulletin",
  "forums": [
    {
      "url": "https://forum.israfish.co.il/viewforum.php?f=128",
      "forumName": "Пресноводная Рыбалка в Израиле",
      "path": "fresh_water"
    },
    {
      "url": "https://forum.israfish.co.il/viewforum.php?f=129", 
      "forumName": "Морская рыбалка в Израиле",
      "path": "salt_water"
    }
  ]
}
```

**Supported Forum Types:**
- `vbulletin` - vBulletin forums (maps to VBulletingForumEngine)
- `israfish` - IsraFish-specific implementation (maps to IsraFishForumEngine)
- `phpbb` - phpBB forums (maps to future PhpbbForumEngine)
- `xenforo` - XenForo forums (maps to future XenforoForumEngine)  
- `invision` - Invision Power Board forums (maps to future InvisionForumEngine)
- `custom` - Custom implementations (uses UniversalForumEngine)
- `unknown` - Unrecognized types (falls back to UniversalForumEngine)

### Engine Registry & Factory

```java
// Create engine from JSON configuration
CommonForumEngine engine = ForumEngineFactory.createEngineFromFile("israfish.json");

// Process forums
for (ForumConfig.Forum forum : engine.getConfig().getForums()) {
    engine.processForum(forum);
    
    // Stream topics
    TopicDownloader.Topic topic;
    while ((topic = engine.getNextTopic()) != null) {
        // Process topic content
    }
}
```

### Key Technical Improvements

**Fixed Issues:**
- ✅ **Empty list issue resolved**: Corrected vBulletin selectors from "showthread.php?t=" to "viewtopic.php?t="
- ✅ **Proper forum ID mapping**: IsraFish fresh_water=128, salt_water=129
- ✅ **Modular design**: Base class + adapters + forum-specific engines
- ✅ **Centralized functionality**: HTML cleaning in BaseDownloader

**Architecture Benefits:**
- ✅ **Modular separation**: Clear separation of concerns between base functionality, adapters, and engines
- ✅ **Easy extension**: Add new forum types via adapters without modifying core code
- ✅ **Reusable components**: Common functionality centralized in BaseDownloader
- ✅ **Forum-specific customization**: Only when needed (IsraFish works with standard VBulletingAdapter)
- ✅ **Production ready**: All tests passing (73 tests, 0 failures, 0 errors)

### Test Coverage

**Comprehensive Test Suite:**
- ✅ 73 tests total
- ✅ 0 failures, 0 errors  
- ✅ 2 skipped (integration tests with network dependency)
- ✅ All compilation issues resolved
- ✅ Unit tests verify basic functionality
- ✅ Integration tests disabled by default

## Directory Separation

```
data/          # Operational data (runtime, dynamic)
├── raw/       # Downloaded/scraped content
├── processed/ # Extracted sentences and intermediate data  
└── labeled/   # Training data with labels

output/        # Generated files (external consumption)
└── training/  # Final LoRA training files ready for GPU

src/main/resources/  # Static application assets only
├── config/          # Configuration files
└── dictionaries/    # Static dictionaries
```

- **data/**: Runtime operational data that changes during execution
- **output/**: Generated files meant for external systems (LoRA training)
- **resources/**: Static application assets that ship with the program