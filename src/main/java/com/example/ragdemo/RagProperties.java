package com.example.ragdemo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rag")
public class RagProperties {

    private String kbPattern = "classpath:/kb/*.md";
    private String storeFile = "./data/vector-store.json";
    private int topK = 4;
    private double similarityThreshold = 0.6d;

    public String getKbPattern() {
        return kbPattern;
    }

    public void setKbPattern(String kbPattern) {
        this.kbPattern = kbPattern;
    }

    public String getStoreFile() {
        return storeFile;
    }

    public void setStoreFile(String storeFile) {
        this.storeFile = storeFile;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }
}
