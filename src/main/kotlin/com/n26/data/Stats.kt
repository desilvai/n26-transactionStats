package com.n26.data;

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.n26.data.serialization.BigDecimalSerializer
import java.math.BigDecimal


/**
 * DTO class used to specify the statistics that can be returned to the client.
 */
data class Stats(
        @JsonSerialize(using = BigDecimalSerializer::class) val sum: BigDecimal,
        @JsonSerialize(using = BigDecimalSerializer::class) val avg: BigDecimal,
        @JsonSerialize(using = BigDecimalSerializer::class) val max: BigDecimal,
        @JsonSerialize(using = BigDecimalSerializer::class) val min: BigDecimal,
        val count: Long)
