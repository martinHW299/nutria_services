package com.nutria.app.config;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Slf4j
@Configuration
public class GeminiConfiguration {

    @Value("${gemini.vertex.id}")
    private String projectId;

    @Value("${gemini.vertex.location}")
    private String projectLocation;

    @Value("${gemini.vertex.model}")
    private String aiModel;

    @Bean
    public VertexAI vertexAI() throws IOException {
        log.info("Initializing VertexAI with project: {}, location: {}", projectId, projectLocation);

        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("gemini.vertex.id cannot be null or empty");
        }
        if (projectLocation == null || projectLocation.isBlank()) {
            throw new IllegalArgumentException("gemini.vertex.location cannot be null or empty");
        }

        try {
            VertexAI vertexAI = new VertexAI(projectId, projectLocation);
            log.info("VertexAI initialized successfully");
            return vertexAI;
        } catch (Exception e) {
            log.error("Failed to initialize VertexAI", e);
            throw e;
        }
    }

    @Bean
    public GenerativeModel generativeModel(VertexAI vertexAI) {
        log.info("Initializing GenerativeModel with model: {}", aiModel);

        if (aiModel == null || aiModel.isBlank()) {
            throw new IllegalArgumentException("gemini.vertex.model cannot be null or empty");
        }
        if (vertexAI == null) {
            throw new IllegalArgumentException("VertexAI cannot be null");
        }

        try {
            GenerativeModel model = new GenerativeModel(aiModel, vertexAI);
            log.info("GenerativeModel initialized successfully");
            return model;
        } catch (Exception e) {
            log.error("Failed to initialize GenerativeModel", e);
            throw e;
        }
    }
}