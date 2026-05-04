package com.graduation.mathai.controller;

import com.graduation.mathai.dto.AnalysisResponse;
import com.graduation.mathai.dto.Requests.SolveRequest;
import com.graduation.mathai.dto.Requests.UpdateWrongQuestionRequest;
import com.graduation.mathai.dto.Requests.WrongQuestionRequest;
import com.graduation.mathai.model.AiSolution;
import com.graduation.mathai.model.WrongQuestion;
import com.graduation.mathai.service.AiService;
import com.graduation.mathai.service.WrongQuestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final AiService aiService;
    private final WrongQuestionService wrongQuestionService;

    public ApiController(AiService aiService, WrongQuestionService wrongQuestionService) {
        this.aiService = aiService;
        this.wrongQuestionService = wrongQuestionService;
    }

    @PostMapping("/solve")
    public AiSolution solve(@Valid @RequestBody SolveRequest request) {
        return aiService.solve(request.question());
    }

    @GetMapping("/wrong-questions")
    public List<WrongQuestion> wrongQuestions() {
        return wrongQuestionService.list();
    }

    @PostMapping("/wrong-questions")
    public WrongQuestion addWrongQuestion(@RequestBody WrongQuestionRequest request) {
        return wrongQuestionService.add(request.solution(), request.mistakeReason());
    }

    @PutMapping("/wrong-questions/{id}")
    public WrongQuestion updateWrongQuestion(@PathVariable long id, @RequestBody UpdateWrongQuestionRequest request) {
        return wrongQuestionService.update(id, request.mistakeReason(), request.mastered());
    }

    @DeleteMapping("/wrong-questions/{id}")
    public ResponseEntity<Void> deleteWrongQuestion(@PathVariable long id) {
        wrongQuestionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/analysis")
    public AnalysisResponse analysis() {
        return wrongQuestionService.analyze();
    }

    @PostMapping("/practice")
    public List<AiSolution> practice() {
        return wrongQuestionService.generatePractice();
    }
}
