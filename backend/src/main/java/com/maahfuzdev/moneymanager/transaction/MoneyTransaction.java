package com.maahfuzdev.moneymanager.transaction;

import com.maahfuzdev.moneymanager.user.AppUser;
import com.maahfuzdev.moneymanager.account.MoneyAccount;
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
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "money_transactions")
public class MoneyTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transactions_user"))
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transactions_account"))
    private MoneyAccount account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 60)
    private String category;

    @Column(length = 500)
    private String note;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MoneyTransaction() {
    }

    public MoneyTransaction(AppUser user, MoneyAccount account, TransactionType type, BigDecimal amount, String category,
                            String note, LocalDate transactionDate) {
        this.user = user;
        this.account = account;
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.note = note;
        this.transactionDate = transactionDate;
    }

    public MoneyTransaction(AppUser user, TransactionType type, BigDecimal amount, String category,
                            String note, LocalDate transactionDate) {
        this(user, new MoneyAccount(user, "Cash", com.maahfuzdev.moneymanager.account.AccountType.CASH,
                BigDecimal.ZERO.setScale(2)), type, amount, category, note, transactionDate);
    }

    public void update(MoneyAccount account, TransactionType type, BigDecimal amount, String category, String note,
                       LocalDate transactionDate) {
        this.account = account;
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.note = note;
        this.transactionDate = transactionDate;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public MoneyAccount getAccount() { return account; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getCategory() { return category; }
    public String getNote() { return note; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public Instant getCreatedAt() { return createdAt; }
}
