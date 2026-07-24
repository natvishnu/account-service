package com.eventledger.account.web;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<FieldViolation> violations
) {
    public record FieldViolation(String field, String message) {
    }
}
