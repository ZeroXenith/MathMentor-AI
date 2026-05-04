package com.graduation.mathai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.mathai.config.AiProperties;
import com.graduation.mathai.model.AiSolution;
import com.graduation.mathai.model.WrongQuestion;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
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
        if (!StringUtils.hasText(properties.getApiKey())) {
            return mockSolution(question);
        }

        String prompt = """
                你是一个严谨的数学老师。请解答用户给出的数学题，并严格返回 JSON，不要返回 Markdown 代码块。
                要求：
                1. 数学公式必须使用 LaTeX，行内公式用 \\( ... \\)，独立公式用 \\[ ... \\]。
                2. explanation 使用 Markdown，包含清晰步骤。
                3. knowledgePoints 和 commonMistakes 使用中文短语数组。
                JSON 字段：
                {
                  "questionType": "题型",
                  "difficulty": "简单/中等/困难",
                  "finalAnswer": "最终答案，支持 LaTeX",
                  "explanation": "详细解析，支持 Markdown + LaTeX",
                  "knowledgePoints": ["知识点"],
                  "commonMistakes": ["易错点"]
                }
                用户题目：
                """ + question;

        try {
            String content = chat(prompt);
            AiSolution solution = parseSolution(content, question);
            if (!StringUtils.hasText(solution.getQuestion())) {
                solution.setQuestion(question);
            }
            return solution;
        } catch (Exception ex) {
            AiSolution fallback = mockSolution(question);
            fallback.setExplanation(fallback.getExplanation()
                    + "\n\n> AI 接口暂时不可用，当前展示本地演示解析。错误信息：" + ex.getMessage());
            return fallback;
        }
    }

    public String buildAdvice(List<WrongQuestion> wrongQuestions, Map<String, Long> stats) {
        if (wrongQuestions.isEmpty()) {
            return "当前错题本为空。建议先添加几道错题，系统会根据知识点分布生成复习建议。";
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            return mockAdvice(stats);
        }

        String prompt = """
                你是学习诊断老师。请根据错题知识点统计，给出 150 字以内的个性化数学复习建议。
                输出自然语言即可，重点指出薄弱知识点、复习顺序和练习方法。
                错题统计：
                """ + stats;
        try {
            return chat(prompt);
        } catch (Exception ex) {
            return mockAdvice(stats);
        }
    }

    public List<AiSolution> generatePractice(List<WrongQuestion> wrongQuestions, Map<String, Long> stats) {
        String mainPoint = stats.keySet().stream().findFirst().orElse("一元二次方程");
        if (!StringUtils.hasText(properties.getApiKey())) {
            return mockPractice(mainPoint);
        }

        String prompt = """
                你是数学出题老师。请根据错题薄弱知识点生成 3 道强化练习题，并严格返回 JSON 数组，不要返回 Markdown 代码块。
                每个元素字段：
                {
                  "question": "题目，公式使用 LaTeX",
                  "questionType": "题型",
                  "difficulty": "简单/中等/困难",
                  "finalAnswer": "答案，公式使用 LaTeX",
                  "explanation": "解析，Markdown + LaTeX",
                  "knowledgePoints": ["知识点"],
                  "commonMistakes": ["易错点"]
                }
                错题知识点统计：
                """ + stats;
        try {
            String content = chat(prompt);
            JsonNode root = objectMapper.readTree(extractJson(content));
            List<AiSolution> result = new ArrayList<>();
            if (root.isArray()) {
                for (JsonNode node : root) {
                    result.add(objectMapper.treeToValue(node, AiSolution.class));
                }
            }
            return result.isEmpty() ? mockPractice(mainPoint) : result;
        } catch (Exception ex) {
            return mockPractice(mainPoint);
        }
    }

    private String chat(String prompt) throws IOException, InterruptedException {
        Map<String, Object> payload = Map.of(
                "model", properties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", "你只输出用户要求的内容，数学公式保持 LaTeX 格式。"),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.2
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getBaseUrl()))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("AI HTTP " + response.statusCode() + ": " + response.body());
        }
        JsonNode root = objectMapper.readTree(response.body());
        return root.path("choices").path(0).path("message").path("content").asText();
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

    private AiSolution mockSolution(String question) {
        AiSolution solution = new AiSolution();
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

                因此：

                \\[
                (x-2)(x-3)=0
                \\]

                所以最终得到 \\(x=2\\) 或 \\(x=3\\)。
                """);
        return solution;
    }

    private String mockAdvice(Map<String, Long> stats) {
        String mainPoint = stats.keySet().stream().findFirst().orElse("基础概念");
        return "当前薄弱点集中在“" + mainPoint + "”。建议先复习对应公式和基本题型，再做 3 到 5 道同类变式题。练习时重点检查计算符号、步骤完整性和最终答案是否回代验证。";
    }

    private List<AiSolution> mockPractice(String knowledgePoint) {
        List<AiSolution> list = new ArrayList<>();
        list.add(mockPracticeItem("求方程 \\(x^2-7x+12=0\\) 的解。", "\\(x=3\\) 或 \\(x=4\\)", knowledgePoint));
        list.add(mockPracticeItem("已知 \\((x-1)(x+5)=0\\)，求 \\(x\\) 的值。", "\\(x=1\\) 或 \\(x=-5\\)", knowledgePoint));
        list.add(mockPracticeItem("求方程 \\(2x^2-8x=0\\) 的解。", "\\(x=0\\) 或 \\(x=4\\)", knowledgePoint));
        return list;
    }

    private AiSolution mockPracticeItem(String question, String answer, String knowledgePoint) {
        AiSolution solution = new AiSolution();
        solution.setQuestion(question);
        solution.setQuestionType("强化练习");
        solution.setDifficulty("中等");
        solution.setFinalAnswer(answer);
        solution.setKnowledgePoints(List.of(knowledgePoint));
        solution.setCommonMistakes(List.of("漏解", "符号计算错误"));
        solution.setExplanation("将方程化为乘积等于零的形式，利用 \\(ab=0\\) 可得 \\(a=0\\) 或 \\(b=0\\)，再分别求解。");
        return solution;
    }
}
