package com.culinarycoach.service;

import com.culinarycoach.audit.TraceContext;
import com.culinarycoach.domain.entity.*;
import com.culinarycoach.domain.enums.NotebookEntryStatus;
import com.culinarycoach.domain.repository.*;
import com.culinarycoach.web.dto.response.NotebookEntryDetailResponse;
import com.culinarycoach.web.dto.response.NotebookEntryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Service
public class NotebookService {

    private static final Logger log = LoggerFactory.getLogger(NotebookService.class);

    private final WrongNotebookEntryRepository entryRepository;
    private final WrongNotebookTagRepository tagRepository;
    private final WrongNotebookEntryTagRepository entryTagRepository;
    private final WrongNotebookNoteRepository noteRepository;
    private final WrongNotebookFavoriteRepository favoriteRepository;
    private final QuestionRepository questionRepository;
    private final PrivacyAccessLogRepository privacyAccessLogRepository;

    public NotebookService(WrongNotebookEntryRepository entryRepository,
                           WrongNotebookTagRepository tagRepository,
                           WrongNotebookEntryTagRepository entryTagRepository,
                           WrongNotebookNoteRepository noteRepository,
                           WrongNotebookFavoriteRepository favoriteRepository,
                           QuestionRepository questionRepository,
                           PrivacyAccessLogRepository privacyAccessLogRepository) {
        this.entryRepository = entryRepository;
        this.tagRepository = tagRepository;
        this.entryTagRepository = entryTagRepository;
        this.noteRepository = noteRepository;
        this.favoriteRepository = favoriteRepository;
        this.questionRepository = questionRepository;
        this.privacyAccessLogRepository = privacyAccessLogRepository;
    }

    @Transactional(readOnly = true)
    public Page<NotebookEntryResponse> listEntries(Long userId, NotebookEntryStatus status, Pageable pageable) {
        Collection<NotebookEntryStatus> statuses;
        if (status != null) {
            statuses = Set.of(status);
        } else {
            statuses = Set.of(NotebookEntryStatus.ACTIVE, NotebookEntryStatus.FAVORITED);
        }

        Page<WrongNotebookEntry> entries = entryRepository.findByUserIdAndStatusIn(userId, statuses, pageable);
        return entries.map(entry -> toResponse(entry));
    }

    /**
     * List entries for a subject user, viewed by a different user (parent/coach/admin).
     * Logs a privacy access event.
     */
    @Transactional(readOnly = true)
    public Page<NotebookEntryResponse> listEntriesAsViewer(Long viewerUserId, String viewerRole,
                                                            Long subjectUserId, NotebookEntryStatus status,
                                                            Pageable pageable) {
        logPrivacyAccess(viewerUserId, viewerRole, subjectUserId, "NOTEBOOK_ENTRY", null, "LIST_ENTRIES");
        return listEntries(subjectUserId, status, pageable);
    }

    @Transactional(readOnly = true)
    public NotebookEntryDetailResponse getEntry(Long entryId, Long userId) {
        WrongNotebookEntry entry = loadEntryForUser(entryId, userId);
        return toDetailResponse(entry);
    }

    /**
     * Get entry detail as a viewer (parent/coach/admin). Logs a privacy access event.
     */
    @Transactional(readOnly = true)
    public NotebookEntryDetailResponse getEntryAsViewer(Long entryId, Long viewerUserId,
                                                         String viewerRole, Long subjectUserId) {
        logPrivacyAccess(viewerUserId, viewerRole, subjectUserId,
            "NOTEBOOK_ENTRY", String.valueOf(entryId), "VIEW_ENTRY");
        WrongNotebookEntry entry = loadEntryForUser(entryId, subjectUserId);
        return toDetailResponse(entry);
    }

