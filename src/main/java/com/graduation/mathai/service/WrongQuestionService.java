package com.graduation.mathai.service;

import com.graduation.mathai.dto.AnalysisResponse;
import com.graduation.mathai.model.AiSolution;
import com.graduation.mathai.model.WrongQuestion;
import com.graduation.mathai.repository.WrongQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WrongQuestionService {
    private final WrongQuestionRepository wrongQuestionRepository;
    private final AiService aiService;

    public WrongQuestionService(WrongQuestionRepository wrongQuestionRepository, AiService aiService) {
        this.wrongQuestionRepository = wrongQuestionRepository;
        this.aiService = aiService;
    }

    public List<WrongQuestion> list(long userId) {
        return wrongQuestionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public WrongQuestion add(long userId, AiSolution solution, String mistakeReason) {
        WrongQuestion item = new WrongQuestion();
        item.setUserId(userId);
        item.setQuestion(solution.getQuestion());
        item.setFinalAnswer(solution.getFinalAnswer());
        item.setExplanation(solution.getExplanation());
        item.setKnowledgePoints(solution.getKnowledgePoints());
        item.setMistakeReason(StringUtils.hasText(mistakeReason) ? mistakeReason : "暂未填写");
        return wrongQuestionRepository.save(item);
    }

    public WrongQuestion update(long userId, long id, String mistakeReason, Boolean mastered) {
        WrongQuestion item = wrongQuestionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("错题不存在：" + id));
        if (mistakeReason != null) {
            item.setMistakeReason(mistakeReason);
        }
        if (mastered != null) {
            item.setMastered(mastered);
        }
        return wrongQuestionRepository.save(item);
    }

    public void delete(long userId, long id) {
        WrongQuestion item = wrongQuestionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("错题不存在：" + id));
        wrongQuestionRepository.delete(item);
    }

    public AnalysisResponse analyze(long userId) {
        List<WrongQuestion> wrongQuestions = list(userId);
        Map<String, Long> stats = knowledgeStats(wrongQuestions);
        String advice = aiService.buildAdvice(wrongQuestions, stats);
        List<String> reviewPlan = buildReviewPlan(stats);
        int masteredCount = (int) wrongQuestions.stream().filter(WrongQuestion::isMastered).count();
        return new AnalysisResponse(wrongQuestions.size(), masteredCount, stats, advice, reviewPlan);
    }

    public List<AiSolution> generatePractice(long userId) {
        List<WrongQuestion> wrongQuestions = list(userId);
        return aiService.generatePractice(wrongQuestions, knowledgeStats(wrongQuestions));
    }

    private Map<String, Long> knowledgeStats(List<WrongQuestion> wrongQuestions) {
        return wrongQuestions.stream()
                .flatMap(item -> item.getKnowledgePoints().stream())
                .collect(Collectors.groupingBy(point -> point, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private List<String> buildReviewPlan(Map<String, Long> stats) {
        if (stats.isEmpty()) {
            return List.of("先添加错题，再生成针对性的复习计划。");
        }
        List<String> plan = new ArrayList<>();
        stats.keySet().stream().limit(3).forEach(point -> {
            plan.add("复习“" + point + "”的概念、公式和典型例题。");
            plan.add("完成 3 道“" + point + "”相关变式题，并记录错误原因。");
        });
        plan.add("隔天回看已掌握标记，未掌握题目继续进入强化练习。");
        return plan;
    }
}
