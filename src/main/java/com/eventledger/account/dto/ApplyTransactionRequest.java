package com.eventledger.account.dto;

import com.eventledger.account.domain.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * S2S contract: the Gateway asks the Account Service to apply one transaction.
 * {@code accountId} is taken from the path, not the body.
 */
public record ApplyTransactionRequest(

        @NotBlank(message = "eventId is required")
        String eventId,

        @NotNull(message = "type is required and must be CREDIT or DEBIT")
        TransactionType type,

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "eventTimestamp is required")
        Instant eventTimestamp
) {
}
