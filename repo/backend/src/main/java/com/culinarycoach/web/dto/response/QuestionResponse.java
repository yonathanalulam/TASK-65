package com.culinarycoach.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuestionResponse(
    Long id,
    String questionText,
    String questionType,
    String difficulty,
    Long lessonId
) {}
