package com.example.ragdemo.exception;

/**
 * LLM timeout exception
 */
public class LlmTimeoutException extends LlmException {

    public LlmTimeoutException(String message) {
        super(ErrorCode.TIMEOUT, message);
    }

    public LlmTimeoutException(String message, Throwable cause) {
        super(ErrorCode.TIMEOUT, message, cause);
    }
}