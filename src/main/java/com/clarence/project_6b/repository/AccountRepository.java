package com.clarence.project_6b.repository;

import com.clarence.project_6b.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Integer> {
}
