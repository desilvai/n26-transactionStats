package com.n26.services

import com.n26.data.Transaction
import dev.desilvai.utils.AccessesPrivateData
import org.amshove.kluent.`should be false`
import org.amshove.kluent.`should be true`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should not be null`
import org.amshove.kluent.`should not equal`
import org.junit.Assert
import org.junit.Test
import roundUp
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.fail

/**
 * There are two ways to test these.  First, checking against the default
 * bucket mapping (either by accessing the private data or by checking the
 * counts) and, second, checking the hash map that we provide to the
 * constructor so we can analyze the updates made to it.  We are using the
 * second option.
 */
class TransactionServiceTest: AccessesPrivateData
{
    @Test
    fun `add 5 transactions in window`()
    {
        val hashMap = ConcurrentHashMap<Long, TransactionContainer>()
        val service = TransactionService(hashMap)

        // When we add 5 transactions
        val transactions = listOf(
                Transaction(BigDecimal(2456.39195),
                            Instant.now().minusSeconds(25L).minusNanos(2)),
                Transaction(BigDecimal(857),
                            Instant.now().minusSeconds(58L)),
                Transaction(BigDecimal(-94732.847033592),
                            Instant.now()),
                Transaction(BigDecimal(-9992.0002),
                            Instant.now().minusSeconds(3L).minusNanos(5000)),
                Transaction(BigDecimal(-94732.847033592),
                            Instant.now().minusMillis(4))
        )

        // WHEN we add the transactions to the service
        // THEN each is within the window
        transactions.map { service.add(it) }
                .forEach { it.`should be true`() }

        // AND all transactions have been successfully added.
        // (two will likely be in the same bucket and all of the others
        //  will be in their own; we don't check for this.)
        val actual = hashMap.values.flatMap { it.values }.toSet()
        val expected = transactions.toSet()

        actual `should equal` expected
    }


    @Test
    fun `add a transaction outside window`()
    {
        val hashMap = ConcurrentHashMap<Long, TransactionContainer>()
        val service = TransactionService(hashMap)

        val transaction = Transaction(BigDecimal(2456.39195),
                                      Instant.now().minusSeconds(60)
                                                   .minusNanos(1))

        // WHEN we add a transaction outside of the window
        val wasInWindow = service.add(transaction)

        // THEN we flagged the transaction as outside the window
        wasInWindow.`should be false`()

        // AND the transaction was successfully added to the "default" bucket
        val defaultBucket = hashMap[TransactionService.DEFAULT_BUCKET] ?:
                            fail("The transaction was not in the default " +
                                 "bucket as expected!  This means it was in " +
                                 "the statistics window after all.")
        defaultBucket.values.toSet() `should equal` setOf(transaction)

        // AND no other transaction buckets exist.
        hashMap.count() `should equal` 1
    }

    @Test(expected = IllegalArgumentException::class)
    fun `add fails with timestamp from future`()
    {
        val service = TransactionService()

        service.add(Transaction(BigDecimal.ZERO,
                                Instant.now().plusMillis(200)))
    }


    @Test
    fun `get stats expecting the same before and after a delay`() {
        val hashMap = ConcurrentHashMap<Long, TransactionContainer>()
        val service = TransactionService(hashMap)

        service.add(Transaction(BigDecimal(5L),
                                Instant.now().plusMillis(-30001)))
                .`should be true`()
        service.add(Transaction(BigDecimal(3L),
                                Instant.now().plusMillis(-20000)))
                .`should be true`()
        service.add(Transaction(BigDecimal(3L),
                                Instant.now().plusMillis(-10800)))
                .`should be true`()

        val stats = service.getStats()
        with(stats)
        {
            count `should equal` 3
            min.roundUp(2) `should equal` BigDecimal(3.00).roundUp(2)
            max.roundUp(2) `should equal` BigDecimal(5).roundUp(2)
            sum.roundUp(2) `should equal` BigDecimal(11.00).roundUp(2)
            avg.roundUp(2) `should equal` BigDecimal(3.67).roundUp(2)
        }

        // TODO -- There has to be a better way to test this!  Maybe use a
        // factory?
        Thread.sleep(5000)

        val stats2 = service.getStats()
        with(stats2)
        {
            count `should equal` 3
            min.roundUp(2) `should equal` BigDecimal(3.00).roundUp(2)
            max.roundUp(2) `should equal` BigDecimal(5).roundUp(2)
            sum.roundUp(2) `should equal` BigDecimal(11.00).roundUp(2)
            avg.roundUp(2) `should equal` BigDecimal(3.67).roundUp(2)
        }

        // Check that each transaction is in its own bucket.
        hashMap.count() `should equal` 3
        hashMap.values.forEach { it.count `should equal` 1 }
    }


    @Test
    fun `get stats when no transactions present`()
    {
        val service = TransactionService()

        val stats = service.getStats()
        with(stats)
        {
            count `should equal` 0
            min.roundUp(2) `should equal` BigDecimal.ZERO.roundUp(2)
            max.roundUp(2) `should equal` BigDecimal.ZERO.roundUp(2)
            sum.roundUp(2) `should equal` BigDecimal.ZERO.roundUp(2)
            avg.roundUp(2) `should equal` BigDecimal.ZERO.roundUp(2)
        }
    }


    @Test
    fun `trigger clean-up`()
    {
        val hashMap = ConcurrentHashMap<Long, TransactionContainer>()

        val transactionsByTime = (2..4)
                .map { it.toLong() * TransactionService.MAX_TIME }
                .map { Instant.now().minusSeconds(it) }
                .asSequence()
                .flatMap(TransactionService.Companion::bucketWindowSequence)
                .associate {
                    Pair(it,
                         Transaction(BigDecimal.TEN, Instant.ofEpochMilli(it)))
                }

        transactionsByTime
                .mapValues { TransactionContainer().apply { add(it.value) } }
                .also { hashMap.putAll(it) }

        val service = TransactionService(hashMap)

        // WHEN we add another transaction
        val newTransaction = Transaction(BigDecimal.ONE, Instant.now())
        service.add(newTransaction)
                .`should be true`()

        // THEN clean-up is triggered and everything that was previously
        // present (which is too old) is moved to the default bucket.
        val expectedTransactions = transactionsByTime.values.toSet()

        hashMap[TransactionService.DEFAULT_BUCKET].`should not be null`()
        val actualTransactions = hashMap[TransactionService.DEFAULT_BUCKET]!!
                                        .values.toSet()

        expectedTransactions `should equal` actualTransactions

        // AND only the latest transaction is in the other bucket.
        2 `should equal`  hashMap.count()
        hashMap.remove(TransactionService.DEFAULT_BUCKET)
        setOf(newTransaction) `should equal` hashMap.values.single().values.toSet()
    }

    // Tests the companion object
    @Test
    fun `bucket ids are unique when instants separated by at least MILLIS_PER_BUCKET`()
    {
        val firstTime = Instant.now()
        val secondTime = Instant.now()
                .plusMillis(TransactionService.MILLIS_PER_BUCKET.toLong())

        Assert.assertNotEquals(firstTime, secondTime)

        val firstBucket = TransactionService.getBucketId(firstTime)
        val secondBucket = TransactionService.getBucketId(secondTime)

        firstBucket `should not equal` secondBucket
    }

    @Test
    fun `bucket windows are unique`()
    {
        val firstTime = Instant.now()
        val secondTime = Instant.now().plusMillis(5000)

        Assert.assertNotEquals(firstTime, secondTime)

        val firstWindow = TransactionService.bucketWindowSequence(firstTime).toList()
        val secondWindow = TransactionService.bucketWindowSequence(secondTime).toList()

        val expectedWindowSize = TransactionService.MAX_TIME *
                                 TransactionService.BUCKETS_PER_SECOND
        firstWindow.count() `should equal` expectedWindowSize
        firstWindow.count() `should equal` expectedWindowSize

        firstWindow `should not equal` secondWindow
    }


}