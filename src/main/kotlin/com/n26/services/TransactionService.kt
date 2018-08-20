package com.n26.services

import com.n26.data.Stats
import com.n26.data.Transaction
import com.n26.services.TransactionService.Companion.BUCKETS_PER_SECOND
import com.n26.services.TransactionService.Companion.MAX_TIME
import com.n26.services.TransactionService.Companion.NANOS_PER_BUCKET
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * The business logic for the transaction controller (and statistics
 * controller since they work on the same objects).
 *
 * Since there is no database (and thus no DAOs), this service tracks the
 * transactions that have been added and generates the statistics for the
 * service.  It tracks the statistics by placing each transaction into a
 * bucket corresponding to a [NANOS_PER_BUCKET]-sized time range.  There are
 * a fixed number of buckets within the statistics window (exactly
 * [MAX_TIME]*[BUCKETS_PER_SECOND] buckets).  Each bucket tracks the max,
 * min, count, and sum of the transactions in the bucket.
 *
 * When the user asks for statistics within the [MAX_TIME]-time window, we will
 * aggregate the statistics for each bucket and then compute the average on
 * the result.  Because we expect there to be many transactions stored, this
 * size will dwarf the constant number of buckets we have to aggregate
 * statistics from.  We know that we can aggregate the statistics in O(1)
 * time.  Further, since there is [MAX_TIME]*[BUCKETS_PER_SECOND]-number of
 * buckets and each tracks the stats, we know we can store this information
 * in O(4*[MAX_TIME]*[BUCKETS_PER_SECOND]) = O(1) space.
 *
 * Because we are using buckets to categorize transactions according to their
 * time, there is going to be some inaccuracy in the computation of the stats.
 * However, we expect this to fall into the acceptable range.  Why?  There
 * are multiple sources of inaccuracy within this function (assuming a
 * standard definition of time units):
 *   * Request servicing time
 *   * Uncommitted updates
 *   * Possible clock drift among clients adding transactions
 *
 * Websites are considered immediately responsive if they can load in 0.1
 * seconds and seamless at 1 second.  We use a constant for the number of
 * buckets per second so we can measure the end-to-end time and adjust.
 * However, the request servicing time does make the start time of the window
 * ambiguous.  Should we use the instant when we get the request, or should we
 * assume some network latency and use a time in the past (since we don't
 * have a request time)?  A follow-up to this is: How out of date can the data
 * be by the time it reaches the caller and how much does clock drift impact
 * the accuracy of the transaction times?  Indeed, the system may need to wait
 * for the system to reach a consistent data state before it can access the
 * statistics.  As it stands, there is no response time constraint (requests
 * for them have been rejected) and the timestamps are potentially
 * inaccurate/inconsistent.  Thus, we will assume that there  is some leeway in
 * what is meant by "the last 60 seconds"; that a best-effort approach is
 * satisfactory.
 *
 * Beyond the statistics request servicing time, there is a possibility of
 * omitting pertinent, uncommmitted transactions from the statistics.  This
 * occurs when there os a high-volume of adds within the current time window.
 * It is also worth noting here that transactions could arrive out-of-order.
 * Because we want some responsiveness, we will ignore uncommmited
 * transactions and use the stats at the point when the system reaches a
 * stable state.
 *
 * @param buckets  the initial transaction set (a mapping from the instant
 *      identifying the bucket to the bucket itself).  This is provided as a
 *      constructor parameter only to aid in testing and should NOT be used for
 *      any other purpose.
 */
@Service
class TransactionService @JvmOverloads constructor(
        private val buckets: ConcurrentMap<Instant, TransactionContainer>
                = ConcurrentHashMap())
{
    // TODO -- change bucket map structure to have a clean-up option.

    companion object
    {
        const val NANOS_PER_SECOND = 1000000000

        const val BUCKETS_PER_SECOND = 2

        /**
         * The amount of time, in seconds, to include in each bucket -- must
         * be below a second.
         */
        const val NANOS_PER_BUCKET = (NANOS_PER_SECOND / BUCKETS_PER_SECOND)

        /**
         * The size of the statistics window, in seconds.
         */
        const val MAX_TIME = 60


//        @JvmStatic
//        fun getBucketNumber(instant: Instant): Long
//        {
//            // The bucket id is a unique combination of the seconds and
//            // nanoseconds.
//            return (instant.epochSecond % (MAX_TIME + 1))
//                    .shl(BUCKETS_PER_SECOND / 2) +
//                   (instant.nano / NANOS_PER_BUCKET)
//
//            //                val epochNanos = BigInteger.valueOf(instant.epochSecond)
//            //                        .multiply(BigInteger.valueOf(1000000000L))
//            //                        .add(BigInteger.valueOf(instant.nano.toLong()))
//        }


        /**
         * Gets the identifier of the bucket containing the specified timestamp.
         */
        @JvmStatic
        internal fun getBucketId(instant: Instant): Instant
        {
            val nanos = (instant.nano / NANOS_PER_BUCKET) * NANOS_PER_BUCKET
            return instant.minusNanos((instant.nano - nanos).toLong())
        }

        @JvmStatic
        internal fun bucketWindowSequence(startTime: Instant): Sequence<Instant>
                = (MAX_TIME * BUCKETS_PER_SECOND).downTo(0)
                    .asSequence()
                    .map { (NANOS_PER_SECOND * it).toLong() }
                    .map { getBucketId(startTime.minusNanos(it)) }
    }


    /**
     * Adds the transaction to the transaction storage/cache.
     *
     * @param transaction  the [Transaction] to add
     * @return  true if the transaction was within the statistics time
     *          window; false otherwise
     */
    fun add(transaction: Transaction): Boolean
    {
        val currentTime = Instant.now()

        val bucketId = when {
            transaction.timestamp > currentTime ->
                throw IllegalArgumentException("Invalid transaction time: the" +
                                               " transaction occurs in the " +
                                               "future")

            transaction.timestamp < currentTime.minusSeconds(MAX_TIME.toLong()) ->
                  Instant.EPOCH

            else -> getBucketId(transaction.timestamp)
        }

        val bucket = buckets.getOrPut(bucketId, ::TransactionContainer)
        bucket.add(transaction)

        return bucketId != Instant.EPOCH
    }

    fun getStats(): Stats
    {
        // Get range
        val startTime = Instant.now()

        // Retrieve stats for each bucket, combine them, then return
        // What if there are no transactions?
        return bucketWindowSequence(startTime)
                .mapNotNull { buckets[it]?.getContainerStats() }
                .takeIf { it.any() }
                ?.reduce(ContainerStats::plus)
                .let { Stats(min = it?.min,
                              max = it?.max,
                              sum = it?.sum,
                              avg = it?.avg,
                              count = it?.count ?: 0L) }
//                ?: Stats(null, null, null, null, 0L)
    }

    fun clear()
    {
        buckets.clear()
    }

    // For debug
    fun count(): Long
            = buckets.values.map { it.count }.fold(0, Long::plus)

//    private fun instantNow() = Instant.now()
}