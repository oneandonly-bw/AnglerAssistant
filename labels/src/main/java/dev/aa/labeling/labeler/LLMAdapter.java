package dev.aa.labeling.labeler;

public interface LLMAdapter {
    boolean isFormOf(String key, String candidate, String language);
    boolean isRelevantType(String term, String sentence, String entryType);
}
