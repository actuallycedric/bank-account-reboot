package com.clarence.bank_api.service;

import com.clarence.bank_api.dto.*;
import com.clarence.bank_api.exception.AccountNotFoundException;
import com.clarence.bank_api.exception.AccountViolationException;
import com.clarence.bank_api.model.Account;
import com.clarence.bank_api.model.Transaction;
import com.clarence.bank_api.model.TransactionType;
import com.clarence.bank_api.repository.AccountRepository;
import com.clarence.bank_api.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    public TransactionResponse parseToTransactionResponse(Transaction t){
        TransactionResponse responseObject = new TransactionResponse();

        responseObject.setAmount(t.getAmount());
        responseObject.setDescription(t.getDescription());
        responseObject.setId(t.getId());
        responseObject.setTime(t.getTime());
        responseObject.setType(t.getType());

        return responseObject;
    }

    public ResponseEntity<AccountResponse> findById(int id){
        Optional<Account> a = accountRepository.findById(id);

        if(a.isEmpty()) throw new AccountNotFoundException("Cannot find an account with id " + id + "!");

        AccountResponse response = parseToResponseBody(a.get());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<AccountResponse> createAccount(CreateAccountRequest req){
        Account a = new Account();

        a.setBalance(req.getInitialDeposit());
        a.setFirstName(req.getFirstName());
        a.setLastName(req.getLastName());

        accountRepository.save(a);

        return new ResponseEntity<>(parseToResponseBody(a), HttpStatus.CREATED);
    }

    @Transactional
    public ResponseEntity<String> closeAccount(int id){
        Optional<Account> accountWrapper = accountRepository.findById(id);
        if(accountWrapper.isEmpty()) throw new AccountNotFoundException("Cannot find an account with id " + id + "!");

        // handle exception
        Account a = accountWrapper.get();
        if(!a.getBalance().equals(new BigDecimal("0.00"))) throw new AccountViolationException("You cannot close an account with an outstanding balance/deficit!");

        accountRepository.delete(a);

        return new ResponseEntity<>("", HttpStatus.NO_CONTENT);
    }

    public PaginationResponse<TransactionResponse> getAllTransactionsById(int id, int page, int size){

        Optional<Account> key = accountRepository.findById(id);
        if(key.isEmpty()) throw new AccountNotFoundException("Cannot find an account with id " + id + "!");
        Account a = key.get();

        Sort sort = Sort.by("time").ascending();
        Pageable pageReq = PageRequest.of(page, size, sort);

        Page<TransactionResponse> paginatedResult = transactionRepository.findByAccountId(a, pageReq).map(this::parseToTransactionResponse);

        PaginationResponse<TransactionResponse> p = new PaginationResponse<>();
        p.setCurrentPage(page);
        p.setLeftoverPages(paginatedResult.getTotalPages()-1);
        p.setTotalElements(paginatedResult.getTotalElements());
        p.setContent(paginatedResult.stream().collect(Collectors.toList()));
        p.setHasNext(paginatedResult.hasNext());

        return p;
    }

    public PaginationResponse<AccountResponse> findAll(int page, int size){
        List<Account> list = accountRepository.findAll();

        Sort sort = Sort.by("id").ascending();

        Pageable pageReq = PageRequest.of(page, size, sort);

        Page<AccountResponse> paginatedResult = accountRepository.findAll(pageReq).map(this::parseToResponseBody);

        PaginationResponse<AccountResponse> p = new PaginationResponse<>();
        p.setCurrentPage(page);
        p.setLeftoverPages(paginatedResult.getTotalPages()-1);
        p.setTotalElements(paginatedResult.getTotalElements());
        p.setContent(paginatedResult.stream().collect(Collectors.toList()));
        p.setHasNext(paginatedResult.hasNext());

        return p;
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

        if(a.getBalance().compareTo(amountToWithdraw) < 0) throw new AccountViolationException("You can't withdraw more than your balance!");

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

        if(req.getRecipientAccount() == senderId) throw new AccountViolationException("Your sender id must be different from the recipient id!");

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
