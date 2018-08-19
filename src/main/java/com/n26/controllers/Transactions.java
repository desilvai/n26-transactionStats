package com.n26.controllers;

import com.n26.data.Transaction;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/transactions",
                produces = MediaType.APPLICATION_JSON_VALUE)
public class Transactions {

    @PostMapping
    public ResponseEntity<?> createTransaction(Transaction transaction)
    {
        // TODO -- Make this an empty body.
        return new ResponseEntity<Transaction>(transaction, HttpStatus.CREATED);
    }


    @DeleteMapping
    public ResponseEntity<?> deleteAllTransactions()
    {
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