    @Transactional
    public NotebookEntryDetailResponse addNote(Long entryId, Long userId, String noteText) {
        WrongNotebookEntry entry = loadEntryForUser(entryId, userId);

        WrongNotebookNote note = new WrongNotebookNote();
        note.setEntryId(entryId);
        note.setNoteText(noteText);
        noteRepository.save(note);

        // Update denormalized latest_note
        entry.setLatestNote(noteText);
        entryRepository.save(entry);

        log.info("Added note to notebook entry {} for user {}", entryId, userId);
        return toDetailResponse(entry);
    }

    @Transactional
    public NotebookEntryDetailResponse addTag(Long entryId, Long userId, String tagLabel) {
        WrongNotebookEntry entry = loadEntryForUser(entryId, userId);

        // Find or create tag
        WrongNotebookTag tag = tagRepository.findByLabel(tagLabel.trim())
            .orElseGet(() -> {
                WrongNotebookTag newTag = new WrongNotebookTag();
                newTag.setLabel(tagLabel.trim());
                newTag.setCreatedByUserId(userId);
                return tagRepository.save(newTag);
            });

        // Check for duplicate
        var existingLink = entryTagRepository.findByEntryIdAndTagId(entryId, tag.getId());
        if (existingLink.isPresent()) {
            return toDetailResponse(entry);
        }

        WrongNotebookEntryTag entryTag = new WrongNotebookEntryTag();
        entryTag.setEntryId(entryId);
        entryTag.setTagId(tag.getId());
        entryTagRepository.save(entryTag);

        log.info("Added tag '{}' to notebook entry {} for user {}", tagLabel, entryId, userId);
        return toDetailResponse(entry);
    }

    @Transactional
    public NotebookEntryDetailResponse removeTag(Long entryId, Long userId, Long tagId) {
        WrongNotebookEntry entry = loadEntryForUser(entryId, userId);

        entryTagRepository.findByEntryIdAndTagId(entryId, tagId)
            .ifPresent(entryTagRepository::delete);

        log.info("Removed tag {} from notebook entry {} for user {}", tagId, entryId, userId);
        return toDetailResponse(entry);
    }

    @Transactional
    public NotebookEntryResponse toggleFavorite(Long entryId, Long userId) {
        WrongNotebookEntry entry = loadEntryForUser(entryId, userId);

        if (entry.getStatus() == NotebookEntryStatus.ACTIVE) {
            entry.setStatus(NotebookEntryStatus.FAVORITED);
            entry.setFavorite(true);

            // Create favorite record
            if (!favoriteRepository.existsByUserIdAndEntryId(userId, entryId)) {
                WrongNotebookFavorite fav = new WrongNotebookFavorite();
                fav.setUserId(userId);
                fav.setEntryId(entryId);
                favoriteRepository.save(fav);
            }
        } else if (entry.getStatus() == NotebookEntryStatus.FAVORITED) {
            entry.setStatus(NotebookEntryStatus.ACTIVE);
            entry.setFavorite(false);

            // Remove favorite record
            favoriteRepository.findByUserIdAndEntryId(userId, entryId)
                .ifPresent(favoriteRepository::delete);
        } else {
            throw new IllegalStateException(
                "Can only toggle favorite on ACTIVE or FAVORITED entries. Current: " + entry.getStatus());
        }

        entryRepository.save(entry);
        log.info("Toggled favorite for notebook entry {} user {}: now {}", entryId, userId, entry.getStatus());
        return toResponse(entry);
    }

    @Transactional
    public NotebookEntryResponse resolveEntry(Long entryId, Long userId) {
        WrongNotebookEntry entry = loadEntryForUser(entryId, userId);
        entry.setStatus(NotebookEntryStatus.RESOLVED);
        entryRepository.save(entry);
        log.info("Resolved notebook entry {} for user {}", entryId, userId);
        return toResponse(entry);
    }

    @Transactional
    public NotebookEntryResponse archiveEntry(Long entryId, Long userId) {
        WrongNotebookEntry entry = loadEntryForUser(entryId, userId);
        entry.setStatus(NotebookEntryStatus.ARCHIVED);
        entryRepository.save(entry);
        log.info("Archived notebook entry {} for user {}", entryId, userId);
        return toResponse(entry);
    }

