package com.maahfuzdev.moneymanager.goal;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "savings_goal_contributions")
public class GoalContribution {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", nullable = false, foreignKey = @ForeignKey(name = "fk_goal_contributions_goal"))
    private SavingsGoal goal;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(length = 300)
    private String note;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected GoalContribution() { }
    public GoalContribution(SavingsGoal goal, BigDecimal amount, String note) {
        this.goal = goal; this.amount = amount; this.note = note;
    }
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public Long getId() { return id; }
    public Long getGoalId() { return goal.getId(); }
    public BigDecimal getAmount() { return amount; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }
}
