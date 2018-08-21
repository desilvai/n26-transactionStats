package com.n26.controllers

import com.n26.services.TransactionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/statistics"],
                produces = [MediaType.APPLICATION_JSON_VALUE])
class StatisticsController
{
    @Autowired
    private lateinit var transactionService: TransactionService


    /**
     * GET /statistics
     *
     * This endpoint returns the statistics based on the transactions that
     * happened in the last 60 seconds. It executes in constant time and
     * memory (O(1)).
     *
     * Example Returns:
     *     {
     *         "sum": "1000.00",
     *         "avg": "100.53",
     *         "max": "200000.49",
     *         "min": "50.23",
     *         "count": 10
     *     }
     *
     * Where:
     *   * sum – a [BigDecimal] specifying the total sum of transaction value
     *           in the last 60 seconds
     *   * avg – a [BigDecimal] specifying the average amount of transaction
     *           value in the last 60 seconds
     *   * max – a [BigDecimal] specifying single highest transaction value in
     *           the last 60 seconds
     *   * min – a [BigDecimal] specifying single lowest transaction value in
     *           the last 60 seconds
     *   * count – a [Long] specifying the total number of transactions that
     *           happened in the last 60 seconds
     *
     * All BigDecimal values always contain exactly two decimal places and
     * use `HALF_ROUND_UP` rounding. eg: 10.345 is returned as 10.35 10.8 is
     * returned as 10.80
     *
     * @return  the response entity with status code 200 and the stats
     *          represented as a [Stats] object.
     */
    @GetMapping
    @Suppress("UNUSED")
    fun getStatistics(): ResponseEntity<*>
         = ResponseEntity(transactionService.getStats(), HttpStatus.OK)
}