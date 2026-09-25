package com.project.semsobra.ui.util

import com.project.semsobra.domain.model.QuantityPolicy
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun parseDoubleOrNull(value: String): Double? = value
    .trim()
    .replace(",", ".")
    .toDoubleOrNull()
    ?.takeIf(Double::isFinite)
    ?.let(QuantityPolicy::normalize)

fun formatInput(value: Double): String =
    QuantityPolicy.normalize(value).let { normalized ->
        BigDecimal.valueOf(normalized).stripTrailingZeros().toPlainString().replace('.', ',')
    }

fun formatQuantity(value: Double): String {
    return NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).apply {
        maximumFractionDigits = QuantityPolicy.STORAGE_SCALE
        minimumFractionDigits = 0
    }.format(value)
}

fun formatWeightOneDecimal(value: Double): String {
    return NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).apply {
        maximumFractionDigits = 1
        minimumFractionDigits = 1
    }.format(value)
}

fun formatPercentage(value: Double): String {
    return NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).apply {
        maximumFractionDigits = 1
        minimumFractionDigits = 0
    }.format(value * 100.0)
}

fun formatDate(value: String): String {
    return runCatching {
        LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }.getOrDefault(value)
}

fun dayName(dayOfWeek: Int): String = when (dayOfWeek) {
    1 -> "segunda-feira"
    2 -> "terça-feira"
    3 -> "quarta-feira"
    4 -> "quinta-feira"
    5 -> "sexta-feira"
    6 -> "sábado"
    7 -> "domingo"
    else -> "-"
}
