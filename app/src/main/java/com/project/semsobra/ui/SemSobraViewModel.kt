package com.project.semsobra.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.project.semsobra.data.mapper.HistoricoProducaoMapper
import com.project.semsobra.data.repository.PreparoLocalRepository
import com.project.semsobra.domain.previsao.MotorPrevisao
import com.project.semsobra.domain.previsao.PrevisaoPorMediaPonderada
import com.project.semsobra.domain.previsao.model.EntradaPrevisao
import com.project.semsobra.domain.previsao.model.ResultadoPrevisao
import com.project.semsobra.domain.previsao.model.Turno
import com.project.semsobra.ui.model.AnalyticsResult
import com.project.semsobra.ui.model.FoodMetric
import com.project.semsobra.ui.model.FoodUiModel
import com.project.semsobra.ui.model.disponivelNoDia
import com.project.semsobra.ui.model.ForecastItem
import com.project.semsobra.ui.model.ForecastResult
import com.project.semsobra.ui.model.ProductionDayUiModel
import com.project.semsobra.ui.model.ProductionItemDisplay
import com.project.semsobra.ui.model.ProductionItemUiModel
import com.project.semsobra.ui.model.ProductionSummary
import com.project.semsobra.ui.model.ReportSummary
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SemSobraUiState(
    val previsaoDemanda: ResultadoPrevisao,
    val foods: List<FoodUiModel> = emptyList(),
    val productionSummaries: List<ProductionSummary> = emptyList(),
    val analytics: AnalyticsResult = emptyAnalytics()
)

class SemSobraViewModel(application: Application) : AndroidViewModel(application) {
    private val motorPrevisao: MotorPrevisao = PrevisaoPorMediaPonderada()
    private val historicoMapper = HistoricoProducaoMapper()
    private val preparoRepository = PreparoLocalRepository(application)

    private val _uiState = MutableStateFlow(
        SemSobraUiState(previsaoDemanda = calcularPrevisaoDemanda(emptyList()))
    )
    val uiState: StateFlow<SemSobraUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var nextFoodId = 1L
    private var nextProductionId = 1L
    private var nextProductionItemId = 1L

    init {
        carregarPreparos()
    }

    private fun carregarPreparos() {
        viewModelScope.launch {
            try {
                val preparos = withContext(Dispatchers.IO) {
                    preparoRepository.listarTodos()
                }
                val foods = preparos.map { preparo ->
                    FoodUiModel(
                        id = preparo.id,
                        nome = preparo.nome,
                        descricao = preparo.descricao,
                        unidadeMedida = preparo.unidadeMedida,
                        diaDaSemana = preparo.diaDaSemana
                    )
                }

                nextFoodId = (foods.maxOfOrNull { it.id } ?: 0L) + 1
                updateState(foods, _uiState.value.productionSummaries)
            } catch (_: Exception) {
                _messages.emit("Não foi possível carregar os preparos salvos")
            }
        }
    }

    fun saveFood(id: Long, nome: String, descricao: String, unidade: String, diaDaSemana: Int) {
        val cleanName = nome.trim()
        if (cleanName.isBlank()) {
            _messages.tryEmit("Informe o nome do preparo")
            return
        }

        val current = _uiState.value
        val cleanUnit = unidade.trim().ifBlank { "kg" }
        val food = FoodUiModel(
            id = if (id > 0) id else nextFoodId++,
            nome = cleanName,
            descricao = descricao.trim(),
            unidadeMedida = cleanUnit,
            diaDaSemana = diaDaSemana.takeIf { it in 1..7 } ?: FoodUiModel.TODOS_OS_DIAS
        )
        val foods = if (id > 0) {
            current.foods.map { item -> if (item.id == id) food else item }
        } else {
            current.foods + food
        }
        val summaries = current.productionSummaries.map { summary ->
            summary.copy(
                items = summary.items.map { display ->
                    if (display.food.id == food.id) display.copy(food = food) else display
                }
            )
        }

        updateState(foods, summaries)
        _messages.tryEmit(if (id > 0) "Preparo atualizado" else "Preparo cadastrado")
    }