    @Transactional
    public NotebookEntryResponse reactivateEntry(Long entryId, Long userId) {
        WrongNotebookEntry entry = loadEntryForUser(entryId, userId);

        if (entry.getStatus() != NotebookEntryStatus.RESOLVED
            && entry.getStatus() != NotebookEntryStatus.ARCHIVED) {
            throw new IllegalStateException(
                "Only RESOLVED or ARCHIVED entries can be reactivated. Current: " + entry.getStatus());
        }

        entry.setStatus(NotebookEntryStatus.ACTIVE);
        entry.setFavorite(false);
        entryRepository.save(entry);
        log.info("Reactivated notebook entry {} for user {}", entryId, userId);
        return toResponse(entry);
    }

    private WrongNotebookEntry loadEntryForUser(Long entryId, Long userId) {
        WrongNotebookEntry entry = entryRepository.findById(entryId)
            .orElseThrow(() -> new IllegalArgumentException("Notebook entry not found: " + entryId));
        if (!entry.getUserId().equals(userId)) {
            throw new SecurityException("Access denied to notebook entry " + entryId);
        }
        return entry;
    }

    private NotebookEntryResponse toResponse(WrongNotebookEntry entry) {
        List<String> tags = loadTagLabels(entry.getId());

        String questionText = questionRepository.findById(entry.getQuestionId())
            .map(Question::getQuestionText)
            .orElse(null);

        return new NotebookEntryResponse(
            entry.getId(),
            questionText,
            entry.getStatus().name(),
            entry.getFailCount(),
            entry.isFavorite(),
            tags.isEmpty() ? null : tags,
            entry.getLatestNote(),
            entry.getLastAttemptAt()
        );
    }

    private NotebookEntryDetailResponse toDetailResponse(WrongNotebookEntry entry) {
        List<String> tags = loadTagLabels(entry.getId());

        Question question = questionRepository.findById(entry.getQuestionId()).orElse(null);
        String questionText = question != null ? question.getQuestionText() : null;
        String questionExplanation = question != null ? question.getExplanation() : null;

        List<WrongNotebookNote> notes = noteRepository.findByEntryIdOrderByCreatedAtDesc(entry.getId());
        List<NotebookEntryDetailResponse.NoteItem> noteItems = notes.stream()
            .map(n -> new NotebookEntryDetailResponse.NoteItem(n.getId(), n.getNoteText(), n.getCreatedAt()))
            .toList();

        return new NotebookEntryDetailResponse(
            entry.getId(),
            questionText,
            entry.getStatus().name(),
            entry.getFailCount(),
            entry.isFavorite(),
            tags.isEmpty() ? null : tags,
            entry.getLatestNote(),
            entry.getLastAttemptAt(),
            noteItems.isEmpty() ? null : noteItems,
            questionExplanation
        );
    }

    private List<String> loadTagLabels(Long entryId) {
        List<WrongNotebookEntryTag> entryTags = entryTagRepository.findByEntryId(entryId);
        return entryTags.stream()
            .map(et -> tagRepository.findById(et.getTagId())
                .map(WrongNotebookTag::getLabel)
                .orElse(null))
            .filter(label -> label != null)
            .toList();
    }

    private void logPrivacyAccess(Long viewerUserId, String viewerRole, Long subjectUserId,
                                   String resourceType, String resourceId, String reasonCode) {
        try {
            PrivacyAccessLog accessLog = new PrivacyAccessLog();
            accessLog.setViewerUserId(viewerUserId);
            accessLog.setViewerRole(viewerRole);
            accessLog.setSubjectUserId(subjectUserId);
            accessLog.setResourceType(resourceType);
            accessLog.setResourceId(resourceId);
            accessLog.setReasonCode(reasonCode);
            accessLog.setTraceId(TraceContext.get());
            privacyAccessLogRepository.save(accessLog);
        } catch (Exception e) {
            log.error("Failed to log privacy access: viewer={}, subject={}, resource={}",
                viewerUserId, subjectUserId, resourceType, e);
        }
    }
}
