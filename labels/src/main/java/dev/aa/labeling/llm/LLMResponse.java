package dev.aa.labeling.llm;

public class LLMResponse {
    private final String content;
    private final String model;
    private final int inputTokens;
    private final int outputTokens;
    private final long responseTimeMs;

    public LLMResponse(String content, String model, int inputTokens, int outputTokens, long responseTimeMs) {
        this.content = content;
        this.model = model;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.responseTimeMs = responseTimeMs;
    }

    public String getContent() { return content; }
    public String getModel() { return model; }
    public int getInputTokens() { return inputTokens; }
    public int getOutputTokens() { return outputTokens; }
    public int getTotalTokens() { return inputTokens + outputTokens; }
    public long getResponseTimeMs() { return responseTimeMs; }
}