    fun deleteFood(food: FoodUiModel) {
        val current = _uiState.value
        val foods = current.foods.filterNot { it.id == food.id }
        val summaries = current.productionSummaries.mapNotNull { summary ->
            val items = summary.items.filterNot { it.food.id == food.id }
            if (items.isEmpty()) {
                null
            } else {
                summary.copy(items = items, totalSobra = items.sumOf { it.item.quantidadeSobra })
            }
        }
        updateState(foods, summaries)
        _messages.tryEmit("Preparo excluído")
    }

    fun saveProductionToday(quantitiesByFoodId: Map<Long, Double>) {
        val current = _uiState.value
        val validQuantities = quantitiesByFoodId.filterValues { it > 0.0 }
        if (validQuantities.isEmpty()) {
            _messages.tryEmit("Informe ao menos uma quantidade maior que zero")
            return
        }

        val today = LocalDate.now()
        val previous = current.productionSummaries.firstOrNull { it.day.data == today.toString() }
        val productionId = previous?.day?.id ?: nextProductionId++
        val itemsByFood = previous?.items.orEmpty().associateBy { it.food.id }
        val items = current.foods
            .filter { it.disponivelNoDia(today.dayOfWeek.value) }
            .mapNotNull { food ->
            val quantity = validQuantities[food.id] ?: return@mapNotNull null
            val previousItem = itemsByFood[food.id]?.item
            val item = ProductionItemUiModel(
                id = previousItem?.id ?: nextProductionItemId++,
                producaoDiaId = productionId,
                alimentoId = food.id,
                quantidadeProduzida = quantity
            )
            ProductionItemDisplay(item = item, food = food, consumo = quantity)
        }
        if (items.isEmpty()) {
            _messages.tryEmit("Os preparos informados não estão mais disponíveis")
            return
        }

        val summary = ProductionSummary(
            day = ProductionDayUiModel(
                id = productionId,
                data = today.toString(),
                diaDaSemana = today.dayOfWeek.value
            ),
            items = items,
            totalSobra = 0.0,
            fechado = false
        )
        val summaries = (current.productionSummaries.filterNot { it.day.id == productionId } + summary)
            .sortedByDescending { it.day.data }
        updateState(current.foods, summaries)
        _messages.tryEmit("Produção salva")
    }

    fun closeProduction(
        productionDayId: Long,
        clientesAtendidos: Int,
        closingItems: List<ProductionItemUiModel>
    ) {
        if (clientesAtendidos <= 0) {
            _messages.tryEmit("Informe a quantidade de clientes atendidos")
            return
        }

        val current = _uiState.value
        val summaries = current.productionSummaries.map { summary ->
            if (summary.day.id != productionDayId) {
                summary
            } else {
                val closingById = closingItems.associateBy { it.id }
                val displays = summary.items.map { display ->
                    val savedItem = closingById[display.item.id] ?: display.item
                    val safeLeftover = savedItem.quantidadeSobra
                        .coerceAtLeast(0.0)
                        .coerceAtMost(savedItem.quantidadeProduzida)
                    val item = savedItem.copy(quantidadeSobra = safeLeftover)
                    display.copy(
                        item = item,
                        consumo = (item.quantidadeProduzida - safeLeftover).coerceAtLeast(0.0)
                    )
                }
                summary.copy(
                    day = summary.day.copy(clientesAtendidos = clientesAtendidos),
                    items = displays,
                    totalSobra = displays.sumOf { it.item.quantidadeSobra },
                    fechado = true
                )
            }
        }
        updateState(current.foods, summaries)
        _messages.tryEmit("Fechamento salvo")
    }

