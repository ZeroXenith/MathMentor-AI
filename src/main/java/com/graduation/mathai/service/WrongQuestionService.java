package com.graduation.mathai.service;

import com.graduation.mathai.dto.AnalysisResponse;
import com.graduation.mathai.model.AiSolution;
import com.graduation.mathai.model.WrongQuestion;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class WrongQuestionService {
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final Map<Long, WrongQuestion> storage = new ConcurrentHashMap<>();
    private final AiService aiService;

    public WrongQuestionService(AiService aiService) {
        this.aiService = aiService;
    }

    public List<WrongQuestion> list() {
        return storage.values().stream()
                .sorted(Comparator.comparing(WrongQuestion::getCreatedAt).reversed())
                .toList();
    }

    public WrongQuestion add(AiSolution solution, String mistakeReason) {
        WrongQuestion item = new WrongQuestion();
        item.setId(idGenerator.getAndIncrement());
        item.setQuestion(solution.getQuestion());
        item.setFinalAnswer(solution.getFinalAnswer());
        item.setExplanation(solution.getExplanation());
        item.setKnowledgePoints(solution.getKnowledgePoints());
        item.setMistakeReason(StringUtils.hasText(mistakeReason) ? mistakeReason : "暂未填写");
        storage.put(item.getId(), item);
        return item;
    }

    public WrongQuestion update(long id, String mistakeReason, Boolean mastered) {
        WrongQuestion item = storage.get(id);
        if (item == null) {
            throw new IllegalArgumentException("错题不存在：" + id);
        }
        if (mistakeReason != null) {
            item.setMistakeReason(mistakeReason);
        }
        if (mastered != null) {
            item.setMastered(mastered);
        }
        return item;
    }

    public void delete(long id) {
        storage.remove(id);
    }

    public AnalysisResponse analyze() {
        List<WrongQuestion> wrongQuestions = list();
        Map<String, Long> stats = knowledgeStats(wrongQuestions);
        String advice = aiService.buildAdvice(wrongQuestions, stats);
        List<String> reviewPlan = buildReviewPlan(stats);
        int masteredCount = (int) wrongQuestions.stream().filter(WrongQuestion::isMastered).count();
        return new AnalysisResponse(wrongQuestions.size(), masteredCount, stats, advice, reviewPlan);
    }

    public List<AiSolution> generatePractice() {
        List<WrongQuestion> wrongQuestions = list();
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
