package com.maahfuzdev.moneymanager.account;

import com.maahfuzdev.moneymanager.user.AppUser;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "money_accounts")
public class MoneyAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType type;

    @Column(name = "opening_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal openingBalance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MoneyAccount() { }

    public MoneyAccount(AppUser user, String name, AccountType type, BigDecimal openingBalance) {
        this.user = user;
        this.name = name;
        this.type = type;
        this.openingBalance = openingBalance;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public String getName() { return name; }
    public AccountType getType() { return type; }
    public BigDecimal getOpeningBalance() { return openingBalance; }
}
