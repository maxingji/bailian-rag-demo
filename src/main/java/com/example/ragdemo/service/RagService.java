package com.example.ragdemo.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Service;

import com.example.ragdemo.client.LlmClient;
import com.example.ragdemo.exception.LlmException;
import com.example.ragdemo.properties.RagProperties;

/**
 * RAG business service, responsible for retrieve, build prompt, call model, build response
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private static final String DEFAULT_SYSTEM_PROMPT =
            "You are a knowledge base Q&A assistant. Please answer strictly based on the retrieved content; " +
            "if there is no answer in the materials, just answer \"I don't know\". Keep answers concise.";

    private final LlmClient llmClient;
    private final KnowledgeBaseService knowledgeBaseService;
    private final RagProperties properties;

    public RagService(LlmClient llmClient,
                      KnowledgeBaseService knowledgeBaseService,
                      RagProperties properties) {
        this.llmClient = llmClient;
        this.knowledgeBaseService = knowledgeBaseService;
        this.properties = properties;
    }

    /**
     * Execute RAG Q&A
     */
    public Map<String, Object> ask(String question) throws LlmException {
        log.info("Processing question: {}", truncateForLog(question));

        // 1. Retrieve - fetch documents for references
        List<Document> references = retrieve(question);
        log.debug("Retrieved {} documents", references.size());

        // 2. Build prompt
        String systemPrompt = buildSystemPrompt(question, references);

        // 3. Build search request for QuestionAnswerAdvisor
        SearchRequest searchRequest = SearchRequest.builder()
                .topK(properties.getTopK())
                .similarityThreshold(properties.getSimilarityThreshold())
                .build();

        // 4. Call model with RAG
        String answer = llmClient.generateWithRag(
                systemPrompt,
                question,
                knowledgeBaseService.currentStore(),
                searchRequest
        );

        // 5. Build response
        return buildResponse(question, answer, references);
    }

    private List<Document> retrieve(String question) {
        return knowledgeBaseService.search(question);
    }

    private String buildSystemPrompt(String question, List<Document> references) {
        if (references.isEmpty()) {
            return DEFAULT_SYSTEM_PROMPT + "\n\nNote: No relevant documents were found in the knowledge base.";
        }
        return DEFAULT_SYSTEM_PROMPT;
    }

    private Map<String, Object> buildResponse(String question, String answer, List<Document> references) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("question", question);
        result.put("answer", answer);
        result.put("references", references.stream().map(this::toMap).toList());
        return result;
    }

    private Map<String, Object> toMap(Document doc) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", doc.getId());
        item.put("metadata", doc.getMetadata());
        item.put("content", doc.getText());
        return item;
    }

    private String truncateForLog(String text) {
        if (text == null) return "null";
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }
}