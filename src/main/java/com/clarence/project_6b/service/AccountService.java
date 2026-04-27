package com.clarence.project_6b.service;

import com.clarence.project_6b.dto.*;
import com.clarence.project_6b.exception.AccountNotFoundException;
import com.clarence.project_6b.exception.AccountViolationException;
import com.clarence.project_6b.model.Account;
import com.clarence.project_6b.model.Transaction;
import com.clarence.project_6b.model.TransactionType;
import com.clarence.project_6b.repository.AccountRepository;
import com.clarence.project_6b.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;

    @Autowired
    public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository){
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public AccountResponse parseToResponseBody(Account a){
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

        AccountResponse response = parseToResponseBody(a.get());
        return response;
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest req){
        Account a = new Account();

        a.setBalance(req.getInitialDeposit());
        a.setFirstName(req.getFirstName());
        a.setLastName(req.getLastName());

        accountRepository.save(a);

        return parseToResponseBody(a);
    }

    @Transactional
    public String closeAccount(int id){
        Optional<Account> accountWrapper = accountRepository.findById(id);
        if(accountWrapper.isEmpty()) throw new AccountNotFoundException("Cannot find an account with id " + id + "!");

        // handle exception
        Account a = accountWrapper.get();
        if(!a.getBalance().equals(new BigDecimal("0.00"))) throw new AccountViolationException("You cannot close an account with an outstanding balance/deficit!");

        accountRepository.delete(a);

        return "The account has been closed. Sorry to see you go!";
    }


    public List<TransactionResponse> getAllTransactionsById(int id){

        Optional<Account> key = accountRepository.findById(id);
        if(key.isEmpty()) throw new AccountNotFoundException("Cannot find an account with id " + id + "!");


        List<Transaction> resource = transactionRepository.findByAccountId(key.get());

        List<TransactionResponse> response = new ArrayList<>();

        resource.forEach(t -> {
            TransactionResponse responseObject = new TransactionResponse();

            responseObject.setAmount(t.getAmount());
            responseObject.setDescription(t.getDescription());
            responseObject.setId(t.getId());
            responseObject.setTime(t.getTime());
            responseObject.setType(t.getType());

            response.add(responseObject);
        });

        return response;
    }

    @Transactional
    public AccountResponse deposit(BalanceChangeRequest req, int id){

        Optional<Account> accountWrapper = accountRepository.findById(id);
        if(accountWrapper.isEmpty()) throw new AccountNotFoundException("Cannot find an account with id " + id + "!");


        // update account balance
        Account a = accountWrapper.get();
        BigDecimal addend = req.getAmount();
        BigDecimal amount = a.getBalance().add(addend);
        a.setBalance(amount);


        // log the transaction
        Transaction t = new Transaction();
        t.setType(TransactionType.DEPOSIT);
        t.setAmount(addend);
        t.setTime(LocalDateTime.now());
        t.setDescription(req.getDescription());
        t.setAccount(a);

        // merge changes
        accountRepository.save(a);
        transactionRepository.save(t);

        return parseToResponseBody(a);
    }

    @Transactional
    public AccountResponse withdraw(BalanceChangeRequest req, int id){

        Optional<Account> accountWrapper = accountRepository.findById(id);
        if(accountWrapper.isEmpty()) throw new AccountNotFoundException("Cannot find an account with id " + id + "!");


        // update account balance
        Account a = accountWrapper.get();
        BigDecimal amountToWithdraw = req.getAmount();

        if(a.getBalance().compareTo(amountToWithdraw) < 0) throw new AccountViolationException("You can't withdraw an amount more than your balance!");

        BigDecimal amount = a.getBalance().subtract(amountToWithdraw);
        a.setBalance(amount);


        // log the transaction
        Transaction t = new Transaction();
        t.setType(TransactionType.WITHDRAW);
        t.setAmount(amountToWithdraw);
        t.setTime(LocalDateTime.now());
        t.setDescription(req.getDescription());
        t.setAccount(a);

        // merge changes
        accountRepository.save(a);
        transactionRepository.save(t);

        return parseToResponseBody(a);
    }

    @Transactional
    public List<AccountResponse> transfer(TransferRequest req, int senderId){
        
        Optional<Account> accountWrapper = accountRepository.findById(senderId);
        Optional<Account> recipientAccountWrapper = accountRepository.findById(req.getRecipientAccount());
        
        if(accountWrapper.isEmpty()) throw new AccountNotFoundException("Cannot find an account with id " + senderId + "!");
        if(recipientAccountWrapper.isEmpty()) throw new AccountNotFoundException("Cannot find an account with id " + req.getRecipientAccount() + "!");

        Account sender = accountWrapper.get();
        Account receiver = recipientAccountWrapper.get();


        // adjust balances
        BigDecimal amountTransferred = req.getAmount();


        BigDecimal senderBalance = sender.getBalance().subtract(amountTransferred);

        if(senderBalance.compareTo(BigDecimal.ZERO) < 0) throw new AccountViolationException("You can't withdraw more than your balance!");

        sender.setBalance(senderBalance);

        BigDecimal receiverBalance = receiver.getBalance().add(amountTransferred);

        receiver.setBalance(receiverBalance);

        // log transaction
        Transaction t = new Transaction();
        t.setType(TransactionType.TRANSFER);
        t.setAmount(amountTransferred);
        t.setTime(LocalDateTime.now());
        t.setDescription(req.getDescription());
        t.setAccount(sender);

        // merge changes
        accountRepository.save(sender);
        accountRepository.save(receiver);
        transactionRepository.save(t);

        return List.of(parseToResponseBody(sender), parseToResponseBody(receiver));

    }


}
