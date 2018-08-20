package com.n26.controllers;

import com.n26.data.Transaction;
import com.n26.services.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping(path = "/transactions",
                produces = MediaType.APPLICATION_JSON_VALUE)
public class Transactions {
    @Autowired
    private TransactionService transactionService;

    @PostMapping
    public ResponseEntity<?> createTransaction(@RequestBody Transaction transaction)
    {
        // If the transaction is from the future, return an error.
        if(Instant.now().compareTo(transaction.getTimestamp()) < 0)
            return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);

        // TODO -- change return code if transaction over 60s out.
        transactionService.add(transaction);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }


    @DeleteMapping
    public ResponseEntity<?> deleteAllTransactions()
    {
        transactionService.clear();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // Needed for testing
    @GetMapping
    public ResponseEntity<?> countTransactions()
    {
        return new ResponseEntity<>(transactionService.count(), HttpStatus.OK);
    }
}
