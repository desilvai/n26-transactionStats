package com.n26.controllers;

import com.n26.data.Transaction;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping(path = "/transactions",
                produces = MediaType.APPLICATION_JSON_VALUE)
public class Transactions {

    @PostMapping
    public ResponseEntity<?> createTransaction(@RequestBody Transaction transaction)
    {
        // If the transaction is from the future, return an error.
        if(Instant.now().compareTo(transaction.getTimestamp()) < 0)
            new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }


    @DeleteMapping
    public ResponseEntity<?> deleteAllTransactions()
    {
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
