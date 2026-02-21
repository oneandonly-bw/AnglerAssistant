# Sentence Processing & Candidate Validation Flow with Label Structure

This document contains **everything in one place** for sentence processing, candidate validation, and label creation, including handling `isValid` flags and scanning the entire sentence for each dictionary key.

---

## Label Format

```java
record LabeledSentence(
    String forumUrl,       // Source forum
    String topicUrl,       // Source topic
    String lang,           // Language code
    String text,           // Cleaned/normalized sentence
    List<LabelEntry> validLabels,    // All accepted labels (isValid = true)
    List<LabelEntry> invalidLabels   // All rejected labels (isValid = false)
)

record LabelEntry(
    String surface,        // Exact word from text (lowercase)
    String canonical,      // Dictionary canonical name
    String variant,        // Variant key if applicable
    int start,             // Start index in sentence
    int end,               // End index in sentence
    boolean isValid        // TRUE if accepted, FALSE if rejected
)
```

---

## Sentence Processing Flow

### 1. Get the sentence.

### 2. Normalize the sentence:

- Remove double spaces.
- Preserve original case and punctuation.

### 3. For each dictionary key (`key`):

- Perform case-insensitive search over the entire sentence.
- If no match → continue to next key.

### 4. If match is found:

- Extract candidate using word boundaries (preserving original case/punctuation).
- **Label ALL occurrences** of the same key in the sentence (no deduplication).

### 5. Check candidate against `blockedList` (case-sensitive):

- If `candidate.equals(blockedWord)` → create label with `isValid = false`, add to `invalidLabels`, continue scanning.

### 6. Convert candidate to lowercase:

- `candidateLower = candidate.toLowerCase()`

### 7. Check `candidateLower` against `skipList`:

- If found → create label with `isValid = false`, add to `invalidLabels`, continue scanning.

### 8. Check `candidateLower` against `seenTerms`:

- If found → create label with `isValid = true`, add to `validLabels`, continue scanning.

### 9. Fast exact-match optimization:

- If `candidateLower.equals(key)`:
  - Create label with `isValid = true`.
  - Add `candidateLower` to `seenTerms`.
  - Continue scanning the sentence for this key.

### 10. Lemmatization:

- `lemma = getLemma(candidateLower)`

### 11. If `!lemma.contains(key)`:

- Create label with `isValid = false`, add to `invalidLabels`, continue scanning.

### 12. If `lemma.length == key.length`:

- Create label with `isValid = true`.
- Add `candidateLower` to `seenTerms`.
- Add `lemma` to `seenLemmas`.
- Continue scanning the sentence for this key.

### 13. LLM fallback:

- Send:
  - `key`
  - `candidateLower` (punctuation preserved)
- Receive TRUE or FALSE.

### 14. If LLM returns TRUE:

- Create label with `isValid = true`.
- Add `candidateLower` to `seenTerms`.
- Add `lemma` to `seenLemmas`.
- Continue scanning sentence for this key.

### 15. If LLM returns FALSE:

- Create label with `isValid = false`.
- Add `candidateLower` to `skipList`.
- Add to `invalidLabels`.
- Continue scanning sentence for this key.

### 16. After scanning the entire sentence for the current key → move to the next key.

---

## Dictionary Entry Structure

- **canonical** — Official species name.
- **variant** — Alternative names mapping to the same canonical.
- **mostlyUsed** — Most common term in text (for analytics/normalization, not stored in label).

---

## Label Resolution Rules

1. Resolve dictionary entry for each matched key.
2. Set:
   - `canonical` → dictionary canonical.
   - `variant` → matched key only if stored as a variant; null if key is canonical.
3. Multiple labels allowed per sentence.
4. Global caches (`seenTerms`, `seenLemmas`, `skipList`) reduce repeated lemma/LLM calls.

---

## Key Updates / Notes

- Scan the **entire sentence** for each dictionary key.
- Label **ALL occurrences** - do not deduplicate within the same sentence.
- Create label with `isValid = true` → add to `validLabels`.
- Create label with `isValid = false` → add to `invalidLabels`.
- Labels reference canonical as the stable class identifier.
- Variants are preserved only to track which alternative name triggered the match.
