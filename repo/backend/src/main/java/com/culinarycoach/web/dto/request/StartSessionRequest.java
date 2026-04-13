package com.culinarycoach.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record StartSessionRequest(
    @NotBlank(message = "Recipe title is required")
    @Size(max = 255, message = "Recipe title must be at most 255 characters")
    String recipeTitle,

    Long lessonId,

    @NotEmpty(message = "At least one step is required")
    @Valid
    List<StepInput> steps
) {
    public record StepInput(
        @NotBlank(message = "Step title is required")
        @Size(max = 255, message = "Step title must be at most 255 characters")
        String title,

        String description,

        Integer expectedDurationSeconds,

        boolean hasTimer,

        Integer timerDurationSeconds,

        @Size(max = 500, message = "Reminder text must be at most 500 characters")
        String reminderText
    ) {}
}
