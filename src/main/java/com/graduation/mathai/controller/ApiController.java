package com.graduation.mathai.controller;

import com.graduation.mathai.dto.AnalysisResponse;
import com.graduation.mathai.dto.Requests.SolveRequest;
import com.graduation.mathai.dto.Requests.UpdateWrongQuestionRequest;
import com.graduation.mathai.dto.Requests.WrongQuestionRequest;
import com.graduation.mathai.model.AiSolution;
import com.graduation.mathai.model.WrongQuestion;
import com.graduation.mathai.service.AuthService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final AiService aiService;
    private final WrongQuestionService wrongQuestionService;
    private final AuthService authService;

    public ApiController(AiService aiService, WrongQuestionService wrongQuestionService, AuthService authService) {
        this.aiService = aiService;
        this.wrongQuestionService = wrongQuestionService;
        this.authService = authService;
    }

    @PostMapping("/solve")
    public AiSolution solve(@Valid @RequestBody SolveRequest request) {
        return aiService.solve(request.question(), request.subject());
    }

    @GetMapping("/wrong-questions")
    public List<WrongQuestion> wrongQuestions(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        return wrongQuestionService.list(authService.requireUserId(token));
    }

    @PostMapping("/wrong-questions")
    public WrongQuestion addWrongQuestion(@RequestHeader(value = "X-Auth-Token", required = false) String token,
                                          @RequestBody WrongQuestionRequest request) {
        return wrongQuestionService.add(authService.requireUserId(token), request.solution(), request.mistakeReason());
    }

    @PutMapping("/wrong-questions/{id}")
    public WrongQuestion updateWrongQuestion(@RequestHeader(value = "X-Auth-Token", required = false) String token,
                                             @PathVariable long id,
                                             @RequestBody UpdateWrongQuestionRequest request) {
        return wrongQuestionService.update(authService.requireUserId(token), id, request.mistakeReason(), request.mastered());
    }

    @DeleteMapping("/wrong-questions/{id}")
    public ResponseEntity<Void> deleteWrongQuestion(@RequestHeader(value = "X-Auth-Token", required = false) String token,
                                                    @PathVariable long id) {
        wrongQuestionService.delete(authService.requireUserId(token), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/analysis")
    public AnalysisResponse analysis(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        return wrongQuestionService.analyze(authService.requireUserId(token));
    }

    @PostMapping("/practice")
    public List<AiSolution> practice(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        return wrongQuestionService.generatePractice(authService.requireUserId(token));
    }
}
