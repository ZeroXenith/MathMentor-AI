package com.graduation.mathai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.mathai.config.AiProperties;
import com.graduation.mathai.model.AiSolution;
import com.graduation.mathai.model.WrongQuestion;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiService {
    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AiService(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public AiSolution solve(String question) {
        return solve(question, "math");
    }

    public AiSolution solve(String question, String subject) {
        String normalizedSubject = normalizeSubject(subject);
        requireApiKeyOrMock();

        if (properties.isMockEnabled() && !StringUtils.hasText(properties.getApiKey())) {
            return mockSolution(question, normalizedSubject);
        }

        try {
            String content = chat(buildSolvePrompt(question, normalizedSubject), true);
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
            return mockAdvice(stats);
        }

        String subject = wrongQuestions.stream()
                .map(WrongQuestion::getSubject)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("math");

        String prompt = """
                You are a Chinese study diagnosis teacher.
                Write a review suggestion within 150 Chinese characters.
                Subject: %s
                Focus on weak knowledge points, review order, and practice method.

                Wrong-question statistics:
                %s
                """.formatted(subject, stats);
        try {
            return chat(prompt, false);
        } catch (Exception ex) {
            throw aiUnavailable(ex);
        }
    }

    public List<AiSolution> generatePractice(List<WrongQuestion> wrongQuestions, Map<String, Long> stats) {
        String subject = wrongQuestions.stream()
                .map(WrongQuestion::getSubject)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("math");
        String mainPoint = stats.keySet().stream().findFirst().orElse("math".equals(subject) ? "一元二次方程" : "基础语法");

        requireApiKeyOrMock();
        if (properties.isMockEnabled() && !StringUtils.hasText(properties.getApiKey())) {
            return mockPractice(mainPoint, subject);
        }

        try {
            String content = chat(buildPracticePrompt(subject, stats), true);
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
            return result.isEmpty() ? mockPractice(mainPoint, subject) : result;
        } catch (Exception ex) {
            throw aiUnavailable(ex);
        }
    }

    private String buildSolvePrompt(String question, String subject) {
        if ("english".equals(subject)) {
            return """
                    You are a careful Chinese English teacher.
                    Analyze the user's English learning question and return only one valid JSON object.
                    Requirements:
                    1. All explanations must be in Simplified Chinese.
                    2. Keep original English words, sentences, or options unchanged when useful.
                    3. explanation must be Markdown with clear sections.
                    4. Do not wrap the JSON in a Markdown code block.

                    JSON schema:
                    {
                      "subject": "english",
                      "questionType": "题型",
                      "difficulty": "简单/中等/困难",
                      "finalAnswer": "最终答案",
                      "explanation": "详细解析",
                      "knowledgePoints": ["知识点"],
                      "commonMistakes": ["易错点"]
                    }

                    User problem:
                    %s
                    """.formatted(question);
        }

        return """
                You are a careful Chinese math teacher.
                Solve the user's math problem and return only one valid JSON object.
                Requirements:
                1. All visible text must be Simplified Chinese.
                2. Use LaTeX for math expressions.
                3. Inline formulas use \\( ... \\), display formulas use \\[ ... \\].
                4. explanation must be Markdown with detailed, step-by-step derivation.
                5. Do not wrap the JSON in a Markdown code block.
                6. Never use plain parentheses like (u = ln x) or square brackets like [formula] as math delimiters.
                7. For math proof/calculation questions, explanation must include:
                   - "### 步骤1：..."
                   - "### 步骤2：..."
                   - at least one formula transformation per key step
                   - final substitution or verification when applicable

                JSON schema:
                {
                  "subject": "math",
                  "questionType": "题型",
                  "difficulty": "简单/中等/困难",
                  "finalAnswer": "最终答案",
                  "explanation": "详细解析",
                  "knowledgePoints": ["知识点"],
                  "commonMistakes": ["易错点"]
                }

                User problem:
                %s
                """.formatted(question);
    }

    private String buildPracticePrompt(String subject, Map<String, Long> stats) {
        if ("english".equals(subject)) {
            return """
                    You are a Chinese English practice writer.
                    Generate 3 reinforcement English practice questions based on the weak knowledge points.
                    Return only one valid JSON object. Do not wrap it in a Markdown code block.

                    JSON schema:
                    {
                      "items": [
                        {
                          "subject": "english",
                          "question": "练习题",
                          "questionType": "题型",
                          "difficulty": "简单/中等/困难",
                          "finalAnswer": "答案",
                          "explanation": "中文解析，Markdown",
                          "knowledgePoints": ["知识点"],
                          "commonMistakes": ["易错点"]
                        }
                      ]
                    }

                    Weak-point statistics:
                    %s
                    """.formatted(stats);
        }

        return """
                You are a Chinese math problem writer.
                Generate 3 reinforcement practice problems based on the weak knowledge points.
                Return only one valid JSON object. Do not wrap it in a Markdown code block.
                Formula output rule: use only \\( ... \\) for inline math and \\[ ... \\] for display math.

                JSON schema:
                {
                  "items": [
                    {
                      "subject": "math",
                      "question": "题目，公式使用 LaTeX",
                      "questionType": "题型",
                      "difficulty": "简单/中等/困难",
                      "finalAnswer": "答案，公式使用 LaTeX",
                      "explanation": "解析，Markdown + LaTeX",
                      "knowledgePoints": ["知识点"],
                      "commonMistakes": ["易错点"]
                    }
                  ]
                }

                Weak-point statistics:
                %s
                """.formatted(stats);
    }

    private String chat(String prompt, boolean jsonObject) throws IOException, InterruptedException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", properties.getModel());
        payload.put("messages", List.of(
                Map.of("role", "system", "content", "Return exactly what the user asks for. Keep output concise and structured."),
                Map.of("role", "user", "content", prompt)
        ));
        payload.put("temperature", 0.2);
        payload.put("max_tokens", 2048);
        payload.put("stream", false);
        if (jsonObject) {
            payload.put("response_format", Map.of("type", "json_object"));
        }

        HttpResponse<String> response = sendChatRequest(payload);
        if (isHttpError(response) && jsonObject) {
            payload.remove("response_format");
            response = sendChatRequest(payload);
        }
        if (isHttpError(response)) {
            throw new IOException("AI HTTP " + response.statusCode() + ": " + extractErrorMessage(response.body()));
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode choice = root.path("choices").path(0);
        String content = choice.path("message").path("content").asText();
        if (!StringUtils.hasText(content)) {
            throw new IOException("AI response content is empty, finish_reason=" + choice.path("finish_reason").asText("unknown"));
        }
        return content;
    }

    private HttpResponse<String> sendChatRequest(Map<String, Object> payload) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(chatCompletionsUri())
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private URI chatCompletionsUri() {
        String baseUrl = properties.getBaseUrl().trim();
        if (baseUrl.endsWith("/chat/completions") || baseUrl.endsWith("/chat/completions/")) {
            return URI.create(baseUrl);
        }
        String separator = baseUrl.endsWith("/") ? "" : "/";
        return URI.create(baseUrl + separator + "chat/completions");
    }

    private boolean isHttpError(HttpResponse<String> response) {
        return response.statusCode() < 200 || response.statusCode() >= 300;
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

    private String extractErrorMessage(String body) {
        try {
            String message = objectMapper.readTree(body).path("error").path("message").asText();
            if (StringUtils.hasText(message)) {
                return removeApiKeyHint(message);
            }
        } catch (JsonProcessingException ignored) {
            // Use the raw body below.
        }
        return removeApiKeyHint(body);
    }

    private String removeApiKeyHint(String message) {
        return message.replaceAll("(?i),?\\s*Your api key:[^,}]+", "").trim();
    }

    private AiSolution parseSolution(String content, String question) throws JsonProcessingException {
        AiSolution solution = objectMapper.readValue(extractJson(content), AiSolution.class);
        solution.setQuestion(question);
        return solution;
    }

    private String extractJson(String content) {
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

    private String normalizeSubject(String subject) {
        if (!StringUtils.hasText(subject)) {
            return "math";
        }
        String value = subject.trim().toLowerCase();
        return "english".equals(value) ? "english" : "math";
    }

    private AiSolution mockSolution(String question, String subject) {
        if ("english".equals(subject)) {
            AiSolution solution = new AiSolution();
            solution.setSubject("english");
            solution.setQuestion(question);
            solution.setQuestionType("语法题");
            solution.setDifficulty("中等");
            solution.setFinalAnswer("建议先判断句子结构，再定位谓语、从句和关键词。");
            solution.setKnowledgePoints(List.of("句子结构", "语法分析", "阅读理解"));
            solution.setCommonMistakes(List.of("忽略上下文", "只翻译单词不分析句子结构"));
            solution.setExplanation("### 解析思路\n先划分主干，再处理修饰成分，最后结合上下文确定答案。");
            return solution;
        }

        AiSolution solution = new AiSolution();
        solution.setSubject("math");
        solution.setQuestion(question);
        solution.setQuestionType("解答题");
        solution.setDifficulty("中等");
        solution.setFinalAnswer("\\(x=2\\) 或 \\(x=3\\)");
        solution.setKnowledgePoints(List.of("一元二次方程", "因式分解", "方程求根"));
        solution.setCommonMistakes(List.of("移项时符号出错", "分解因式后漏掉一个根"));
        solution.setExplanation("""
                ### 解题过程
                先将方程整理为标准形式：

                \\[
                x^2 - 5x + 6 = 0
                \\]

                对左侧进行因式分解：

                \\[
                x^2 - 5x + 6 = (x-2)(x-3)
                \\]

                所以最终得到 \\(x=2\\) 或 \\(x=3\\)。
                """);
        return solution;
    }

    private String mockAdvice(Map<String, Long> stats) {
        String mainPoint = stats.keySet().stream().findFirst().orElse("基础概念");
        return "当前薄弱点集中在“" + mainPoint + "”。建议先复习对应概念和典型题，再做 3 到 5 道同类变式练习。";
    }

    private List<AiSolution> mockPractice(String knowledgePoint, String subject) {
        if ("english".equals(subject)) {
            return List.of(
                    mockPracticeItem("Choose the best answer: I have lived here ___ 2020.", "since", knowledgePoint, "english"),
                    mockPracticeItem("Translate: Practice makes perfect.", "熟能生巧。", knowledgePoint, "english"),
                    mockPracticeItem("Find the main clause: Although it was raining, we went out.", "we went out", knowledgePoint, "english")
            );
        }
        return List.of(
                mockPracticeItem("求方程 \\(x^2-7x+12=0\\) 的解。", "\\(x=3\\) 或 \\(x=4\\)", knowledgePoint, "math"),
                mockPracticeItem("已知 \\((x-1)(x+5)=0\\)，求 \\(x\\) 的值。", "\\(x=1\\) 或 \\(x=-5\\)", knowledgePoint, "math"),
                mockPracticeItem("求方程 \\(2x^2-8x=0\\) 的解。", "\\(x=0\\) 或 \\(x=4\\)", knowledgePoint, "math")
        );
    }

    private AiSolution mockPracticeItem(String question, String answer, String knowledgePoint, String subject) {
        AiSolution solution = new AiSolution();
        solution.setSubject(subject);
        solution.setQuestion(question);
        solution.setQuestionType("强化练习");
        solution.setDifficulty("中等");
        solution.setFinalAnswer(answer);
        solution.setKnowledgePoints(List.of(knowledgePoint));
        solution.setCommonMistakes(List.of("审题不细", "步骤不完整"));
        solution.setExplanation("围绕薄弱知识点进行同类练习，完成后对照解析复盘错误原因。");
        return solution;
    }
}
