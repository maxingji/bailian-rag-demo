package com.example.ragdemo.exception;

/**
 * LLM authentication failure exception
 */
public class LlmAuthException extends LlmException {

    public LlmAuthException(String message) {
        super(ErrorCode.AUTH_FAILED, message);
    }

    public LlmAuthException(String message, Throwable cause) {
        super(ErrorCode.AUTH_FAILED, message, cause);
    }
}