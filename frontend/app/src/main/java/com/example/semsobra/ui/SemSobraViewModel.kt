package com.example.semsobra.ui

import androidx.lifecycle.ViewModel
import com.example.semsobra.ui.model.AnalyticsResult
import com.example.semsobra.ui.model.FoodUiModel
import com.example.semsobra.ui.model.ForecastResult
import com.example.semsobra.ui.model.ProductionDayUiModel
import com.example.semsobra.ui.model.ProductionItemDisplay
import com.example.semsobra.ui.model.ProductionItemUiModel
import com.example.semsobra.ui.model.ProductionSummary
import com.example.semsobra.ui.model.ReportSummary
import com.example.semsobra.ui.state.SemSobraPlanner
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SemSobraViewModel : ViewModel() {
    private val _foods = MutableStateFlow<List<FoodUiModel>>(emptyList())
    val foods: StateFlow<List<FoodUiModel>> = _foods.asStateFlow()

    private val _productionSummaries = MutableStateFlow<List<ProductionSummary>>(emptyList())
    val productionSummaries: StateFlow<List<ProductionSummary>> = _productionSummaries.asStateFlow()

    private val _analytics = MutableStateFlow(emptyAnalytics())
    val analytics: StateFlow<AnalyticsResult> = _analytics.asStateFlow()

    fun saveFood(id: Long, nome: String, descricao: String, unidade: String) {
        if (nome.isBlank()) return
        val food = FoodUiModel(
            id = id.takeIf { it > 0 } ?: nextFoodId(),
            nome = nome.trim(),
            descricao = descricao.trim(),
            unidadeMedida = unidade.trim().ifBlank { "kg" }
        )

        _foods.value = if (id > 0) {
            _foods.value.map { current -> if (current.id == id) food else current }
        } else {
            _foods.value + food
        }
        _productionSummaries.value = _productionSummaries.value.map { summary ->
            summary.copy(
                items = summary.items.map { display ->
                    if (display.food.id == food.id) display.copy(food = food) else display
                }
            )
        }
        refreshAnalytics()
    }

    fun deleteFood(food: FoodUiModel) {
        _foods.value = _foods.value.filterNot { it.id == food.id }
        _productionSummaries.value = _productionSummaries.value.mapNotNull { summary ->
            val remainingItems = summary.items.filterNot { it.food.id == food.id }
            if (remainingItems.isEmpty()) {
                null
            } else {
                summary.copy(
                    items = remainingItems,
                    totalSobra = remainingItems.sumOf { it.item.quantidadeSobra }
                )
            }
        }
        refreshAnalytics()
    }

    fun saveProductionToday(quantitiesByFoodId: Map<Long, Double>) {
        val today = LocalDate.now()
        val existingSummary = _productionSummaries.value.firstOrNull { it.day.data == today.toString() }
        val dayId = existingSummary?.day?.id ?: nextProductionDayId()
        var nextItemId = nextProductionItemId()
        val displayItems = quantitiesByFoodId
            .filterValues { it > 0.0 }
            .mapNotNull { (foodId, quantity) ->
                val food = _foods.value.firstOrNull { it.id == foodId } ?: return@mapNotNull null
                val previousItem = existingSummary?.items?.firstOrNull { it.food.id == foodId }?.item
                val item = ProductionItemUiModel(
                    id = previousItem?.id ?: nextItemId++,
                    producaoDiaId = dayId,
                    alimentoId = foodId,
                    quantidadeProduzida = quantity,
                    quantidadeSobra = previousItem?.quantidadeSobra ?: 0.0,
                    acabouAntesDoFim = previousItem?.acabouAntesDoFim ?: false,
                    horarioAcabou = previousItem?.horarioAcabou
                )
                ProductionItemDisplay(
                    item = item,
                    food = food,
                    consumo = SemSobraPlanner.consumo(item.quantidadeProduzida, item.quantidadeSobra)
                )
            }

        val updatedSummary = ProductionSummary(
            day = existingSummary?.day ?: ProductionDayUiModel(
                id = dayId,
                data = today.toString(),
                diaDaSemana = today.dayOfWeek.value
            ),
            items = displayItems,
            totalSobra = displayItems.sumOf { it.item.quantidadeSobra },
            fechado = existingSummary?.fechado ?: false
        )

        _productionSummaries.value = _productionSummaries.value
            .filterNot { it.day.id == dayId } + updatedSummary
        refreshAnalytics()
    }

    fun closeProduction(
        productionDayId: Long,
        clientesAtendidos: Int,
        closingItems: List<ProductionItemUiModel>
    ) {
        _productionSummaries.value = _productionSummaries.value.map { summary ->
            if (summary.day.id != productionDayId) return@map summary

            val foodsById = _foods.value.associateBy { it.id }
            val items = closingItems.mapNotNull { item ->
                val food = foodsById[item.alimentoId] ?: return@mapNotNull null
                ProductionItemDisplay(
                    item = item,
                    food = food,
                    consumo = SemSobraPlanner.consumo(item.quantidadeProduzida, item.quantidadeSobra)
                )
            }
            summary.copy(
                day = summary.day.copy(clientesAtendidos = clientesAtendidos.coerceAtLeast(0)),
                items = items,
                totalSobra = items.sumOf { it.item.quantidadeSobra },
                fechado = true
            )
        }
        refreshAnalytics()
    }

    private fun refreshAnalytics() {
        val summaries = _productionSummaries.value.sortedByDescending { it.day.data }
        _productionSummaries.value = summaries
        _analytics.value = AnalyticsResult(
            forecast = SemSobraPlanner.forecast(
                foods = _foods.value,
                summaries = summaries,
                targetDayOfWeek = LocalDate.now().dayOfWeek.value
            ),
            report = SemSobraPlanner.report(summaries)
        )
    }

    private fun nextFoodId(): Long = (_foods.value.maxOfOrNull { it.id } ?: 0L) + 1L

    private fun nextProductionDayId(): Long = (_productionSummaries.value.maxOfOrNull { it.day.id } ?: 0L) + 1L

    private fun nextProductionItemId(): Long {
        return (_productionSummaries.value.flatMap { summary -> summary.items }.maxOfOrNull { it.item.id } ?: 0L) + 1L
    }

    private companion object {
        fun emptyAnalytics() = AnalyticsResult(
            forecast = ForecastResult(clientesPrevistos = 0, items = emptyList(), alerts = emptyList()),
            report = ReportSummary(
                totalSobras = 0.0,
                alimentosComMaisSobra = emptyList(),
                alimentosQueMaisAcabaram = emptyList()
            )
        )
    }
}
