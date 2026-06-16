package com.graduation.mathai.service;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Builds AI prompts for different tasks (solve, practice, advice).
 * Extracted from AiService for maintainability and testability.
 */
@Component
public class PromptBuilder {

    public String buildSolvePrompt(String question, String subject) {
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

    public String buildPracticePrompt(String subject, Map<String, Long> stats) {
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

    public String buildAdvicePrompt(String subject, Map<String, Long> stats) {
        return """
                You are a Chinese study diagnosis teacher.
                Write a review suggestion within 150 Chinese characters.
                Subject: %s
                Focus on weak knowledge points, review order, and practice method.

                Wrong-question statistics:
                %s
                """.formatted(subject, stats);
    }
}
