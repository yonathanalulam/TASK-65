package com.culinarycoach.domain.entity;

import com.culinarycoach.domain.enums.TipDisplayMode;
import jakarta.persistence.*;

@Entity
@Table(name = "step_tip_bindings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"step_id", "tip_card_id"}))
public class StepTipBinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "step_id", nullable = false)
    private Long stepId;

    @Column(name = "tip_card_id", nullable = false)
    private Long tipCardId;

    @Enumerated(EnumType.STRING)
    @Column(name = "display_mode", nullable = false, length = 20)
    private TipDisplayMode displayMode = TipDisplayMode.SHORT;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStepId() { return stepId; }
    public void setStepId(Long stepId) { this.stepId = stepId; }

    public Long getTipCardId() { return tipCardId; }
    public void setTipCardId(Long tipCardId) { this.tipCardId = tipCardId; }

    public TipDisplayMode getDisplayMode() { return displayMode; }
    public void setDisplayMode(TipDisplayMode displayMode) { this.displayMode = displayMode; }
}
