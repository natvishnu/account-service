package com.eventledger.account.dto;

import java.math.BigDecimal;

/**
 * Result of applying a transaction.
 *
 * @param duplicate true when the eventId had already been applied (no balance change occurred)
 */
public record ApplyTransactionResponse(
        String accountId,
        String eventId,
        BigDecimal balance,
        boolean duplicate
) {
}
