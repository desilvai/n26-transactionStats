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

    /**
     * POST /transactions
     *
     * This endpoint is called to create a new transaction.
     *
     * Body:
     *     {
     *         "amount": "12.3343",
     *         "timestamp": "2018-07-17T09:59:51.312Z"
     *     }
     *
     * Where:
     *    * amount – transaction amount; a string of arbitrary length that is
     *          parsable as a BigDecimal
     *    * timestamp – transaction time in the ISO 8601 format
     *          YYYY-MM-DDThh:mm:ss.sssZ in the UTC timezone (this is not the
     *          current timestamp)
     *
     * @param transaction  the [Transaction] plain-old Kotlin object (POKO)
     *              created by Jackson to represent the contents of the request
     *              body.
     * @return  An empty body with one of the following:
     *     * 201 – in case of success
     *     * 204 – if the transaction is older than 60 seconds
     *     * 400 – if the JSON is invalid
     *     * 422 – if any of the fields are not parsable or the transaction
     *             date is in the future
     */
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


    /**
     * DELETE /transactions
     *
     * This endpoint causes all existing transactions to be deleted
     *
     * @return a 204 status code.
     */
    @DeleteMapping
    @Suppress("UNUSED")
    fun deleteAllTransactions(): ResponseEntity<*>
    {
        transactionService.clear()
        return ResponseEntity<Any>(HttpStatus.NO_CONTENT)
    }


    /**
     * GET  /transactions
     *
     * This endpoint gets the count of all transactions in the
     * "database"/cache.  It is provided solely for testing and therefore
     * does not return a JSON object.  Instead it just returns the count in
     * plain text.
     */
    // Needed for testing -- see if we can deprecate this.
    @GetMapping
    @Suppress("UNUSED")
    fun countTransactions(): ResponseEntity<*>
    {
        return ResponseEntity(transactionService.count(), HttpStatus.OK)
    }
}
