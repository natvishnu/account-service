package com.eventledger.account.repository;

import com.eventledger.account.domain.AccountTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, String> {

    /** Recent transactions for an account, most recent business timestamp first. */
    List<AccountTransaction> findTop20ByAccountIdOrderByEventTimestampDesc(String accountId);

    long countByAccountId(String accountId);
}
