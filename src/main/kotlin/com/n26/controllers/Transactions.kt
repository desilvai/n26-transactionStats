package com.n26.controllers

import com.n26.data.Transaction
import com.n26.services.TransactionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller behind the '/transactions' webservice endpoint.
 */
@RestController
@RequestMapping(path = ["/transactions"],
                produces = [MediaType.APPLICATION_JSON_VALUE])
class Transactions
{
    @Autowired
    private lateinit var transactionService: TransactionService

    @PostMapping
    @Suppress("UNUSED")
    fun createTransaction(@RequestBody transaction: Transaction): ResponseEntity<*>
    {
        val status: HttpStatus
        try
        {
            status = if (transactionService.add(transaction)) HttpStatus.CREATED
                     else HttpStatus.NO_CONTENT
        }
        catch (e: IllegalArgumentException)
        {
            // If the transaction is from the future or is otherwise
            // malformed (and it hasn't been detected already), return an error.
            return ResponseEntity<Any>(HttpStatus.UNPROCESSABLE_ENTITY)
        }

        return ResponseEntity<Any>(status)
    }


    @DeleteMapping
    @Suppress("UNUSED")
    fun deleteAllTransactions(): ResponseEntity<*>
    {
        transactionService.clear()
        return ResponseEntity<Any>(HttpStatus.NO_CONTENT)
    }


    // Needed for testing
    @GetMapping
    @Suppress("UNUSED")
    fun countTransactions(): ResponseEntity<*>
    {
        return ResponseEntity(transactionService.count(), HttpStatus.OK)
    }
}
