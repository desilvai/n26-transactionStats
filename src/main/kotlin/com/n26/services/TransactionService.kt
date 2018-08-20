package com.n26.services

import com.n26.data.Stats
import com.n26.data.Transaction
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service
class TransactionService
{
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


        @JvmStatic
        fun getBucketId(instant: Instant): Instant
        {
            val nanos = (instant.nano / NANOS_PER_BUCKET) * NANOS_PER_BUCKET
            return instant.minusNanos((instant.nano - nanos).toLong())
        }
    }


    private val buckets = ConcurrentHashMap<Instant, TransactionContainer>()

    fun add(transaction: Transaction)
    {
        val bucketId = getBucketId(transaction.timestamp)
        val bucket = buckets.getOrPut(bucketId, ::TransactionContainer)
        bucket.add(transaction)
    }

    fun getStats(): Stats
    {
        // Get range
        val startTime = Instant.now()

        // Retrieve stats for each bucket, combine them, then return
        // What if there are no transactions?
        return (MAX_TIME * BUCKETS_PER_SECOND).downTo(0).asSequence()
                .map { (NANOS_PER_SECOND * it).toLong() }
                .map { getBucketId(startTime.minusNanos(it)) }
                .mapNotNull { buckets[it]?.getContainerStats() }
                .takeIf { it.count() > 0 }
                ?.reduce(ContainerStats::plus)
                ?.let { Stats(min = it.min,
                              max = it.max,
                              sum = it.sum,
                              avg = it.avg,
                              count = it.count) }
                ?: Stats(null, null, null, null, 0L)
    }

    fun clear()
    {
        buckets.clear()
    }

    // For debug
    fun count(): Long
            = buckets.values.map { it.count }.fold(0, Long::plus)
}