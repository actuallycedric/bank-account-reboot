package com.clarence.project_6b.repository;

import com.clarence.project_6b.model.Account;
import com.clarence.project_6b.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    List<Transaction> findByAccountId(Account a);
}
