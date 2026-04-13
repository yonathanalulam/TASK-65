package com.culinarycoach.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotebookEntryDetailResponse(
    Long id,
    String questionText,
    String status,
    int failCount,
    boolean isFavorite,
    List<String> tags,
    String latestNote,
    Instant lastAttemptAt,
    List<NoteItem> notes,
    String questionExplanation
) {
    public record NoteItem(
        Long id,
        String noteText,
        Instant createdAt
    ) {}
}
