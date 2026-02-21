# Labeling Algorithm Design

## Overview
Efficient sentence labeling by iterating dictionary entries instead of dictionary keys.

## Data Structure

### Dictionary Entry
```java
record DictionaryEntry(
    String uid,           // Database reference (not used for matching)
    String type,          // e.g., SPECIES, METHOD, SPOT
    List<DictValue> values
)

record DictValue(
    String value,         // e.g., "карп", "мушт"
    String specificity   // CANONICAL, VARIANT, MOSTLY_USED
)
```

### Dictionary Loading
- Load all entries into `List<DictionaryEntry>` (e.g., 42 species entries)
- Each entry contains canonical + variants (typically 2-3 values)
- Total ~126 values to check per sentence max

## Algorithm

### Input: sentence text

### Process:
```
processedWords = Set()  // Track words already labeled

For each entry in dictionary:
    matchedEntry = null
    
    For each value in entry.values:
        // Check if value associates with any word in text
        associated = checkAssociation(value, text)  // match, lemma, or LLM
        
        if associated:
            // Determine canonical and variant
            if value.specificity == VARIANT || value.specificity == MOSTLY_USED:
                canonical = entry.getCanonical()  // Find CANONICAL value in entry
                variant = value.value
            else:
                canonical = value.value
                variant = null
            
            // Create label
            label = new LabelPosition(
                surface = associatedWord,  // Exact word from text
                canonical = canonical,
                variant = variant,
                start = position,
                end = position + length
            )
            
            processedWords.add(associatedWord)
            matchedEntry = entry
            // DO NOT BREAK - continue checking other values in THIS entry
            // Same entry can match multiple different surface forms!
    
    // After checking all values in entry, continue to next entry

return all labels found
```

### Association Check (for each value):
1. **Exact match**: value == word in text
2. **Lemma match**: stem(value) == stem(word) 
3. **LLM match**: LLM confirms value is form of word

## Efficiency

### Before (Old Code):
```
For each key in dictionary (42 canonical keys):
    If key in text:
        Check occurrence...
```
- O(n) iterations per sentence
- Only checks canonical keys, misses variants

### After (New Code):
```
For each entry in dictionary (42 entries):
    For each value in entry (2-3 values):
        Check association...
```
- Max 42 × 3 = 126 string comparisons
- Checks ALL values (canonical + variants)
- Short-circuits on match

## Label Output Format

```json
{
  "type": "data",
  "forumUrl": "https://forum.israfish.co.il/viewforum.php?f=7",
  "topicUrl": "https://forum.israfish.co.il/viewtopic.php?t=356",
  "lang": "ru",
  "text": "Столь нахально утащил его карпик грамм на 800",
  "labels": [
    {
      "surface": "карпик",
      "canonical": "карп",
      "variant": null,
      "start": 26,
      "end": 32
    }
  ]
}
```

### Field Rules:
- **surface**: Exact text from sentence
- **canonical**: Required - CANONICAL dictionary entry value
- **variant**: Only if matched value is VARIANT or MOSTLY_USED
- **start/end**: Character offsets in sentence (end exclusive)
