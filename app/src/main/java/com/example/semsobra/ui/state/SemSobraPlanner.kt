package com.example.semsobra.ui.state

import com.example.semsobra.ui.model.FoodMetric
import com.example.semsobra.ui.model.FoodUiModel
import com.example.semsobra.ui.model.ForecastItem
import com.example.semsobra.ui.model.ForecastResult
import com.example.semsobra.ui.model.ProductionSummary
import com.example.semsobra.ui.model.ReportSummary
import kotlin.math.roundToInt

object SemSobraPlanner {
    fun consumo(quantidadeProduzida: Double, quantidadeSobra: Double): Double {
        return (quantidadeProduzida - quantidadeSobra).coerceAtLeast(0.0)
    }

    fun forecast(
        foods: List<FoodUiModel>,
        summaries: List<ProductionSummary>,
        targetDayOfWeek: Int
    ): ForecastResult {
        val closedSummaries = summaries.filter { it.fechado && it.day.clientesAtendidos > 0 }
        val sameWeekDay = closedSummaries.filter { it.day.diaDaSemana == targetDayOfWeek }
        val baseDays = sameWeekDay.ifEmpty { closedSummaries.take(5) }
        val clientesPrevistos = baseDays
            .map { it.day.clientesAtendidos }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.roundToInt()
            ?: 0

        val recent = closedSummaries.take(5)
        val forecastItems = foods.map { food ->
            val historicalItems = baseDays.mapNotNull { summary ->
                val item = summary.items.firstOrNull { it.food.id == food.id }
                if (item != null && summary.day.clientesAtendidos > 0) {
                    item.consumo / summary.day.clientesAtendidos
                } else {
                    null
                }
            }
            val consumoMedioPorCliente = historicalItems.takeIf { it.isNotEmpty() }?.average() ?: 0.0
            val ranOutRecently = recent.any { summary ->
                summary.items.any { it.food.id == food.id && it.item.acabouAntesDoFim }
            }
            val recommended = clientesPrevistos * consumoMedioPorCliente
            ForecastItem(
                food = food,
                quantidadeRecomendada = if (ranOutRecently) recommended * 1.10 else recommended,
                consumoMedioPorCliente = consumoMedioPorCliente,
                ajusteSegurancaAplicado = ranOutRecently
            )
        }

        val highWasteAlerts = recent.flatMap { it.items }
            .filter { it.item.quantidadeProduzida > 0 && it.item.quantidadeSobra / it.item.quantidadeProduzida >= 0.25 }
            .map { "Sobra alta recente em ${it.food.nome}" }

        val ranOutAlerts = recent.flatMap { it.items }
            .filter { it.item.acabouAntesDoFim }
            .map { "${it.food.nome} costuma acabar antes do fim" }

        return ForecastResult(
            clientesPrevistos = clientesPrevistos,
            items = forecastItems,
            alerts = (highWasteAlerts + ranOutAlerts).distinct().take(5)
        )
    }

    fun report(summaries: List<ProductionSummary>): ReportSummary {
        val allItems = summaries.flatMap { it.items }
        val totalSobras = allItems.sumOf { it.item.quantidadeSobra }
        val maisSobra = allItems
            .groupBy { it.food }
            .map { (food, items) -> FoodMetric(food, items.sumOf { it.item.quantidadeSobra }) }
            .sortedByDescending { it.quantidade }
            .take(5)
        val maisAcabaram = allItems
            .filter { it.item.acabouAntesDoFim }
            .groupBy { it.food }
            .map { (food, items) -> FoodMetric(food, items.size.toDouble()) }
            .sortedByDescending { it.quantidade }
            .take(5)

        return ReportSummary(
            totalSobras = totalSobras,
            alimentosComMaisSobra = maisSobra,
            alimentosQueMaisAcabaram = maisAcabaram
        )
    }
}
