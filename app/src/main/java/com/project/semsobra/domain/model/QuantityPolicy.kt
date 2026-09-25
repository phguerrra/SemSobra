package com.project.semsobra.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/** Centraliza a precisão usada para quantidades persistidas e calculadas. */
object QuantityPolicy {
    const val STORAGE_SCALE = 3

    fun normalize(value: Double, scale: Int = STORAGE_SCALE): Double {
        if (!value.isFinite()) return value
        return BigDecimal.valueOf(value)
            .setScale(scale, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun sum(values: Iterable<Double>): Double = normalize(
        values.fold(BigDecimal.ZERO) { total, value ->
            total.add(BigDecimal.valueOf(normalize(value)))
        }.toDouble()
    )

    fun subtract(minuend: Double, subtrahend: Double): Double = normalize(
        BigDecimal.valueOf(normalize(minuend))
            .subtract(BigDecimal.valueOf(normalize(subtrahend)))
            .toDouble()
    )
}
