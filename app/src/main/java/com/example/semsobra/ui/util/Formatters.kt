package com.example.semsobra.ui.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun parseDouble(value: String): Double {
    return value.replace(",", ".").toDoubleOrNull() ?: 0.0
}

fun formatInput(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}

fun formatQuantity(value: Double): String {
    return NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }.format(value)
}

fun formatDate(value: String): String {
    return runCatching {
        LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }.getOrDefault(value)
}

fun dayName(dayOfWeek: Int): String {
    return when (dayOfWeek) {
        1 -> "segunda-feira"
        2 -> "terca-feira"
        3 -> "quarta-feira"
        4 -> "quinta-feira"
        5 -> "sexta-feira"
        6 -> "sabado"
        7 -> "domingo"
        else -> "-"
    }
}
