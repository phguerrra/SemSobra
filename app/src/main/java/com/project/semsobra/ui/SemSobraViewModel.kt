package com.project.semsobra.ui

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.project.semsobra.data.mapper.HistoricoProducaoMapper
import com.project.semsobra.data.repository.PreparoLocalRepository
import com.project.semsobra.data.repository.ProducaoLocalRepository
import com.project.semsobra.domain.model.Preparo
import com.project.semsobra.domain.model.QuantityPolicy
import com.project.semsobra.domain.previsao.MotorPrevisao
import com.project.semsobra.domain.previsao.PrevisaoPorMediaPonderada
import com.project.semsobra.domain.previsao.model.EntradaPrevisao
import com.project.semsobra.domain.previsao.model.ResultadoPrevisao
import com.project.semsobra.domain.previsao.model.Turno
import com.project.semsobra.domain.usecase.ExcluirPreparoUseCase
import com.project.semsobra.domain.usecase.ValidarDadosProducaoUseCase
import com.project.semsobra.domain.usecase.ValidarNomePreparoUseCase
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
import com.project.semsobra.ui.model.QuantityByUnit
import com.project.semsobra.ui.model.ReportSummary
import com.project.semsobra.ui.model.UiEvent
import com.project.semsobra.ui.model.UiMessage
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
    val analytics: AnalyticsResult = emptyAnalytics(),
    val foodSaveStatus: SaveStatus = SaveStatus.IDLE,
    val productionSaveStatus: SaveStatus = SaveStatus.IDLE,
    val foodDeleteStatus: SaveStatus = SaveStatus.IDLE
)

enum class SaveStatus {
    IDLE,
    SAVING,
    SUCCESS,
    ERROR
}

class SemSobraViewModel(application: Application) : AndroidViewModel(application) {
    private val motorPrevisao: MotorPrevisao = PrevisaoPorMediaPonderada()
    private val historicoMapper = HistoricoProducaoMapper()
    private val preparoRepository = PreparoLocalRepository(application)
    private val excluirPreparo = ExcluirPreparoUseCase(preparoRepository)
    private val validarDadosProducao = ValidarDadosProducaoUseCase()
    private val validarNomePreparo = ValidarNomePreparoUseCase(preparoRepository)
    private val producaoRepository = ProducaoLocalRepository(application)

    private val _uiState = MutableStateFlow(
        SemSobraUiState(previsaoDemanda = calcularPrevisaoDemanda(emptyList()))
    )
    val uiState: StateFlow<SemSobraUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    init {
        carregarDadosIniciais()
    }

