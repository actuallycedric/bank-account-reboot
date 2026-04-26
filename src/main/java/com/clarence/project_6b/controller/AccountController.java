package com.clarence.project_6b.controller;

import com.clarence.project_6b.dto.AccountResponse;
import com.clarence.project_6b.dto.CreateAccountRequest;
import com.clarence.project_6b.model.Account;
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
}
