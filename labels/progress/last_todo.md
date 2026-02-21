# Last Todo - 2026-02-21

## Pending

2. **LLM providers refactor** - add enabled providers list in site config, add new properties in providerconfig

## Completed

- Add isValid field to LabelEntry
- Create labels with isValid=false for rejected candidates
- Remove processedWords check - allow all occurrences
- Update LabeledSentence with validLabels and invalidLabels lists
- Rename LabelPosition → LabelEntry
- Update docs in new folder
- Implement ContextExtractor for long sentences (>200 chars)
- Add long sentence tests
- Push to git
- DeDuplicationMain - deduplicate output files using Levenshtein similarity 0.9

## Notes

- Label ALL occurrences in sentence (no deduplication)
- Two output files: valid and invalid labels separated
- Rejected candidates get labels with isValid=false
- Long sentences (>200 chars) get context extraction: 5 words before + label + 5 words after
- Tests: 148 passing
