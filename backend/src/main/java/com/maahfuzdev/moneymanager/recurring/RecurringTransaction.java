package com.maahfuzdev.moneymanager.recurring;

import com.maahfuzdev.moneymanager.transaction.TransactionType;
import com.maahfuzdev.moneymanager.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "recurring_transactions")
public class RecurringTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_recurring_transactions_user"))
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 60)
    private String category;

    @Column(length = 500)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RecurrenceFrequency frequency;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "next_run_date", nullable = false)
    private LocalDate nextRunDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RecurringTransaction() { }

    public RecurringTransaction(AppUser user, RecurringTransactionRequest request) {
        this.user = user;
        this.type = request.type();
        this.amount = request.amount();
        this.category = request.category().trim();
        this.note = cleanNote(request.note());
        this.frequency = request.frequency();
        this.startDate = request.startDate();
        this.nextRunDate = request.startDate();
        this.endDate = request.endDate();
        this.active = true;
    }

    public RecurringTransaction(AppUser user, RecurringTransactionResponse backup) {
        this.user = user;
        this.type = backup.type();
        this.amount = backup.amount();
        this.category = backup.category();
        this.note = cleanNote(backup.note());
        this.frequency = backup.frequency();
        this.startDate = backup.startDate();
        this.nextRunDate = backup.nextRunDate();
        this.endDate = backup.endDate();
        this.active = backup.active();
    }

    public void setActive(boolean active) { this.active = active; }

    public boolean isFinished() {
        return endDate != null && nextRunDate.isAfter(endDate);
    }

    public void recordOccurrence() {
        nextRunDate = frequency.nextDate(nextRunDate, startDate);
        if (endDate != null && nextRunDate.isAfter(endDate)) active = false;
    }

    private static String cleanNote(String note) {
        if (note == null) return null;
        String cleaned = note.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    @PrePersist
    void onCreate() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getCategory() { return category; }
    public String getNote() { return note; }
    public RecurrenceFrequency getFrequency() { return frequency; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getNextRunDate() { return nextRunDate; }
    public LocalDate getEndDate() { return endDate; }
    public boolean isActive() { return active; }
}
