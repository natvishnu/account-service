package com.eventledger.account.web;

import com.eventledger.account.dto.AccountDetailsResponse;
import com.eventledger.account.dto.ApplyTransactionRequest;
import com.eventledger.account.dto.ApplyTransactionResponse;
import com.eventledger.account.dto.BalanceResponse;
import com.eventledger.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/{accountId}/transactions")
    public ResponseEntity<ApplyTransactionResponse> apply(
            @PathVariable String accountId,
            @Valid @RequestBody ApplyTransactionRequest request) {
        ApplyTransactionResponse response = accountService.apply(accountId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountId}/balance")
    public BalanceResponse balance(@PathVariable String accountId) {
        return accountService.getBalance(accountId);
    }

    @GetMapping("/{accountId}")
    public AccountDetailsResponse details(@PathVariable String accountId) {
        return accountService.getDetails(accountId);
    }
}
