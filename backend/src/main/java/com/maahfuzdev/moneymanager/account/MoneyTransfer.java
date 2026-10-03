package com.maahfuzdev.moneymanager.account;

import com.maahfuzdev.moneymanager.user.AppUser;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "money_transfers")
public class MoneyTransfer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_account_id", nullable = false)
    private MoneyAccount fromAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_account_id", nullable = false)
    private MoneyAccount toAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "transfer_date", nullable = false)
    private LocalDate transferDate;

    @Column(length = 500)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MoneyTransfer() { }

    public MoneyTransfer(AppUser user, MoneyAccount fromAccount, MoneyAccount toAccount,
                         BigDecimal amount, LocalDate transferDate, String note) {
        this.user = user;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.transferDate = transferDate;
        this.note = note;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public MoneyAccount getFromAccount() { return fromAccount; }
    public MoneyAccount getToAccount() { return toAccount; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getTransferDate() { return transferDate; }
    public String getNote() { return note; }
}
