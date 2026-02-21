# Last Todo - 2026-02-21

## Current Tasks

0. **Groq key injection** - implement injectKey(providerName) method in LLMProviderManager - pending
   - Add injectKey(String providerName) method
   - Load {providerName}_key.json and inject key into config before creating adapter
   - Add test: verify key is injected correctly

1. **Test refactoring** - verify all tests work with new package names - pending

2. **Add isValid field to LabelEntry** - pending
3. **Create labels with isValid=false for rejected candidates** (blocked, skipList, !lemma.contains, LLM FALSE) - pending
4. **Remove processedWords check** - allow all occurrences to be labeled - pending
5. **Update LabeledSentence** - add validLabels and invalidLabels lists (keep isValid in LabelEntry) - pending
6. **Rename LabelPosition to LabelEntry** - pending
7. **Update docs in new folder** - completed

## Notes

- Label ALL occurrences in sentence (no deduplication)
- Two label lists: validLabels (isValid=true) and invalidLabels (isValid=false)
- Rejected candidates get labels with isValid=false
- Rename LabelPosition → LabelEntry
- Use Flask+pymorphy3 for Russian lemmatization
