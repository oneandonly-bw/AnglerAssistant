package dev.aa.labeling.llm;

public interface LLMProvider {
    String getName();
    String getModel();
    boolean isEnabled();
    int getPriority();
    LLMResponse chat(String systemPrompt, String userMessage) throws LLMException;
    boolean isAvailable();
    void close();
}
