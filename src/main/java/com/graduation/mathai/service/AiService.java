package com.graduation.mathai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.mathai.config.AiProperties;
import com.graduation.mathai.model.AiSolution;
import com.graduation.mathai.model.WrongQuestion;
import com.graduation.mathai.util.SubjectUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Main AI service — orchestrates prompt building, chat requests, mock fallback,
 * and JSON response parsing. Delegates low-level concerns to specialized components.
 */
@Service
public class AiService {
    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final PromptBuilder promptBuilder;
    private final AiChatClient chatClient;
    private final MockDataProvider mockDataProvider;

    public AiService(AiProperties properties,
                     ObjectMapper objectMapper,
                     PromptBuilder promptBuilder,
                     AiChatClient chatClient,
                     MockDataProvider mockDataProvider) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.promptBuilder = promptBuilder;
        this.chatClient = chatClient;
        this.mockDataProvider = mockDataProvider;
    }

    public AiSolution solve(String question) {
        return solve(question, SubjectUtils.MATH);
    }

    public AiSolution solve(String question, String subject) {
        String normalizedSubject = SubjectUtils.normalize(subject);
        requireApiKeyOrMock();

        if (properties.isMockEnabled() && !StringUtils.hasText(properties.getApiKey())) {
            return mockDataProvider.mockSolution(question, normalizedSubject);
        }

        try {
            String content = chatClient.chat(promptBuilder.buildSolvePrompt(question, normalizedSubject), true);
            AiSolution solution = parseSolution(content, question);
            solution.setSubject(normalizedSubject);
            if (!StringUtils.hasText(solution.getQuestion())) {
                solution.setQuestion(question);
            }
            return solution;
        } catch (Exception ex) {
            throw aiUnavailable(ex);
        }
    }

    public String buildAdvice(List<WrongQuestion> wrongQuestions, Map<String, Long> stats) {
        if (wrongQuestions.isEmpty()) {
            return "当前错题本为空。建议先添加错题，系统会根据知识点分布生成复习建议。";
        }

        requireApiKeyOrMock();
        if (properties.isMockEnabled() && !StringUtils.hasText(properties.getApiKey())) {
            return mockDataProvider.mockAdvice(stats);
        }

        String subject = wrongQuestions.stream()
                .map(WrongQuestion::getSubject)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(SubjectUtils.MATH);

        try {
            return chatClient.chat(promptBuilder.buildAdvicePrompt(subject, stats), false);
        } catch (Exception ex) {
            throw aiUnavailable(ex);
        }
    }

    public List<AiSolution> generatePractice(List<WrongQuestion> wrongQuestions, Map<String, Long> stats) {
        String subject = wrongQuestions.stream()
                .map(WrongQuestion::getSubject)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(SubjectUtils.MATH);
        String mainPoint = stats.keySet().stream().findFirst()
                .orElse(SubjectUtils.MATH.equals(subject) ? "一元二次方程" : "基础语法");

        requireApiKeyOrMock();
        if (properties.isMockEnabled() && !StringUtils.hasText(properties.getApiKey())) {
            return mockDataProvider.mockPractice(mainPoint, subject);
        }

        try {
            String content = chatClient.chat(promptBuilder.buildPracticePrompt(subject, stats), true);
            JsonNode root = objectMapper.readTree(extractJson(content));
            JsonNode items = root.isArray() ? root : root.path("items");
            List<AiSolution> result = new ArrayList<>();
            if (items.isArray()) {
                for (JsonNode node : items) {
                    AiSolution solution = objectMapper.treeToValue(node, AiSolution.class);
                    solution.setSubject(subject);
                    result.add(solution);
                }
            }
            return result.isEmpty() ? mockDataProvider.mockPractice(mainPoint, subject) : result;
        } catch (Exception ex) {
            throw aiUnavailable(ex);
        }
    }

    private AiSolution parseSolution(String content, String question) throws JsonProcessingException {
        AiSolution solution = objectMapper.readValue(extractJson(content), AiSolution.class);
        solution.setQuestion(question);
        return solution;
    }

    String extractJson(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }
        int objectStart = trimmed.indexOf('{');
        int arrayStart = trimmed.indexOf('[');
        int start;
        char endChar;
        if (arrayStart >= 0 && (objectStart < 0 || arrayStart < objectStart)) {
            start = arrayStart;
            endChar = ']';
        } else {
            start = objectStart;
            endChar = '}';
        }
        int end = trimmed.lastIndexOf(endChar);
        if (start >= 0 && end >= start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private void requireApiKeyOrMock() {
        if (!StringUtils.hasText(properties.getApiKey()) && !properties.isMockEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "AI API key is missing. Set DEEPSEEK_API_KEY or AI_API_KEY before starting the app."
            );
        }
    }

    private ResponseStatusException aiUnavailable(Exception ex) {
        if (ex instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI API call failed: " + ex.getMessage(), ex);
    }
}
