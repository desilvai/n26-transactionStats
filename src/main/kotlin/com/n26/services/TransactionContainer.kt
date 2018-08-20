package com.n26.services

import com.n26.data.Transaction
import java.math.BigDecimal

class TransactionContainer
{
    private val transactions: MutableList<Transaction> = mutableListOf()

    /**
     * Contains the minimum value for the container.  You MUST lock prior to
     * accessing
     */
    private var min: BigDecimal? = null
    private var max: BigDecimal? = null
    private var sum: BigDecimal = BigDecimal.ZERO

    val count: Long
        @Synchronized get() = transactions.count().toLong()

    val values: List<Transaction>
        @Synchronized get() = transactions.toList()

    fun add(transaction: Transaction) {
        synchronized(this) {
            transactions.add(transaction)

            min = min?.min(transaction.amount) ?: transaction.amount
            max = max?.max(transaction.amount) ?: transaction.amount
            sum = sum.add(transaction.amount)
        }
    }

    fun getContainerStats(): ContainerStats?
    {
        return synchronized(this) {
            if(min == null)
                null
            else
                ContainerStats(min!!, max!!, sum, transactions.count().toLong())
        }
    }
}