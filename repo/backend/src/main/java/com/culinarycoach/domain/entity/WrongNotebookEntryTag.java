package com.culinarycoach.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "wrong_notebook_entry_tags",
       uniqueConstraints = @UniqueConstraint(columnNames = {"entry_id", "tag_id"}))
public class WrongNotebookEntryTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_id", nullable = false)
    private Long entryId;

    @Column(name = "tag_id", nullable = false)
    private Long tagId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEntryId() { return entryId; }
    public void setEntryId(Long entryId) { this.entryId = entryId; }

    public Long getTagId() { return tagId; }
    public void setTagId(Long tagId) { this.tagId = tagId; }
}
