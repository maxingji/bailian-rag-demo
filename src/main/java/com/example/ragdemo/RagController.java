package com.example.ragdemo;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rag")
public class RagController {

    private final ChatClient chatClient;
    private final KnowledgeBaseService knowledgeBaseService;
    private final RagProperties properties;

    public RagController(ChatClient.Builder chatClientBuilder,
                         KnowledgeBaseService knowledgeBaseService,
                         RagProperties properties) {
        this.chatClient = chatClientBuilder.build();
        this.knowledgeBaseService = knowledgeBaseService;
        this.properties = properties;
    }

    @GetMapping("/ask")
    public Map<String, Object> ask(@RequestParam("q") String question) {
        QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(knowledgeBaseService.currentStore())
                .searchRequest(SearchRequest.builder()
                        .topK(properties.getTopK())
                        .similarityThreshold(properties.getSimilarityThreshold())
                        .build())
                .build();

        String answer = chatClient.prompt()
                .system("You are a knowledge base Q&A assistant. Please answer strictly based on the retrieved content; if there is no answer in the materials, just answer \\”I don't know\\”. Keep answers concise.")
                .advisors(qaAdvisor)
                .user(question)
                .call()
                .content();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("question", question);
        result.put("answer", answer);
        result.put("references", knowledgeBaseService.search(question).stream().map(this::toMap).toList());
        return result;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("kbPattern", properties.getKbPattern());
        result.put("storeFile", properties.getStoreFile());
        result.put("topK", properties.getTopK());
        result.put("similarityThreshold", properties.getSimilarityThreshold());
        result.put("storeFileExists", new File(properties.getStoreFile()).exists());
        return result;
    }

    @PostMapping("/rebuild")
    public Map<String, Object> rebuild() throws IOException {
        return knowledgeBaseService.rebuild();
    }

    private Map<String, Object> toMap(Document doc) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", doc.getId());
        item.put("metadata", doc.getMetadata());
        item.put("content", doc.getText());
        return item;
    }
}
