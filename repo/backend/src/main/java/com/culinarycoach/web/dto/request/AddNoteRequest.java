package com.culinarycoach.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddNoteRequest(
    @NotBlank(message = "Note text is required")
    @Size(max = 5000, message = "Note text must be at most 5000 characters")
    String noteText
) {}
