package dev.aa.labeling.llm;

public class LLMException extends Exception {
    private static final long serialVersionUID = 1L;
    
    private final String provider;
    private final ErrorType errorType;
    private Integer retryAfterSeconds;

    public enum ErrorType {
        RATE_LIMIT,
        TOKEN_LIMIT,
        NETWORK_ERROR,
        AUTH_ERROR,
        INVALID_REQUEST,
        SERVER_ERROR,
        UNKNOWN
    }

    public LLMException(String message, String provider, ErrorType errorType) {
        super(message);
        this.provider = provider;
        this.errorType = errorType;
    }

    public LLMException(String message, String provider, ErrorType errorType, Throwable cause) {
        super(message, cause);
        this.provider = provider;
        this.errorType = errorType;
    }

    public String getProvider() { return provider; }
    public ErrorType getErrorType() { return errorType; }
    public Integer getRetryAfterSeconds() { return retryAfterSeconds; }
    public void setRetryAfterSeconds(Integer seconds) { this.retryAfterSeconds = seconds; }

    public boolean isRetryable() {
        return errorType == ErrorType.RATE_LIMIT || 
               errorType == ErrorType.TOKEN_LIMIT || 
               errorType == ErrorType.NETWORK_ERROR ||
               errorType == ErrorType.SERVER_ERROR;
    }
}
