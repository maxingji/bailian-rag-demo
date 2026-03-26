package com.example.ragdemo;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);

    private final EmbeddingModel embeddingModel;
    private final RagProperties properties;
    private volatile SimpleVectorStore vectorStore;

    public KnowledgeBaseService(EmbeddingModel embeddingModel, RagProperties properties) {
        this.embeddingModel = embeddingModel;
        this.properties = properties;
    }

    @PostConstruct
    public void init() throws IOException {
        File storeFile = new File(properties.getStoreFile());
        if (storeFile.exists() && storeFile.length() > 0) {
            SimpleVectorStore loadedStore = SimpleVectorStore.builder(embeddingModel).build();
            loadedStore.load(storeFile);
            this.vectorStore = loadedStore;
            log.info("Loaded local vector index from {}", storeFile.getAbsolutePath());
            return;
        }
        rebuild();
    }

    public synchronized Map<String, Object> rebuild() throws IOException {
        MarkdownDocumentReader reader = new MarkdownDocumentReader(properties.getKbPattern());
        List<Document> rawDocuments = reader.get();

        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(500)
                .withMinChunkSizeChars(200)
                .withMinChunkLengthToEmbed(20)
                .withMaxNumChunks(2000)
                .withKeepSeparator(true)
                .build();

        List<Document> chunks = splitter.apply(rawDocuments);
        SimpleVectorStore newStore = SimpleVectorStore.builder(embeddingModel).build();
        newStore.add(chunks);

        File storeFile = new File(properties.getStoreFile());
        File parent = storeFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create directory: " + parent.getAbsolutePath());
        }
        newStore.save(storeFile);
        this.vectorStore = newStore;

        log.info("Knowledge base initialized/rebuilt, {} raw documents, {} chunks after splitting, index saved to {}",
                rawDocuments.size(), chunks.size(), storeFile.getAbsolutePath());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Knowledge base rebuilt");
        result.put("rawDocuments", rawDocuments.size());
        result.put("chunks", chunks.size());
        result.put("storeFile", storeFile.getAbsolutePath());
        return result;
    }

    public List<Document> search(String query) {
        return currentStore().similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(properties.getTopK())
                .similarityThreshold(properties.getSimilarityThreshold())
                .build());
    }

    public SimpleVectorStore currentStore() {
        if (this.vectorStore == null) {
            throw new IllegalStateException("Vector store not yet initialized");
        }
        return this.vectorStore;
    }
}
