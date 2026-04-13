package com.culinarycoach.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubmitAnswerResponse(
    String classification,
    boolean correct,
    String explanation,
    boolean notebookEntryCreated
) {}
