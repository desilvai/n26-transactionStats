package com.n26.services

import com.n26.data.Transaction
import java.math.BigDecimal

data class ContainerStats(val min: BigDecimal,
                          val max: BigDecimal,
                          val sum: BigDecimal,
                          val count: Long)
{
    val avg: BigDecimal
        get() = sum.divide(BigDecimal(count))

    infix operator fun plus(other: ContainerStats)
           = ContainerStats(min.min(other.min),
                            max.max(other.max),
                            sum + other.sum,
                            count + other.count)
}

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