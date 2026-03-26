package com.example.ragdemo.exception;

/**
 * LLM network exception
 */
public class LlmNetworkException extends LlmException {

    public LlmNetworkException(String message) {
        super(ErrorCode.NETWORK_ERROR, message);
    }

    public LlmNetworkException(String message, Throwable cause) {
        super(ErrorCode.NETWORK_ERROR, message, cause);
    }
}