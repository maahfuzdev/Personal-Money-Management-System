package com.maahfuzdev.moneymanager.account;

import com.maahfuzdev.moneymanager.user.AppUser;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "money_account_adjustments")
public class MoneyAccountAdjustment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private MoneyAccount account;

    @Column(name = "previous_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal previousBalance;

    @Column(name = "actual_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal actualBalance;

    @Column(name = "adjustment_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal adjustmentAmount;

    @Column(name = "adjustment_date", nullable = false)
    private LocalDate adjustmentDate;

    @Column(length = 300)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MoneyAccountAdjustment() { }

    public MoneyAccountAdjustment(AppUser user, MoneyAccount account, BigDecimal previousBalance,
                                  BigDecimal actualBalance, LocalDate adjustmentDate, String note) {
        this.user = user;
        this.account = account;
        this.previousBalance = previousBalance;
        this.actualBalance = actualBalance;
        this.adjustmentAmount = actualBalance.subtract(previousBalance);
        this.adjustmentDate = adjustmentDate;
        this.note = note;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public MoneyAccount getAccount() { return account; }
    public BigDecimal getPreviousBalance() { return previousBalance; }
    public BigDecimal getActualBalance() { return actualBalance; }
    public BigDecimal getAdjustmentAmount() { return adjustmentAmount; }
    public LocalDate getAdjustmentDate() { return adjustmentDate; }
    public String getNote() { return note; }
}
