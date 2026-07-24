package com.eventledger.account.dto;

import com.eventledger.account.domain.AccountTransaction;
import com.eventledger.account.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionView(
        String eventId,
        TransactionType type,
        BigDecimal amount,
        Instant eventTimestamp,
        Instant appliedAt
) {
    public static TransactionView from(AccountTransaction tx) {
        return new TransactionView(
                tx.getEventId(),
                tx.getType(),
                tx.getAmount(),
                tx.getEventTimestamp(),
                tx.getAppliedAt()
        );
    }
}
