package com.culinarycoach.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitDrillAnswerRequest(
    @NotNull(message = "Question ID is required")
    Long questionId,

    @NotBlank(message = "Answer is required")
    @Size(max = 5000, message = "Answer must be at most 5000 characters")
    String answer
) {}