    private fun carregarDadosIniciais() {
        viewModelScope.launch {
            try {
                val (preparos, historico) = withContext(Dispatchers.IO) {
                    preparoRepository.listarTodos() to producaoRepository.listarHistorico()
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

                updateState(foods, historico)
            } catch (error: SQLiteException) {
                reportPersistenceError("carregar dados iniciais", error)
                emitMessage(UiMessage.Persistence("Não foi possível carregar os dados salvos"))
            } catch (error: RuntimeException) {
                reportUnexpectedError("carregar dados iniciais", error)
                emitMessage(UiMessage.Persistence("Ocorreu um erro ao carregar os dados"))
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
        if (_uiState.value.foodSaveStatus == SaveStatus.SAVING) return

        val cleanName = nome.trim()
        if (cleanName.isBlank()) {
            tryEmitMessage(UiMessage.Validation("Informe o nome do preparo"))
            return
        }

        val cleanUnit = unidade.trim().ifBlank { "kg" }
        val validDay = diaDaSemana.takeIf { it in 1..7 } ?: FoodUiModel.TODOS_OS_DIAS
        setFoodSaveStatus(SaveStatus.SAVING)

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
                val nomeDisponivel = withContext(Dispatchers.IO) {
                    validarNomePreparo.estaDisponivel(nome, diaDaSemana, null)
                }
                if (!nomeDisponivel) {
                    setFoodSaveStatus(SaveStatus.ERROR)
                    emitMessage(UiMessage.Conflict("Já existe um preparo com esse nome neste dia"))
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    preparoRepository.inserir(
                        Preparo(nome, descricao, unidadeMedida, diaDaSemana)
                    )
                }
                atualizarPreparosSalvos()
                setFoodSaveStatus(SaveStatus.SUCCESS)
                emitMessage(UiMessage.Success("Preparo cadastrado"))
            } catch (error: SQLiteConstraintException) {
                reportPersistenceError("cadastrar preparo com nome duplicado", error)
                setFoodSaveStatus(SaveStatus.ERROR)
                emitMessage(UiMessage.Conflict("Já existe um preparo com esse nome neste dia"))
            } catch (error: SQLiteException) {
                reportPersistenceError("cadastrar preparo", error)
                setFoodSaveStatus(SaveStatus.ERROR)
                emitMessage(UiMessage.Persistence("Não foi possível cadastrar o preparo"))
            } catch (error: IllegalArgumentException) {
                Log.w(LOG_TAG, "Dados inválidos ao cadastrar preparo", error)
                setFoodSaveStatus(SaveStatus.ERROR)
                emitMessage(UiMessage.Validation(error.message ?: "Dados do preparo inválidos"))
            } catch (error: RuntimeException) {
                reportUnexpectedError("cadastrar preparo", error)
                setFoodSaveStatus(SaveStatus.ERROR)
                emitMessage(UiMessage.Persistence("Ocorreu um erro ao cadastrar o preparo"))
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
                val nomeDisponivel = withContext(Dispatchers.IO) {
                    validarNomePreparo.estaDisponivel(nome, diaDaSemana, id)
                }
                if (!nomeDisponivel) {
                    setFoodSaveStatus(SaveStatus.ERROR)
                    emitMessage(UiMessage.Conflict("Já existe um preparo com esse nome neste dia"))
                    return@launch
                }

                val atualizado = withContext(Dispatchers.IO) {
                    preparoRepository.atualizar(
                        Preparo(id, nome, descricao, unidadeMedida, diaDaSemana)
                    )
                }
                if (!atualizado) {
                    throw IllegalStateException("Preparo não encontrado")
                }

                atualizarPreparosSalvos()
                setFoodSaveStatus(SaveStatus.SUCCESS)
                emitMessage(UiMessage.Success("Preparo atualizado"))
            } catch (error: SQLiteConstraintException) {
                reportPersistenceError("atualizar preparo com nome duplicado", error)
                setFoodSaveStatus(SaveStatus.ERROR)
                emitMessage(UiMessage.Conflict("Já existe um preparo com esse nome neste dia"))
            } catch (error: SQLiteException) {
                reportPersistenceError("atualizar preparo", error)
                setFoodSaveStatus(SaveStatus.ERROR)
                emitMessage(UiMessage.Persistence("Não foi possível atualizar o preparo"))
            } catch (error: IllegalArgumentException) {
                Log.w(LOG_TAG, "Dados inválidos ao atualizar preparo", error)
                setFoodSaveStatus(SaveStatus.ERROR)
                emitMessage(UiMessage.Validation(error.message ?: "Dados do preparo inválidos"))
            } catch (error: IllegalStateException) {
                Log.w(LOG_TAG, "Preparo não encontrado durante atualização", error)
                setFoodSaveStatus(SaveStatus.ERROR)
                emitMessage(UiMessage.NotFound("Preparo não encontrado"))
            } catch (error: RuntimeException) {
                reportUnexpectedError("atualizar preparo", error)
                setFoodSaveStatus(SaveStatus.ERROR)
                emitMessage(UiMessage.Persistence("Ocorreu um erro ao atualizar o preparo"))
            }
        }
    }

    fun deleteFood(food: FoodUiModel) {
        if (_uiState.value.foodDeleteStatus == SaveStatus.SAVING) return
        setFoodDeleteStatus(SaveStatus.SAVING)

        viewModelScope.launch {
            try {
                val resultado = withContext(Dispatchers.IO) {
                    excluirPreparo.executar(food.id)
                }
                when (resultado) {
                    ExcluirPreparoUseCase.Resultado.NAO_ENCONTRADO -> {
                        setFoodDeleteStatus(SaveStatus.ERROR)
                        emitMessage(UiMessage.NotFound("Preparo não encontrado"))
                        return@launch
                    }
                    ExcluirPreparoUseCase.Resultado.EXCLUIDO -> {
                        atualizarPreparosSalvos()
                        setFoodDeleteStatus(SaveStatus.SUCCESS)
                        emitMessage(UiMessage.Success("Preparo excluído"))
                    }
                    ExcluirPreparoUseCase.Resultado.INATIVADO_POR_HISTORICO -> {
                        atualizarPreparosSalvos()
                        setFoodDeleteStatus(SaveStatus.SUCCESS)
                        emitMessage(UiMessage.Success("Este preparo possui histórico e foi inativado"))
                    }
                }
            } catch (error: SQLiteException) {
                reportPersistenceError("excluir ou inativar preparo", error)
                setFoodDeleteStatus(SaveStatus.ERROR)
                emitMessage(UiMessage.Persistence("Não foi possível excluir o preparo"))
            } catch (error: RuntimeException) {
                reportUnexpectedError("excluir ou inativar preparo", error)
                setFoodDeleteStatus(SaveStatus.ERROR)
                emitMessage(UiMessage.Persistence("Ocorreu um erro ao excluir o preparo"))
            }
        }
    }

    fun saveProductionToday(quantitiesByFoodId: Map<Long, Double>) {
        if (_uiState.value.productionSaveStatus == SaveStatus.SAVING) return

        val current = _uiState.value
        if (quantitiesByFoodId.isEmpty()) {
            tryEmitMessage(UiMessage.Validation("Informe ao menos uma quantidade maior que zero"))
            return
        }
        try {
            quantitiesByFoodId.values.forEach(validarDadosProducao::validarQuantidadeProduzida)
        } catch (error: IllegalArgumentException) {
            tryEmitMessage(UiMessage.Validation(error.message ?: "Quantidade produzida inválida"))
            return
        }

        val today = LocalDate.now()
        val availableFoodIds = current.foods
            .filter { it.disponivelNoDia(today.dayOfWeek.value) }
            .map(FoodUiModel::id)
            .toSet()
        val quantitiesToSave = quantitiesByFoodId.filterKeys(availableFoodIds::contains)
        if (quantitiesToSave.isEmpty()) {
            tryEmitMessage(UiMessage.Conflict("Os preparos informados não estão mais disponíveis"))
            return
        }

        val production = ProductionDayUiModel(
            data = today.toString(),
            diaDaSemana = today.dayOfWeek.value
        )
        setProductionSaveStatus(SaveStatus.SAVING)
        viewModelScope.launch {
            try {
                val savedHistory = withContext(Dispatchers.IO) {
                    producaoRepository.salvarProducao(production, quantitiesToSave)
                    producaoRepository.listarHistorico()
                }
                updateState(_uiState.value.foods, savedHistory)
                setProductionSaveStatus(SaveStatus.SUCCESS)
                emitMessage(UiMessage.Success("Produção salva"))
            } catch (error: SQLiteConstraintException) {
                reportPersistenceError("salvar produção com dados conflitantes", error)
                setProductionSaveStatus(SaveStatus.ERROR)
                emitMessage(UiMessage.Conflict("A produção possui dados conflitantes; atualize a tela e tente novamente"))
            } catch (error: SQLiteException) {
                reportPersistenceError("salvar produção", error)
                setProductionSaveStatus(SaveStatus.ERROR)
                emitMessage(UiMessage.Persistence("Não foi possível salvar a produção"))
            } catch (error: RuntimeException) {
                reportUnexpectedError("salvar produção", error)
                setProductionSaveStatus(SaveStatus.ERROR)
                emitMessage(UiMessage.Persistence("Ocorreu um erro ao salvar a produção"))
            }
        }
    }

    fun closeProduction(
        productionDayId: Long,
        clientesAtendidos: Int,
        closingItems: List<ProductionItemUiModel>
    ) {
        try {
            validarDadosProducao.validarClientesAtendidos(clientesAtendidos)
        } catch (error: IllegalArgumentException) {
            tryEmitMessage(UiMessage.Validation(error.message ?: "Quantidade de clientes inválida"))
            return
        }

        val current = _uiState.value
        val production = current.productionSummaries.firstOrNull {
            it.day.id == productionDayId && !it.fechado
        }
        if (production == null) {
            tryEmitMessage(UiMessage.Conflict("A produção não foi encontrada ou já está fechada"))
            return
        }

        val closingById = closingItems.associateBy(ProductionItemUiModel::id)
        val itemsToSave = try {
            production.items.map { display ->
                val closingItem = closingById[display.item.id] ?: display.item
                validarDadosProducao.validarFechamentoDoItem(
                    display.item.quantidadeProduzida,
                    closingItem.quantidadeSobra,
                    closingItem.acabouAntesDoFim,
                    closingItem.horarioAcabou
                )
                closingItem.copy(
                    producaoDiaId = productionDayId,
                    alimentoId = display.item.alimentoId,
                    quantidadeProduzida = display.item.quantidadeProduzida,
                    horarioAcabou = closingItem.horarioAcabou
                        ?.trim()
                        ?.takeIf { closingItem.acabouAntesDoFim }
                )
            }
        } catch (error: IllegalArgumentException) {
            tryEmitMessage(UiMessage.Validation(error.message ?: "Dados do fechamento inválidos"))
            return
        }
        viewModelScope.launch {
            try {
                val savedHistory = withContext(Dispatchers.IO) {
                    producaoRepository.fecharProducao(
                        producaoId = productionDayId,
                        clientesAtendidos = clientesAtendidos,
                        itens = itemsToSave
                    )
                    producaoRepository.listarHistorico()
                }
                updateState(_uiState.value.foods, savedHistory)
                emitMessage(UiMessage.Success("Fechamento salvo"))
            } catch (error: SQLiteConstraintException) {
                reportPersistenceError("salvar fechamento com dados conflitantes", error)
                emitMessage(UiMessage.Conflict("Os dados do fechamento entram em conflito com a produção salva"))
            } catch (error: SQLiteException) {
                reportPersistenceError("salvar fechamento", error)
                emitMessage(UiMessage.Persistence("Não foi possível salvar o fechamento"))
            } catch (error: IllegalStateException) {
                Log.w(LOG_TAG, "Produção ou item não encontrado durante fechamento", error)
                emitMessage(UiMessage.NotFound("A produção ou um de seus itens não foi encontrado"))
            } catch (error: RuntimeException) {
                reportUnexpectedError("salvar fechamento", error)
                emitMessage(UiMessage.Persistence("Ocorreu um erro ao salvar o fechamento"))
            }
        }
    }

    private fun tryEmitMessage(message: UiMessage) {
        _events.tryEmit(UiEvent.ShowMessage(message))
    }

    private suspend fun emitMessage(message: UiMessage) {
        _events.emit(UiEvent.ShowMessage(message))
    }

    private fun reportPersistenceError(operation: String, error: SQLiteException) {
        Log.e(LOG_TAG, "Falha de persistência ao $operation", error)
    }

    private fun reportUnexpectedError(operation: String, error: RuntimeException) {
        Log.e(LOG_TAG, "Falha inesperada ao $operation", error)
    }

    private fun updateState(foods: List<FoodUiModel>, summaries: List<ProductionSummary>) {
        val previsaoDemanda = calcularPrevisaoDemanda(summaries)
        val foodsNaDataPrevista = foods.filter {
            it.disponivelNoDia(previsaoDemanda.dataPrevisao.dayOfWeek.value)
        }
        _uiState.value = _uiState.value.copy(
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

    fun consumeFoodSaveResult() {
        if (_uiState.value.foodSaveStatus != SaveStatus.SAVING) {
            setFoodSaveStatus(SaveStatus.IDLE)
        }
    }

    fun consumeProductionSaveResult() {
        if (_uiState.value.productionSaveStatus != SaveStatus.SAVING) {
            setProductionSaveStatus(SaveStatus.IDLE)
        }
    }

    fun consumeFoodDeleteResult() {
        if (_uiState.value.foodDeleteStatus != SaveStatus.SAVING) {
            setFoodDeleteStatus(SaveStatus.IDLE)
        }
    }

    private fun setFoodSaveStatus(status: SaveStatus) {
        _uiState.value = _uiState.value.copy(foodSaveStatus = status)
    }

    private fun setProductionSaveStatus(status: SaveStatus) {
        _uiState.value = _uiState.value.copy(productionSaveStatus = status)
    }

    private fun setFoodDeleteStatus(status: SaveStatus) {
        _uiState.value = _uiState.value.copy(foodDeleteStatus = status)
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

    private companion object {
        const val LOG_TAG = "SemSobraViewModel"
    }
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
                quantidadeRecomendada = QuantityPolicy.normalize(
                    averagePerCustomer * forecastCustomers * safetyFactor
                ),
                consumoMedioPorCliente = QuantityPolicy.normalize(averagePerCustomer),
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
            val quantity = QuantityPolicy.sum(items.map { it.item.quantidadeSobra })
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

    val totalSobrasPorUnidade = allClosedItems
        .groupBy { it.food.unidadeMedida.trim().lowercase() }
        .map { (unit, items) ->
            QuantityByUnit(
                unidadeMedida = unit,
                quantidade = QuantityPolicy.sum(items.map { it.item.quantidadeSobra })
            )
        }
        .filter { it.quantidade > 0.0 }
        .sortedBy(QuantityByUnit::unidadeMedida)

    return AnalyticsResult(
        forecast = ForecastResult(
            clientesPrevistos = forecastCustomers,
            items = forecastItems,
            alerts = alerts
        ),
        report = ReportSummary(
            totalSobrasPorUnidade = totalSobrasPorUnidade,
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
        totalSobrasPorUnidade = emptyList(),
        alimentosComMaisSobra = emptyList(),
        alimentosQueMaisAcabaram = emptyList()
    )
)
