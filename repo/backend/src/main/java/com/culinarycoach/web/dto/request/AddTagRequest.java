package com.culinarycoach.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddTagRequest(
    @NotBlank(message = "Tag label is required")
    @Size(max = 50, message = "Tag label must be at most 50 characters")
    String tagLabel
) {}
