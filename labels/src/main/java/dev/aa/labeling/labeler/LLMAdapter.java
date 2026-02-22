package dev.aa.labeling.labeler;

public interface LLMAdapter {
    boolean isFormOf(String key, String candidate, String language, String entryType);
    boolean isRelevantType(String term, String sentence, String entryType, int start, int end);
}
