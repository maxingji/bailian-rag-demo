package com.example.ragdemo.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.example.ragdemo.exception.LlmAuthException;
import com.example.ragdemo.exception.LlmException;
import com.example.ragdemo.exception.LlmNetworkException;
import com.example.ragdemo.exception.LlmRateLimitException;
import com.example.ragdemo.exception.LlmTimeoutException;
import com.example.ragdemo.properties.RagProperties;

/**
 * LLM client wrapper, handles network failures, auth failures, rate limiting, timeout, empty response, etc.
 */
@Component
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    private final ChatClient.Builder chatClientBuilder;
    private final RagProperties properties;

    public LlmClient(ChatClient.Builder chatClientBuilder, RagProperties properties) {
        this.chatClientBuilder = chatClientBuilder;
        this.properties = properties;
    }

    /**
     * RAG mode: uses QuestionAnswerAdvisor to automatically inject retrieved content
     */
    @Retryable(
        retryFor = {LlmNetworkException.class, LlmTimeoutException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public String generateWithRag(String systemPrompt, String userQuestion,
                                  VectorStore vectorStore, SearchRequest searchRequest) throws LlmException {
        validateInput(systemPrompt, userQuestion);

        try {
            log.debug("Calling LLM with RAG, question: {}", truncateForLog(userQuestion));

            QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                    .searchRequest(searchRequest)
                    .build();

            ChatClient chatClient = chatClientBuilder.build();

            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .advisors(qaAdvisor)
                    .user(userQuestion)
                    .call()
                    .content();

            return validateResponse(response);

        } catch (Exception e) {
            throw handleException(e);
        }
    }

    /**
     * Normal mode: without RAG
     */
    @Retryable(
        retryFor = {LlmNetworkException.class, LlmTimeoutException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public String generate(String systemPrompt, String userQuestion) throws LlmException {
        validateInput(systemPrompt, userQuestion);

        try {
            log.debug("Calling LLM with question: {}", truncateForLog(userQuestion));

            ChatClient chatClient = chatClientBuilder.build();

            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userQuestion)
                    .call()
                    .content();

            return validateResponse(response);

        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private void validateInput(String systemPrompt, String userQuestion) throws LlmException {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            throw new LlmException(LlmException.ErrorCode.INVALID_INPUT, "System prompt cannot be empty");
        }
        if (userQuestion == null || userQuestion.isBlank()) {
            throw new LlmException(LlmException.ErrorCode.INVALID_INPUT, "User question cannot be empty");
        }
        if (userQuestion.length() > properties.getMaxQuestionLength()) {
            throw new LlmException(LlmException.ErrorCode.INPUT_TOO_LONG,
                    "Question exceeds maximum length: " + properties.getMaxQuestionLength());
        }
    }

    private String validateResponse(String response) throws LlmException {
        if (response == null || response.isBlank()) {
            log.warn("LLM returned empty response");
            throw new LlmException(LlmException.ErrorCode.EMPTY_RESPONSE, "LLM returned empty response");
        }
        log.debug("LLM response length: {}", response.length());
        return response;
    }

    private LlmException handleException(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            message = e.getClass().getSimpleName();
        }
        String lowerMessage = message.toLowerCase();

        // Authentication failed
        if (lowerMessage.contains("401") || lowerMessage.contains("unauthorized") ||
            lowerMessage.contains("authentication") || lowerMessage.contains("api key")) {
            log.error("LLM authentication failed: {}", message);
            return new LlmAuthException("LLM authentication failed: " + message, e);
        }

        // Access forbidden
        if (lowerMessage.contains("403") || lowerMessage.contains("forbidden")) {
            log.error("LLM access forbidden: {}", message);
            return new LlmAuthException("LLM access forbidden: " + message, e);
        }

        // Rate limited
        if (lowerMessage.contains("429") || lowerMessage.contains("rate limit") ||
            lowerMessage.contains("too many requests") || lowerMessage.contains("quota")) {
            log.warn("LLM rate limited: {}", message);
            return new LlmRateLimitException("LLM rate limited: " + message, e);
        }

        // Timeout
        if (lowerMessage.contains("timeout") || lowerMessage.contains("timed out")) {
            log.warn("LLM request timed out: {}", message);
            return new LlmTimeoutException("LLM request timed out: " + message, e);
        }

        // Network error
        if (lowerMessage.contains("connection") || lowerMessage.contains("network") ||
            lowerMessage.contains("socket") || lowerMessage.contains("i/o")) {
            log.error("LLM network error: {}", message);
            return new LlmNetworkException("LLM network error: " + message, e);
        }

        // Other errors
        log.error("LLM call failed: {}", message, e);
        return new LlmException(LlmException.ErrorCode.UNKNOWN, "LLM call failed: " + message, e);
    }

    private String truncateForLog(String text) {
        if (text == null) return "null";
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }
}