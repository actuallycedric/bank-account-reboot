package com.clarence.project_6b.service;

import com.clarence.project_6b.dto.AccountResponse;
import com.clarence.project_6b.dto.CreateAccountRequest;
import com.clarence.project_6b.exception.AccountNotFoundException;
import com.clarence.project_6b.model.Account;
import com.clarence.project_6b.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    private AccountRepository accountRepository;

    @Autowired
    public AccountService(AccountRepository accountRepository){
        this.accountRepository = accountRepository;
    }

    public AccountResponse parse(Account a){
        AccountResponse response = new AccountResponse();
        response.setId(a.getId());
        response.setBalance(a.getBalance());
        response.setFirstName(a.getFirstName());
        response.setLastName(a.getLastName());

        return response;
    }

    public List<Account> findAll(){
        return accountRepository.findAll();
    }

    public AccountResponse findById(int id){
        Optional<Account> a = accountRepository.findById(id);

        if(a.isEmpty()) throw new AccountNotFoundException("Cannot find an account with id " + id + "!");

        AccountResponse response = parse(a.get());
        return response;
    }

    public AccountResponse createAccount(CreateAccountRequest req){
        Account a = new Account();


        a.setBalance(req.getInitialDeposit());
        a.setFirstName(req.getFirstName());
        a.setLastName(req.getLastName());

        accountRepository.save(a);

        return parse(a);
    }


}
