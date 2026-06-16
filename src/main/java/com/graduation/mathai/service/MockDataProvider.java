package com.graduation.mathai.service;

import com.graduation.mathai.model.AiSolution;
import com.graduation.mathai.util.SubjectUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Provides mock AI responses for demo/offline usage when no API key is configured.
 */
@Component
public class MockDataProvider {

    public AiSolution mockSolution(String question, String subject) {
        if (SubjectUtils.ENGLISH.equals(subject)) {
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

    public String mockAdvice(Map<String, Long> stats) {
        String mainPoint = stats.keySet().stream().findFirst().orElse("基础概念");
        return "当前薄弱点集中在「" + mainPoint + "」。建议先复习对应概念和典型题，再做 3 到 5 道同类变式练习。";
    }

    public List<AiSolution> mockPractice(String knowledgePoint, String subject) {
        if (SubjectUtils.ENGLISH.equals(subject)) {
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
