package com.n26.services

import com.n26.data.Transaction
import org.amshove.kluent.`should be null`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should not be null`
import org.junit.Test
import roundUp
import java.math.BigDecimal
import java.time.Instant

class TransactionContainerTest
{
    /*
     * Wolfram Alpha is my oracle here.
     */
    @Test
    fun `statistics are correct after many adds`()
    {
        val container = TransactionContainer()
        val values = (1..1000).map { 1.0 / it.toDouble() }

        val expectedMax = BigDecimal(1.0)
        val expectedMin = BigDecimal(1.0 / 1000.0)
        val expectedSum = BigDecimal(7.485470860550344912656518)
        val expectedCount = values.count()

        // WHEN we add 1000 values to the container
        values.map { Transaction(BigDecimal(it), Instant.now()) }
                .forEach { container.add(it) }

        // THEN  the stats are non-null
        val actualStats = container.containerStats
        actualStats.`should not be null`()

        // AND they are accurate.
        with(actualStats!!) {
            expectedMin `should equal` min
            expectedMax `should equal` max
            expectedSum.roundUp(10) `should equal` sum.roundUp(10)
            expectedCount.toLong() `should equal` count
        }
    }

    @Test
    fun `cannot get stats for an empty container`()
    {
        val container = TransactionContainer()
        container.containerStats.`should be null`()
    }
}