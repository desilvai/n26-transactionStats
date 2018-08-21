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

//    private var min: BigDecimal? = null
//    private var max: BigDecimal? = null
//    private var sum: BigDecimal = BigDecimal.ZERO.setScale(8, HALF_UP)
//
//    val count: Long
//        @Synchronized get() = transactions.count().toLong()

    val count: Long
        get() = containerStats?.count ?: 0

    val values: List<Transaction>
        @Synchronized get() = transactions.toList()

    fun add(transaction: Transaction) {
        synchronized(this) {
            transactions.add(transaction)

            containerStats = ContainerStats(transaction.amount,
                                            transaction.amount,
                                            transaction.amount,
                                            1)
                                    .plus(containerStats)
//            min = min?.min(transaction.amount) ?: transaction.amount
//            max = max?.max(transaction.amount) ?: transaction.amount
//            sum = sum.add(transaction.amount)
        }
    }

//    /**
//     * Gets all of the container statistics at once.  This ensures that they
//     * are consistent (that no update occurs between accessing them).
//     *
//     * @return  the container statistics if the container is non-empty or
//     *          null if the container is empty.
//     */
//    fun getContainerStats(): ContainerStats?
//    {
////        return synchronized(this) {
//            return containerStats
////            if(min == null)
////                null
////            else
////                ContainerStats(min!!, max!!, sum, transactions.count().toLong())
////        }
//    }
}