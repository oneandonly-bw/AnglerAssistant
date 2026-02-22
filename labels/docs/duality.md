# Duality Feature

## Purpose

Handles ambiguous terms that can mean both a fish species AND something else (person name, place, etc.).

## Dictionary Format

```json
{
  "uid": "species_xxx",
  "type": "SPECIES",
  "en": [...],
  "ru": [
    {
      "value": "карп",
      "specificity": "CANONICAL",
      "duality": {
        "rule": "CASE_SENSITIVE",
        "alternate_meaning": "PERSON_NAME"
      }
    },
    {
      "value": "сазан",
      "specificity": "VARIANT"
    }
  ],
  "he": [...]
}
```

## Duality Rules

| Rule | Description | Implementation |
|------|-------------|----------------|
| `CASE_SENSITIVE` | Use letter case to disambiguate | Check if case differs from dictionary key |

## CASE_SENSITIVE Logic

When a term is found in text and has `duality.rule = CASE_SENSITIVE`:

```
Found: "Карп" (capitalized)
Key:   "карп" (lowercase)

If found.length == key.length AND !found.equals(key):
    → Ask LLM: "Is this a fish species?"
    → YES: Create valid label (isValid=true)
    → NO:  Create invalid label (isValid=false), skip (don't add to seenTerms)
Else:
    → Normal validation (cache → lemma → LLM)
```

### Examples

| Found | Key | Length Match | Equals | Action |
|-------|-----|--------------|--------|--------|
| `"Карп"` | `"карп"` | 4=4 ✓ | NO ✗ | Ask LLM |
| `"карп"` | `"карп"` | 4=4 ✓ | YES ✓ | Normal validation |
| `"карпы"` | `"карп"` | 5≠4 ✗ | - | Normal validation |

## Implementation

### 1. Classes

```java
record Duality(
    String rule,           // CASE_SENSITIVE
    String alternate_meaning  // PERSON_NAME, SURNAME, etc.
)

record DictValue(
    String value,
    String specificity,
    Duality duality  // null if no duality
)
```

### 2. Flow

```
findCandidates(sentence):
    For each dictionary entry:
        For each value in entry:
            Find all occurrences in sentence
            
            If value.duality != null:
                Check CASE_SENSITIVE rule
                Ask LLM if needed
                
            Return Candidate with DictValue
```

### 3. LLM Prompt

```
System: You are a fish species classifier. Answer only YES or NO.

User:
Context: "Утром Карп поймал карпа"
Term: "Карп"
Is this a fish species?
```

## Notes

- Duality terms are NOT added to seenTerms cache (even if validated)
- Unsupported duality rules throw exception (terminate run)
- Duality checked BEFORE normal validation chain
