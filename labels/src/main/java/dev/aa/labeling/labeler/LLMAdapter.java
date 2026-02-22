package dev.aa.labeling.labeler;

public interface LLMAdapter {
    boolean isFormOf(String key, String candidate, String language);
    boolean isFish(String term, String sentence);
}
