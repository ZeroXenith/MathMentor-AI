package com.graduation.mathai.dto;

import com.graduation.mathai.model.AiSolution;
import jakarta.validation.constraints.NotBlank;

public final class Requests {
    private Requests() {
    }

    public record SolveRequest(@NotBlank String question) {
    }

    public record WrongQuestionRequest(AiSolution solution, String mistakeReason) {
    }

    public record UpdateWrongQuestionRequest(String mistakeReason, Boolean mastered) {
    }
}
