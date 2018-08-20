package com.n26.services

import org.amshove.kluent.`should equal`
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP

class ContainerStatsTest
{
    @Test
    fun `add 2 containers`()
    {
        val sum2 = BigDecimal(-2.45 - 1.2 - 1.5 - 2.1)
        val containers = listOf(
                // 0, 1
                ContainerStats(min = BigDecimal.ZERO,
                               max = BigDecimal.ONE,
                               sum = BigDecimal.ONE,
                               count = 2),
                // -2.45, -1.2, -1.5, -2.1
                ContainerStats(min = BigDecimal(-2.45),
                               max = BigDecimal(-1.2),
                               sum = sum2,
                               count = 4)
        )

        val result = containers.first() + containers.last()
        result.min `should equal` BigDecimal(-2.45)
        result.max `should equal` BigDecimal.ONE
        result.count `should equal` 6L

        val expectedSum = BigDecimal(-6.25)
        val expectedAvg = BigDecimal(-1.04166666667)
                .setScale(ContainerStats.DIVISION_PRECISION, HALF_UP)
        result.sum `should equal` expectedSum
        result.avg `should equal` expectedAvg
    }


    @Test(expected = IllegalArgumentException::class)
    fun `container stats cannot have a 0-count`()
    {
        ContainerStats(min = BigDecimal(0),
                       max = BigDecimal(0),
                       sum = BigDecimal(0),
                       count = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `container stats cannot have a negative count`()
    {
        ContainerStats(min = BigDecimal(0),
                       max = BigDecimal(0),
                       sum = BigDecimal(0),
                       count = -1)
    }
}