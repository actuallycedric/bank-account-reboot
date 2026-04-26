package com.clarence.project_6b.controller;

import com.clarence.project_6b.dto.*;
import com.clarence.project_6b.model.Account;
import com.clarence.project_6b.model.Transaction;
import com.clarence.project_6b.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AccountController {

    private AccountService accountService;

    @Autowired
    public AccountController(AccountService accountService){
        this.accountService = accountService;
    }

    @GetMapping("/")
    public String hello(){
        return "Hello!";
    }

    @GetMapping("/accounts")
    public List<Account> findAllAccounts(){
        return accountService.findAll();
    }

    @GetMapping("/accounts/{id}")
    public AccountResponse findById(@PathVariable int id){
        return accountService.findById(id);
    }

    @PostMapping("/accounts")
    public AccountResponse createAccount(@RequestBody CreateAccountRequest req){
        return accountService.createAccount(req);
    }

    @DeleteMapping("/accounts/{id}")
    public String closeAccount(@PathVariable int id){
        return accountService.closeAccount(id);
    }

    @GetMapping("/accounts/{id}/transactions")
    public List<TransactionResponse> getAllTransactionsById(@PathVariable int id){
        return accountService.getAllTransactionsById(id);
    }

    @PostMapping("/accounts/{id}/deposit")
    public AccountResponse deposit(@PathVariable int id, @RequestBody BalanceChangeRequest req){
        return accountService.deposit(req, id);
    }

    @PostMapping("/accounts/{id}/withdraw")
    public AccountResponse withdraw(@PathVariable int id, @RequestBody BalanceChangeRequest req){
        return accountService.withdraw(req, id);
    }

    @PostMapping("/accounts/{senderId}/transfer")
    public List<AccountResponse> transfer(@PathVariable int senderId, @RequestBody TransferRequest req){
        return accountService.transfer(req, senderId);
    }


}
