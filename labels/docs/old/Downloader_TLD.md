# Downloader Top-Level Design (TLD)

## Overview
The Downloader system extracts forum topics, cleans them, and streams each topic to an external extractor. It uses a TopicsListExtractor class with switch pattern for different forum types, and TopicMetadataExtractor for forum-specific HTML parsing.

## Architecture

### TopicsListExtractor Pattern
- Single class handles all forum types
- Public method `getTopicsList(ForumType, forumUrl)` with switch
- Protected overridable methods for each forum type:
  - `getPhpBBForumTopicsList(forumUrl, sectionName)`
  - `getVBulletinForumTopicsList(forumUrl)`

### ForumType Enum
- Values: PHPBB, VBULLETIN
- Factory method: `ForumType.fromValue(String)` for parsing

### TopicMetadataExtractor Pattern
- Interface with implementations per forum type
- Methods: `extractMetadata(Topic, htmlContent)`, `extractTitle(htmlContent)`
- PhpBBTopicMetadataExtractor: extracts author from `<strong>`, date from `username &raquo; DD-MM-YYYY HH:MM`
- VBulletinTopicMetadataExtractor: extracts author from `<a class="username">`, date from `datetime="..."`

### Configuration Flow
```
Configuration → TopicsListExtractor → BaseDownloader → TopicMetadataExtractor → TopicExtractor
```

## Configuration Loading

### ConfigurationFacade
Entry point for loading configuration. Supports both file system and classpath resources.

```java
Configuration config = ConfigurationFacade.getConfiguration("config/forums/israfish.json");
Configuration config = ConfigurationFacade.getConfiguration("/absolute/path/to/config.json");
```

- If path is absolute → loads from file system
- Otherwise → loads from classpath resources

## Configuration JSON Structure

```json
{
  "meta": {
    "forumTypes": ["PHPBB", "VBULLETIN"],
    "filterTypes": ["SECTION", "TOPIC"],
    "languages": ["RU", "EN", "HE"]
  },
  "general": {
    "loggingLevel": "INFO"
  },
  "site": {
    "siteId": "string, unique site identifier",
    "name": "string, human-readable site name",
    "baseUrl": "string, base URL for making absolute URLs",
    "httpTimeout": "integer, HTTP request timeout in ms",
    "httpUserAgent": "string, User-Agent string"
  },
  "runtime": {
    "memoryThreshold": "number, memory threshold (0.1-1.0)",
    "maxRetries": "integer, max retry attempts"
  },
  "extractor": {
    "enabled": "boolean"
  },
  "forums": [
    {
      "url": "string, forum URL",
      "forumName": "string, forum name",
      "path": "string, storage path category",
      "enabled": "boolean",
      "forumType": "PHPBB or VBULLETIN",
      "language": "RU, EN, or HE",
      "include": [{ "name": "...", "type": "SECTION|TOPIC" }],
      "exclude": [{ "name": "...", "type": "SECTION|TOPIC" }]
    }
  ]
}
```

## Topic Object

```java
public class Topic {
    // Identification (Immutable)
    private final String sourceName;
    private final String siteName;
    private final String forumName;
    private final String topicUrl;
    private final String topicId;
    
    // Content (Mutable)
    private String content;
    private String title;
    
    // Structured Metadata (Mutable)
    private String author;
    private LocalDateTime creationDate;
    private String subcategory;
    private String language;
    
    // Processing Metadata
    private LocalDateTime downloadTimestamp;
    private ProcessingStatus processingStatus;
    private Set<ErrorFlag> errorFlags;
}
```

## Key Features
- Jsoup for HTTP/HTML parsing (replaces HttpUtil)
- 1500ms delay between requests to avoid rate limiting
- Skip on HTTP 500/404 errors without retries
- Memory threshold checks

## Status
- ✅ TopicsListExtractor with ForumType enum
- ✅ TopicMetadataExtractor interface with PhpBB/VBulletin implementations
- ✅ Simplified JSON configuration
- ✅ Filter include/exclude
- ✅ JSON schema + custom validation
- ✅ Configuration records
- ✅ Jsoup-based HTTP (no HttpUtil)
- ✅ 81 tests passing
