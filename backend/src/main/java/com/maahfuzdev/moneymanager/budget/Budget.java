package com.maahfuzdev.moneymanager.budget;

import com.maahfuzdev.moneymanager.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "monthly_budgets", uniqueConstraints = @UniqueConstraint(
        name = "uk_budgets_user_category_month", columnNames = {"user_id", "category", "month_start"}))
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_budgets_user"))
    private AppUser user;

    @Column(nullable = false, length = 60)
    private String category;

    @Column(name = "monthly_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyLimit;

    @Column(name = "month_start", nullable = false)
    private LocalDate monthStart;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Budget() {
    }

    public Budget(AppUser user, String category, BigDecimal monthlyLimit, LocalDate monthStart) {
        this.user = user;
        this.category = category;
        this.monthlyLimit = monthlyLimit;
        this.monthStart = monthStart;
    }

    public void update(String category, BigDecimal monthlyLimit, LocalDate monthStart) {
        this.category = category;
        this.monthlyLimit = monthlyLimit;
        this.monthStart = monthStart;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public String getCategory() { return category; }
    public BigDecimal getMonthlyLimit() { return monthlyLimit; }
    public LocalDate getMonthStart() { return monthStart; }
}
