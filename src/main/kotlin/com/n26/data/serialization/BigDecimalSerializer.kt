package com.n26.data.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.concurrent.getOrSet

/**
 * Serializes a BigDecimal so that it always prints two decimal places
 * rounding up the right-most place.
 */
class BigDecimalSerializer(decimal: Class<BigDecimal>? = null):
        StdSerializer<BigDecimal>(decimal)
{
    override fun serialize(decimal: BigDecimal,
                           jsonGenerator: JsonGenerator,
                           provider: SerializerProvider?)
    {
        // This is a thread-safe way to do serialization.
        decimal.let {
                    formatter.getOrSet(Companion::generateFormatter).format(it)
                }
                .let(jsonGenerator::writeString)
    }

    companion object
    {
        /**
         * Creates a string formatter for the default thread.
         */
        @JvmStatic
        val formatter: ThreadLocal<DecimalFormat>
                = ThreadLocal.withInitial(Companion::generateFormatter )

        @JvmStatic
        private fun generateFormatter() = DecimalFormat().apply {
                    roundingMode = RoundingMode.HALF_UP
                    maximumFractionDigits = 2
                    minimumFractionDigits = 2
                    isGroupingUsed = false
                }
    }
}