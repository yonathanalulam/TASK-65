package com.culinarycoach.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitAnswerRequest(
    @NotBlank(message = "User answer is required")
    @Size(max = 5000, message = "Answer must be at most 5000 characters")
    String userAnswer,

    boolean flagged
) {}
