package com.eventledger.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A single applied transaction. The primary key is the originating {@code eventId},
 * which makes {@code apply} idempotent: re-applying the same event is a no-op because
 * the row already exists.
 */
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_tx_account", columnList = "account_id")
})
public class AccountTransaction {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private String eventId;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private TransactionType type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /** Business timestamp from the upstream system (may be out of order vs. arrival). */
    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    /** When this service applied the transaction. */
    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    protected AccountTransaction() {
    }

    public AccountTransaction(String eventId, String accountId, TransactionType type,
                              BigDecimal amount, Instant eventTimestamp, Instant appliedAt) {
        this.eventId = eventId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.eventTimestamp = eventTimestamp;
        this.appliedAt = appliedAt;
    }

    public String getEventId() {
        return eventId;
    }

    public String getAccountId() {
        return accountId;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    /** Signed contribution to the balance: +amount for CREDIT, -amount for DEBIT. */
    public BigDecimal signedAmount() {
        return type == TransactionType.CREDIT ? amount : amount.negate();
    }
}
