package com.eventledger.account.service;

import com.eventledger.account.domain.Account;
import com.eventledger.account.domain.AccountTransaction;
import com.eventledger.account.dto.AccountDetailsResponse;
import com.eventledger.account.dto.ApplyTransactionRequest;
import com.eventledger.account.dto.ApplyTransactionResponse;
import com.eventledger.account.dto.BalanceResponse;
import com.eventledger.account.dto.TransactionView;
import com.eventledger.account.exception.AccountNotFoundException;
import com.eventledger.account.repository.AccountRepository;
import com.eventledger.account.repository.AccountTransactionRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accounts;
    private final AccountTransactionRepository transactions;
    private final MeterRegistry meters;
    private final Clock clock;

    public AccountService(AccountRepository accounts,
                          AccountTransactionRepository transactions,
                          MeterRegistry meters,
                          Clock clock) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.meters = meters;
        this.clock = clock;
    }

    /**
     * Applies a transaction to an account, creating the account on first use.
     *
     * <p>Idempotent on {@code eventId}: if the event was already applied, the balance is
     * left untouched and the response is flagged {@code duplicate=true}. Balance is a
     * commutative sum of signed amounts, so out-of-order arrival never changes the result.
     */
    @Transactional
    public ApplyTransactionResponse apply(String accountId, ApplyTransactionRequest req) {
        // Idempotency guard: the eventId is the transaction PK.
        if (transactions.existsById(req.eventId())) {
            Account existing = accounts.findById(accountId)
                    .orElseThrow(() -> new AccountNotFoundException(accountId));
            log.info("Duplicate transaction ignored: eventId={} account={} balance={}",
                    req.eventId(), accountId, existing.getBalance());
            countApply(req, true);
            return new ApplyTransactionResponse(accountId, req.eventId(), existing.getBalance(), true);
        }

        Instant now = clock.instant();
        Account account = accounts.findById(accountId)
                .orElseGet(() -> new Account(accountId, now));

        AccountTransaction tx = new AccountTransaction(
                req.eventId(), accountId, req.type(), req.amount(), req.eventTimestamp(), now);

        account.setBalance(account.getBalance().add(tx.signedAmount()));
        account.setUpdatedAt(now);

        try {
            accounts.save(account);
            transactions.save(tx);
        } catch (DataIntegrityViolationException race) {
            // Concurrent apply of the same eventId won the race; treat as duplicate.
            Account current = accounts.findById(accountId)
                    .orElseThrow(() -> new AccountNotFoundException(accountId));
            log.info("Concurrent duplicate transaction ignored: eventId={} account={}",
                    req.eventId(), accountId);
            countApply(req, true);
            return new ApplyTransactionResponse(accountId, req.eventId(), current.getBalance(), true);
        }

        log.info("Applied transaction: eventId={} account={} type={} amount={} newBalance={}",
                req.eventId(), accountId, req.type(), req.amount(), account.getBalance());
        countApply(req, false);
        return new ApplyTransactionResponse(accountId, req.eventId(), account.getBalance(), false);
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String accountId) {
        Account account = accounts.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        return new BalanceResponse(accountId, account.getBalance());
    }

    @Transactional(readOnly = true)
    public AccountDetailsResponse getDetails(String accountId) {
        Account account = accounts.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        List<TransactionView> recent = transactions
                .findTop20ByAccountIdOrderByEventTimestampDesc(accountId)
                .stream()
                .map(TransactionView::from)
                .toList();
        return new AccountDetailsResponse(
                account.getAccountId(),
                account.getBalance(),
                transactions.countByAccountId(accountId),
                account.getCreatedAt(),
                account.getUpdatedAt(),
                recent);
    }

    private void countApply(ApplyTransactionRequest req, boolean duplicate) {
        Counter.builder("account.transactions.applied")
                .description("Transactions received by the account service")
                .tag("type", req.type().name())
                .tag("duplicate", Boolean.toString(duplicate))
                .register(meters)
                .increment();
    }
}