    private fun updateState(foods: List<FoodUiModel>, summaries: List<ProductionSummary>) {
        val previsaoDemanda = calcularPrevisaoDemanda(summaries)
        val foodsNaDataPrevista = foods.filter {
            it.disponivelNoDia(previsaoDemanda.dataPrevisao.dayOfWeek.value)
        }
        _uiState.value = SemSobraUiState(
            previsaoDemanda = previsaoDemanda,
            foods = foods,
            productionSummaries = summaries,
            analytics = calculateAnalytics(
                foods = foodsNaDataPrevista,
                summaries = summaries,
                forecastCustomers = previsaoDemanda.clientesPrevistos
            )
        )
    }

    private fun calcularPrevisaoDemanda(
        summaries: List<ProductionSummary>
    ): ResultadoPrevisao = motorPrevisao.calcular(
        EntradaPrevisao(
            dataPrevisao = LocalDate.now().plusDays(1),
            turno = Turno.ALMOCO,
            historico = historicoMapper.mapear(summaries)
        )
    )
}

private fun calculateAnalytics(
    foods: List<FoodUiModel>,
    summaries: List<ProductionSummary>,
    forecastCustomers: Int
): AnalyticsResult {
    val closed = summaries.filter { it.fechado && it.day.clientesAtendidos > 0 }

    val forecastItems = if (forecastCustomers == 0) {
        emptyList()
    } else {
        foods.mapNotNull { food ->
            val history = closed.mapNotNull { summary ->
                summary.items.firstOrNull { it.food.id == food.id }
                    ?.let { summary.day.clientesAtendidos to it }
            }
            val customerTotal = history.sumOf { it.first }
            if (customerTotal == 0) return@mapNotNull null
            val averagePerCustomer = history.sumOf { it.second.consumo } / customerTotal
            val hadShortage = history.take(3).any { it.second.item.acabouAntesDoFim }
            val safetyFactor = if (hadShortage) 1.1 else 1.0
            ForecastItem(
                food = food,
                quantidadeRecomendada = averagePerCustomer * forecastCustomers * safetyFactor,
                consumoMedioPorCliente = averagePerCustomer,
                ajusteSegurancaAplicado = hadShortage
            )
        }
    }

    val alerts = closed.take(3).flatMap { summary ->
        summary.items.mapNotNull { display ->
            when {
                display.item.acabouAntesDoFim ->
                    "${display.food.nome} acabou antes do fim do atendimento."
                display.item.quantidadeProduzida > 0 &&
                    display.item.quantidadeSobra / display.item.quantidadeProduzida >= 0.2 ->
                    "${display.food.nome} teve sobra acima de 20%."
                else -> null
            }
        }
    }.distinct().take(5)

    val allClosedItems = closed.flatMap { it.items }
    val leftovers = allClosedItems
        .groupBy { it.food.id }
        .mapNotNull { (_, items) ->
            val quantity = items.sumOf { it.item.quantidadeSobra }
            items.firstOrNull()?.food?.takeIf { quantity > 0.0 }?.let { FoodMetric(it, quantity) }
        }
        .sortedByDescending { it.quantidade }
    val shortages = allClosedItems
        .filter { it.item.acabouAntesDoFim }
        .groupBy { it.food.id }
        .mapNotNull { (_, items) ->
            items.firstOrNull()?.food?.let { FoodMetric(it, items.size.toDouble()) }
        }
        .sortedByDescending { it.quantidade }

    return AnalyticsResult(
        forecast = ForecastResult(
            clientesPrevistos = forecastCustomers,
            items = forecastItems,
            alerts = alerts
        ),
        report = ReportSummary(
            totalSobras = allClosedItems.sumOf { it.item.quantidadeSobra },
            alimentosComMaisSobra = leftovers,
            alimentosQueMaisAcabaram = shortages
        )
    )
}

private fun emptyAnalytics() = AnalyticsResult(
    forecast = ForecastResult(
        clientesPrevistos = 0,
        items = emptyList(),
        alerts = emptyList()
    ),
    report = ReportSummary(
        totalSobras = 0.0,
        alimentosComMaisSobra = emptyList(),
        alimentosQueMaisAcabaram = emptyList()
    )
)
