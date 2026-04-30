package com.clarence.bank_api.repository;

import com.clarence.bank_api.model.Account;
import com.clarence.bank_api.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    List<Transaction> findByAccountId(Account a);
}
