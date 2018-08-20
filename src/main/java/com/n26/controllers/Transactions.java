package com.n26.controllers;

import com.n26.data.Transaction;
import com.n26.services.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/transactions",
                produces = MediaType.APPLICATION_JSON_VALUE)
public class Transactions {
    @Autowired
    private TransactionService transactionService;

    @PostMapping
    public ResponseEntity<?> createTransaction(@RequestBody Transaction transaction)
    {
        HttpStatus status;
        try
        {
            status = transactionService.add(transaction) ? HttpStatus.CREATED :
                                                        HttpStatus.NO_CONTENT;
        }
        catch (IllegalArgumentException e)
        {
            // If the transaction is from the future or is otherwise
            // malformed (and it hasn't been detected already), return an error.
            return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
        }

        return new ResponseEntity<>(status);
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
