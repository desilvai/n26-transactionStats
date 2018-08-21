package com.n26.services

import com.n26.data.Transaction

class TransactionContainer
{
    private val transactions: MutableList<Transaction> = mutableListOf()

    /**
     * Contains all of the container statistics in one object.  This ensures
     * that they are consistent (that no update occurs while another thread
     * is midway through getting all of the stats).
     */
    @Volatile
    var containerStats: ContainerStats? = null
        private set


    val count: Long
        get() = containerStats?.count ?: 0


    val values: List<Transaction>
        @Synchronized get() = transactions.toList()


    /**
     * Adds a transaction to the set of transactions in the container and
     * updates the statistics.
     */
    fun add(transaction: Transaction)
    {
        synchronized(this) {
            transactions.add(transaction)

            containerStats = ContainerStats(transaction.amount,
                                            transaction.amount,
                                            transaction.amount,
                                            1)
                                    .plus(containerStats)

            // The stats and the container's contents should always match.
            assert(containerStats!!.count == transactions.count().toLong())
        }
    }


    fun addAll(other: TransactionContainer)
    {
        val (otherValues, otherStats) = other.valuesAndStats()

        synchronized(this) {
            transactions.addAll(otherValues)

            containerStats = otherStats?.plus(containerStats) ?: containerStats

            // The stats and the container's contents should always match.
            assert(containerStats == null ||
                   containerStats?.count == transactions.count().toLong())
        }
    }

    @Synchronized
    private fun valuesAndStats(): Pair<List<Transaction>, ContainerStats?>
            = Pair(values, containerStats)

}