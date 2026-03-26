package com.example.ragdemo.exception;

/**
 * Base exception for LLM calls
 */
public class LlmException extends Exception {

    public enum ErrorCode {
        INVALID_INPUT("invalid_input"),
        INPUT_TOO_LONG("input_too_long"),
        EMPTY_RESPONSE("empty_response"),
        AUTH_FAILED("auth_failed"),
        RATE_LIMITED("rate_limited"),
        TIMEOUT("timeout"),
        NETWORK_ERROR("network_error"),
        UNKNOWN("unknown");

        private final String code;

        ErrorCode(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    private final ErrorCode errorCode;

    public LlmException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public LlmException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getErrorCodeString() {
        return errorCode.getCode();
    }
}