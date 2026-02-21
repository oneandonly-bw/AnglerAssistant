# Last Todo - 2026-02-21

## Current Tasks

1. **Add isValid field to LabelEntry** - pending
2. **Create labels with isValid=false for rejected candidates** (blocked, skipList, !lemma.contains, LLM FALSE) - pending
3. **Remove processedWords check** - allow all occurrences to be labeled - pending
4. **Update LabeledSentence** - add validLabels and invalidLabels lists (keep isValid in LabelEntry) - pending
5. **Rename LabelPosition to LabelEntry** - pending
6. **Update docs in new folder** - completed

## Notes

- Label ALL occurrences in sentence (no deduplication)
- Two label lists: validLabels (isValid=true) and invalidLabels (isValid=false)
- Rejected candidates get labels with isValid=false
- Rename LabelPosition → LabelEntry
- Use Flask+pymorphy3 for Russian lemmatization
