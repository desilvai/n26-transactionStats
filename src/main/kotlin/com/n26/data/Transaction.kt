package com.n26.data

import java.math.BigDecimal
import java.time.Instant

/**
 * DTO class that describes a transaction to add to the server.
 */
data class Transaction(val amount: BigDecimal, val timestamp: Instant)