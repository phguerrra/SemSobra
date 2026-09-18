package com.project.semsobra.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.project.semsobra.data.mapper.HistoricoProducaoMapper
import com.project.semsobra.data.repository.PreparoLocalRepository
import com.project.semsobra.domain.model.Preparo
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

    private var nextProductionId = 1L
    private var nextProductionItemId = 1L

    init {
        carregarPreparos()
    }

    private fun carregarPreparos() {
        viewModelScope.launch {
            try {
                atualizarPreparosSalvos()
            } catch (_: Exception) {
                _messages.emit("Não foi possível carregar os preparos salvos")
            }
        }
    }

    private suspend fun atualizarPreparosSalvos(
        productionSummaries: List<ProductionSummary> = _uiState.value.productionSummaries
    ) {
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
        val foodsById = foods.associateBy(FoodUiModel::id)
        val summaries = productionSummaries.map { summary ->
            summary.copy(
                items = summary.items.map { display ->
                    val savedFood = foodsById[display.food.id]
                    if (savedFood == null) display else display.copy(food = savedFood)
                }
            )
        }

        updateState(foods, summaries)
    }

    fun saveFood(id: Long, nome: String, descricao: String, unidade: String, diaDaSemana: Int) {
        val cleanName = nome.trim()
        if (cleanName.isBlank()) {
            _messages.tryEmit("Informe o nome do preparo")
            return
        }

        val cleanUnit = unidade.trim().ifBlank { "kg" }
        val validDay = diaDaSemana.takeIf { it in 1..7 } ?: FoodUiModel.TODOS_OS_DIAS

        if (id == 0L) {
            salvarNovoPreparo(cleanName, descricao.trim(), cleanUnit, validDay)
            return
        }

        atualizarPreparo(id, cleanName, descricao.trim(), cleanUnit, validDay)
    }

    private fun salvarNovoPreparo(
        nome: String,
        descricao: String,
        unidadeMedida: String,
        diaDaSemana: Int
    ) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    preparoRepository.inserir(
                        Preparo(nome, descricao, unidadeMedida, diaDaSemana)
                    )
                }
                atualizarPreparosSalvos()
                _messages.emit("Preparo cadastrado")
            } catch (_: Exception) {
                _messages.emit("Não foi possível cadastrar o preparo")
            }
        }
    }

    private fun atualizarPreparo(
        id: Long,
        nome: String,
        descricao: String,
        unidadeMedida: String,
        diaDaSemana: Int
    ) {
        viewModelScope.launch {
            try {
                val atualizado = withContext(Dispatchers.IO) {
                    preparoRepository.atualizar(
                        Preparo(id, nome, descricao, unidadeMedida, diaDaSemana)
                    )
                }
                if (!atualizado) {
                    throw IllegalStateException("Preparo não encontrado")
                }

                atualizarPreparosSalvos()
                _messages.emit("Preparo atualizado")
            } catch (_: Exception) {
                _messages.emit("Não foi possível atualizar o preparo")
            }
        }
    }

    fun deleteFood(food: FoodUiModel) {
        viewModelScope.launch {
            try {
                val excluido = withContext(Dispatchers.IO) {
                    preparoRepository.excluir(food.id)
                }
                if (!excluido) {
                    throw IllegalStateException("Preparo não encontrado")
                }

                val summaries = _uiState.value.productionSummaries.mapNotNull { summary ->
                    val items = summary.items.filterNot { it.food.id == food.id }
                    if (items.isEmpty()) {
                        null
                    } else {
                        summary.copy(
                            items = items,
                            totalSobra = items.sumOf { it.item.quantidadeSobra }
                        )
                    }
                }
                atualizarPreparosSalvos(summaries)
                _messages.emit("Preparo excluído")
            } catch (_: Exception) {
                _messages.emit("Não foi possível excluir o preparo")
            }
        }
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
