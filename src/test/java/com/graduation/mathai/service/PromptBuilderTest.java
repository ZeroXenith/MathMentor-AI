package com.graduation.mathai.service;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PromptBuilderTest {
    private final PromptBuilder promptBuilder = new PromptBuilder();

    @Test
    void buildSolvePrompt_math_containsLatexInstruction() {
        String prompt = promptBuilder.buildSolvePrompt("x^2 + 2x + 1 = 0", "math");
        assertTrue(prompt.contains("LaTeX"));
        assertTrue(prompt.contains("math teacher"));
        assertTrue(prompt.contains("x^2 + 2x + 1 = 0"));
    }

    @Test
    void buildSolvePrompt_english_containsEnglishTeacher() {
        String prompt = promptBuilder.buildSolvePrompt("What is the past tense of 'go'?", "english");
        assertTrue(prompt.contains("English teacher"));
        assertTrue(prompt.contains("go"));
    }

    @Test
    void buildPracticePrompt_math_generatesReinforcementQuestions() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("一元二次方程", 3L);
        String prompt = promptBuilder.buildPracticePrompt("math", stats);
        assertTrue(prompt.contains("reinforcement"));
        assertTrue(prompt.contains("3"));
    }

    @Test
    void buildPracticePrompt_english_generatesEnglishReinforcement() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("主谓一致", 2L);
        String prompt = promptBuilder.buildPracticePrompt("english", stats);
        assertTrue(prompt.contains("English practice"));
        assertTrue(prompt.contains("主谓一致"));
    }

    @Test
    void buildAdvicePrompt_containsStatsAndSubject() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("数列", 5L);
        String prompt = promptBuilder.buildAdvicePrompt("math", stats);
        assertTrue(prompt.contains("math"));
        assertTrue(prompt.contains("数列"));
        assertTrue(prompt.contains("5"));
    }
}
