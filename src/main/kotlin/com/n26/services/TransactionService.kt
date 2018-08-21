package com.n26.services

import com.n26.data.Stats
import com.n26.data.Transaction
import com.n26.services.TransactionService.Companion.BUCKETS_PER_SECOND
import com.n26.services.TransactionService.Companion.MAX_TIME
import com.n26.services.TransactionService.Companion.MILLIS_PER_BUCKET
import org.springframework.stereotype.Service
import java.math.BigDecimal
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
 * bucket corresponding to a [MILLIS_PER_BUCKET]-sized time range.  There are
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
 * However, this function is inherently inaccurate due to ambiguity over the
 * ending time of "the last 60 seconds" time period.  This period could run
 * from the time the client submitted the request, the time the server received
 * the request, or the time the server services the request.  Further, the
 * transaction times could be off due to challenges synchronizing clocks and
 * clock drift (we assume all transactions are standardized around Java's
 * definition of time primitives).  Due to all of these sources of inaccuracy,
 * we believe the additional inaccuracy from a bucket-categorization of
 * transactions is acceptable.  If additional accuracy is needed, the program
 * can be updated (by increasing the [BUCKETS_PER_SECOND] constant) to bring
 * about the desired improvements.
 *
 * An additional source of inaccuracy codes from the possibility of omitting
 * pertinent, uncommitted transactions from the statistics.  This occurs when
 * there is a high-volume of adds within the current time window.  It is also
 * worth noting here that transactions could arrive out-of-order.  In deference
 * to responsiveness, we will ignore uncommmited transactions and use the stats
 * at the point when the system reaches a stable state.
 *
 * @param buckets  the initial transaction set (a mapping from the instant
 *      identifying the bucket to the bucket itself).  We use a mapping here
 *      because adds are not ordered, and looking up the right transaction
 *      bucket is faster with a hash table (which will also minimize the
 *      amount of time spent with the table locked).  Also, note that this is
 *      provided as a constructor parameter only to aid in testing and should
 *      NOT be used for any other purpose.
 */
@Service
class TransactionService @JvmOverloads constructor(
        private val buckets: ConcurrentMap<Long, TransactionContainer>
                = ConcurrentHashMap())
{
    companion object
    {
        private const val MILLIS_PER_SECOND = 1000

        /**
         * The number of buckets to have in each second.  Make sure this is a
         * divisor 1000.
         *
         * Increasing this number (and the resultant number of buckets) makes
         * this more accurate, but slower.  Ideally, we'd look at our
         * inaccuracy tolerance and set this accordingly.
         */
        const val BUCKETS_PER_SECOND = 2

        /**
         * The amount of time, in seconds, to include in each bucket -- must
         * be below a second.
         */
        const val MILLIS_PER_BUCKET = MILLIS_PER_SECOND / BUCKETS_PER_SECOND


        /**
         * The size of the statistics window, in seconds.
         */
        const val MAX_TIME = 60

        /**
         * The size of the bucket list that triggers clean-up.
         */
        const val TRIGGER_CLEANUP_AT_SIZE = 2 * MAX_TIME * BUCKETS_PER_SECOND

        /**
         * Defines the bucket to use if the transaction is outside of the
         * [MAX_TIME] second statistics window.  This value is equal to
         * [Instant.EPOCH.toEpochMilli()].
         */
        const val DEFAULT_BUCKET = 0L


        init
        {
            // Make sure I don't do something dumb later.  This will only be
            // picked-up while assertions are on.
            assert(MILLIS_PER_SECOND % BUCKETS_PER_SECOND == 0) {
                "Invalid set-up.  The number of buckets must evenly divide " +
                "1000 (the number of milliseconds in a second)."
            }
        }


        /**
         * Gets the identifier of the bucket containing the specified timestamp.
         */
        @JvmStatic
        internal fun getBucketId(instant: Instant): Long
                = (instant.toEpochMilli() / MILLIS_PER_BUCKET) * MILLIS_PER_BUCKET


        /**
         * Gets a sequence of all bucket ids (descending) within the stats
         * window (the last [MAX_TIME] seconds).
         */
        @JvmStatic
        internal fun bucketWindowSequence(startTime: Instant): Sequence<Long>
                = (MAX_TIME * BUCKETS_PER_SECOND - 1).downTo(0)
                    .asSequence()
                    .map { (MILLIS_PER_BUCKET * it).toLong() }
                    .map { getBucketId(startTime.minusMillis(it)) }
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
                DEFAULT_BUCKET

            else -> getBucketId(transaction.timestamp)
        }

        val bucket = buckets.getOrPut(bucketId, ::TransactionContainer)
        bucket.add(transaction)

        // Now, trigger clean-up if the number of buckets has grown beyond
        // double the number needed.  This should be a rare call.
        // I'm not certain that this is needed for the assignment, but it
        // seems like it is required to get the data to a consistent state.
        // We'd probably do something different if we had a DB.
        if(buckets.count() > TRIGGER_CLEANUP_AT_SIZE)
            cleanUp()

        return bucketId != DEFAULT_BUCKET
    }


    /**
     * Gets the stats for the last [MAX_TIME] seconds.  If there have been no
     * transactions in the last [MAX_TIME] seconds, all stats will be 0.
     */
    fun getStats(): Stats
    {
        // Get range
        val startTime = Instant.now()


        // Retrieve stats for each bucket, combine them, then return
        // What if there are no transactions?
        return bucketWindowSequence(startTime)
                .mapNotNull { buckets[it]?.containerStats }
                .takeIf { it.any() }
                ?.reduce(ContainerStats::plus)
                .let { Stats(min = it?.min ?: BigDecimal.ZERO,
                             max = it?.max ?: BigDecimal.ZERO,
                             sum = it?.sum ?: BigDecimal.ZERO,
                             avg = it?.avg ?: BigDecimal.ZERO,
                             count = it?.count ?: 0L) }
    }


    /**
     * Clears all transactions.
     */
    fun clear()
    {
        buckets.clear()
    }


    // For testing/debug
    /**
     * Gets the number of stored transactions.
     */
    fun count(): Long
            = buckets.values.map { it.count }.fold(0, Long::plus)


    /**
     * Occasionally, we want to clean-up the hash table (if it is a long-ish
     * running application, this might matter).  It will also ensure data is
     * classified consistently (we skip right to the default bucket when a
     * transaction comes in outside of the window, but we never moved stuff
     * that was outside of the window into the same bucket).
     *
     * It also seems like it is against the spirit of this assignment to just
     * delete the data.
     */
    @Synchronized
    private fun cleanUp()
    {
        // Make sure we still need to do something now that we have the lock.
        if(buckets.count() <= TRIGGER_CLEANUP_AT_SIZE)
            return

        val cleanUpAfterTime = Instant.now().minusSeconds(MAX_TIME + 1L)
                                        .toEpochMilli()

        val bucketIter = buckets.iterator()
        while (bucketIter.hasNext())
        {
            val (key, bucketContents) = bucketIter.next()
            if(key == DEFAULT_BUCKET)
                continue

            if(key < cleanUpAfterTime)
            {
                // Merge with default bucket
                val bucket = buckets.getOrPut(DEFAULT_BUCKET, ::TransactionContainer)
                bucket.addAll(bucketContents)

                bucketIter.remove()
            }
        }
    }
}