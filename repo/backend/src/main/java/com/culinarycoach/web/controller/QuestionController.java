package com.culinarycoach.web.controller;

import com.culinarycoach.security.auth.AuthenticatedUserResolver;
import com.culinarycoach.service.QuestionService;
import com.culinarycoach.web.dto.request.SubmitAnswerRequest;
import com.culinarycoach.web.dto.response.ApiResponse;
import com.culinarycoach.web.dto.response.QuestionResponse;
import com.culinarycoach.web.dto.response.SubmitAnswerResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/questions")
public class QuestionController {

    private final QuestionService questionService;
    private final AuthenticatedUserResolver userResolver;

    public QuestionController(QuestionService questionService,
                               AuthenticatedUserResolver userResolver) {
        this.questionService = questionService;
        this.userResolver = userResolver;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<QuestionResponse>>> listQuestions(
            @RequestParam Long lessonId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<QuestionResponse> questions = questionService.getQuestionsByLesson(lessonId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(questions));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionResponse>> getQuestion(@PathVariable Long id) {
        QuestionResponse question = questionService.getQuestion(id);
        return ResponseEntity.ok(ApiResponse.ok(question));
    }

    @PostMapping("/{id}/answer")
    public ResponseEntity<ApiResponse<SubmitAnswerResponse>> submitAnswer(
            @PathVariable Long id,
            @Valid @RequestBody SubmitAnswerRequest request,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        SubmitAnswerResponse response = questionService.submitAnswer(
            userId, id, null, request.userAnswer(), request.flagged());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
