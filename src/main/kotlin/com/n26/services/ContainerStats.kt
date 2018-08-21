package com.n26.services

import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP

data class ContainerStats(val min: BigDecimal,
                          val max: BigDecimal,
                          val sum: BigDecimal,
                          val count: Long)
{
    companion object
    {
        /**
         * We make the precision pretty large and can then adjust down when
         * we emit it out.
         */
        const val DIVISION_PRECISION = 6
    }


    init
    {
        // If we have a max and min value, we cannot have a 0 (or lower) count.
        require(count > 0) {
            "Invalid container count of $count.   Count must be greater than 0."
        }
    }


    /**
     * Computes the average.  This operation is safe as it is impossible to
     * divide by 0 (invariant: count > 0).
     */
    val avg: BigDecimal?
        get() = sum.divide(BigDecimal(count), DIVISION_PRECISION, HALF_UP)


    infix operator fun plus(other: ContainerStats?)
            = ContainerStats(other?.min?.min(min) ?: min,
                             other?.max?.max(max) ?: max,
                             other?.sum?.add(sum) ?: sum,
                             other?.count?.plus(count) ?: count)
}