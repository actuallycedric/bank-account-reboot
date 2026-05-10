package com.clarence.bank_api.repository;

import com.clarence.bank_api.model.Account;
import com.clarence.bank_api.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    Page<Transaction> findByAccountId(Account a, Pageable p);
}
