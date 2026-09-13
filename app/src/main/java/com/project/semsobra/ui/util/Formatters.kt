package com.project.semsobra.ui.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun parseDouble(value: String): Double = value.replace(",", ".").toDoubleOrNull() ?: 0.0

fun formatInput(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

fun formatQuantity(value: Double): String {
    return NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).apply {
        maximumFractionDigits = 2
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
