# Last Todo - 2026-02-21

## Current Tasks

0. **Groq key injection** - implement injectKey(providerName) method in LLMProviderManager - completed
1. **Test refactoring** - verify all tests work with new package names - completed
2. **LLM providers refactor** - add enabled providers list in site config, add new properties in providerconfig - pending

3. **Add isValid field to LabelEntry** - pending
4. **Create labels with isValid=false for rejected candidates** (blocked, skipList, !lemma.contains, LLM FALSE) - pending
5. **Remove processedWords check** - allow all occurrences to be labeled - completed
6. **Update LabeledSentence** - add validLabels and invalidLabels lists (keep isValid in LabelEntry) - pending
7. **Rename LabelPosition to LabelEntry** - completed
8. **Update docs in new folder** - completed

## Notes

- Label ALL occurrences in sentence (no deduplication)
- Two label lists: validLabels (isValid=true) and invalidLabels (isValid=false)
- Rejected candidates get labels with isValid=false
- Rename LabelPosition → LabelEntry
- Use Flask+pymorphy3 for Russian lemmatization
