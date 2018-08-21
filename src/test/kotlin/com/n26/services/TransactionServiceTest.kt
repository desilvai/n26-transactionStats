package com.n26.services

import com.n26.data.Transaction
import dev.desilvai.utils.AccessesPrivateData
import org.amshove.kluent.`should be false`
import org.amshove.kluent.`should be true`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should not equal`
import org.junit.Assert
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * There are two ways to test these.  First, checking against the default
 * bucket mapping and the second checking one that we provide so we can see
 * its contents.
 */
class TransactionServiceTest: AccessesPrivateData
{
    companion object
    {
        const val RANDOM_SEED = 4927L
    }

    @Test
    fun `add 4 transactions in window`()
    {
        val hashMap = ConcurrentHashMap<Long, TransactionContainer>()
        val service = TransactionService(hashMap)

        // When we add 4 transactions
        val transactions = listOf(
                Transaction(BigDecimal(2456.39195),
                            Instant.now().minusSeconds(25L).minusNanos(2)),
                Transaction(BigDecimal(857),
                            Instant.now().minusSeconds(58L)),
                Transaction(BigDecimal(-94732.847033592),
                            Instant.now()),
                Transaction(BigDecimal(-9992.0002),
                            Instant.now().minusSeconds(3L).minusNanos(5000))
        )

        // WHEN we add the transactions to the service
        // THEN each is within the window
        transactions.map { service.add(it) }
                .forEach { it.`should be true`() }

        // AND all transactions have been successfully added.
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

        // AND the transaction has been successfully added.
        val actual = hashMap.values.flatMap { it.values }.toSet()
        val expected = setOf(transaction)

        actual `should equal` expected
    }


//    private fun generateTransactions(number: Int, timeRange: Long):
//            List<Transaction>
//    {
//        val rand = Random(RANDOM_SEED)
//        return (1..number).map {
//            Transaction(BigDecimal(rand.nextDouble()),
//                        Instant.now().minusSeconds(rand.nextLong() % (timeRange - 1))
//                                .minusNanos(rand.nextLong() % TransactionService.NANOS_PER_SECOND))
//        }
//    }



    @Test
    fun `failing stats`() {
        val hashMap = ConcurrentHashMap<Long, TransactionContainer>()
        val service = TransactionService(hashMap)

        service.clear()

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
        stats.count `should equal` 3
        stats.min?.roundUp2() `should equal` BigDecimal(3.00).roundUp2()
        stats.max?.roundUp2() `should equal` BigDecimal(5).roundUp2()
        stats.sum?.roundUp2() `should equal` BigDecimal(11.00).roundUp2()
        stats.avg?.roundUp2() `should equal` BigDecimal(3.67).roundUp2()

        Thread.sleep(5000)
        val stats2 = service.getStats()
        println(stats2)
    }


    @Test
    fun `checkIds`()
    {
        val firstTime = Instant.now()
        val secondTime = Instant.now().plusMillis(5000)

        Assert.assertNotEquals(firstTime, secondTime)

        val firstBucket = TransactionService.getBucketId(firstTime)
        val secondBucket = TransactionService.getBucketId(secondTime)

        firstBucket `should not equal` secondBucket
    }

    @Test
    fun `check windows`()
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

    private fun BigDecimal.roundUp2(): BigDecimal
            = this.setScale(2, HALF_UP)
}