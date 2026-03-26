package com.example.ragdemo.controller;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.ragdemo.exception.LlmAuthException;
import com.example.ragdemo.exception.LlmException;
import com.example.ragdemo.exception.LlmNetworkException;
import com.example.ragdemo.exception.LlmRateLimitException;
import com.example.ragdemo.exception.LlmTimeoutException;
import com.example.ragdemo.properties.RagProperties;
import com.example.ragdemo.service.KnowledgeBaseService;
import com.example.ragdemo.service.RagService;

/**
 * RAG controller, only responsible for receiving and validating parameters
 */
@RestController
@RequestMapping("/rag")
public class RagController {

    private static final Logger log = LoggerFactory.getLogger(RagController.class);

    private final RagService ragService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final RagProperties properties;

    public RagController(RagService ragService,
                         KnowledgeBaseService knowledgeBaseService,
                         RagProperties properties) {
        this.ragService = ragService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.properties = properties;
    }

    @GetMapping("/ask")
    public Map<String, Object> ask(@RequestParam("q") String question) throws LlmException {
        validateQuestion(question);
        return ragService.ask(question);
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("kbPattern", properties.getKbPattern());
        result.put("storeFile", properties.getStoreFile());
        result.put("topK", properties.getTopK());
        result.put("similarityThreshold", properties.getSimilarityThreshold());
        result.put("maxQuestionLength", properties.getMaxQuestionLength());
        result.put("storeFileExists", new File(properties.getStoreFile()).exists());
        return result;
    }

    @PostMapping("/rebuild")
    public Map<String, Object> rebuild() throws IOException {
        return knowledgeBaseService.rebuild();
    }

    private void validateQuestion(String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Question cannot be empty");
        }
        if (question.length() > properties.getMaxQuestionLength()) {
            throw new IllegalArgumentException(
                    "Question exceeds maximum length: " + properties.getMaxQuestionLength());
        }
    }

    // ==================== Exception Handling ====================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Invalid request: {}", e.getMessage());
        return ResponseEntity.badRequest().body(errorResponse("invalid_request", e.getMessage()));
    }

    @ExceptionHandler(LlmAuthException.class)
    public ResponseEntity<Map<String, Object>> handleLlmAuthException(LlmAuthException e) {
        log.error("LLM auth error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse(e.getErrorCodeString(), e.getMessage()));
    }

    @ExceptionHandler(LlmRateLimitException.class)
    public ResponseEntity<Map<String, Object>> handleLlmRateLimitException(LlmRateLimitException e) {
        log.warn("LLM rate limited: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(errorResponse(e.getErrorCodeString(), e.getMessage()));
    }

    @ExceptionHandler(LlmTimeoutException.class)
    public ResponseEntity<Map<String, Object>> handleLlmTimeoutException(LlmTimeoutException e) {
        log.warn("LLM timeout: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(errorResponse(e.getErrorCodeString(), e.getMessage()));
    }

    @ExceptionHandler(LlmNetworkException.class)
    public ResponseEntity<Map<String, Object>> handleLlmNetworkException(LlmNetworkException e) {
        log.error("LLM network error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorResponse(e.getErrorCodeString(), e.getMessage()));
    }

    @ExceptionHandler(LlmException.class)
    public ResponseEntity<Map<String, Object>> handleLlmException(LlmException e) {
        log.error("LLM error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse(e.getErrorCodeString(), e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse("internal_error", "An unexpected error occurred"));
    }

    private Map<String, Object> errorResponse(String code, String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", true);
        error.put("code", code);
        error.put("message", message);
        return error;
    }
}