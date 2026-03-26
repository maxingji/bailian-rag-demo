package com.example.ragdemo.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import com.example.ragdemo.properties.RagProperties;

@Configuration
@EnableConfigurationProperties(RagProperties.class)
@EnableRetry
public class RagConfig {
}