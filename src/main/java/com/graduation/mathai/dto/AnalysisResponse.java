package com.graduation.mathai.dto;

import java.util.List;
import java.util.Map;

public record AnalysisResponse(
        int totalWrong,
        int masteredCount,
        Map<String, Long> knowledgeStats,
        String advice,
        List<String> reviewPlan
) {
}
