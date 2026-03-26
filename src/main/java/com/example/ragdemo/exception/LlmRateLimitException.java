package com.example.ragdemo.exception;

/**
 * LLM rate limit exception
 */
public class LlmRateLimitException extends LlmException {

    public LlmRateLimitException(String message) {
        super(ErrorCode.RATE_LIMITED, message);
    }

    public LlmRateLimitException(String message, Throwable cause) {
        super(ErrorCode.RATE_LIMITED, message, cause);
    }
}