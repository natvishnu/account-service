package com.eventledger.account.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AccountDetailsResponse(
        String accountId,
        BigDecimal balance,
        long transactionCount,
        Instant createdAt,
        Instant updatedAt,
        List<TransactionView> recentTransactions
) {
}
