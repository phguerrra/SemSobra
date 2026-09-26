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
import com.project.semsobra.domain.analytics.CalculadoraAnalytics
import com.project.semsobra.domain.model.AnalyticsResult
import com.project.semsobra.domain.model.Preparo
import com.project.semsobra.domain.model.ProductionDayUiModel
import com.project.semsobra.domain.model.ProductionItemDisplay
import com.project.semsobra.domain.model.ProductionItemUiModel
import com.project.semsobra.domain.model.ProductionSummary
import com.project.semsobra.domain.previsao.MotorPrevisao
import com.project.semsobra.domain.previsao.PrevisaoPorMediaPonderada
import com.project.semsobra.domain.previsao.model.EntradaPrevisao
import com.project.semsobra.domain.previsao.model.ResultadoPrevisao
import com.project.semsobra.domain.previsao.model.Turno
import com.project.semsobra.domain.repository.ProducaoRepository
import com.project.semsobra.domain.usecase.ExcluirPreparoUseCase
import com.project.semsobra.domain.usecase.FecharProducaoUseCase
import com.project.semsobra.domain.usecase.SalvarProducaoUseCase
import com.project.semsobra.domain.usecase.ValidarNomePreparoUseCase
import com.project.semsobra.ui.model.FoodUiModel
import com.project.semsobra.ui.model.disponivelNoDia
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
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val foods: List<FoodUiModel> = emptyList(),
    val productionSummaries: List<ProductionSummary> = emptyList(),
    val analytics: AnalyticsResult = AnalyticsResult.vazio(),
    val foodSaveStatus: SaveStatus = SaveStatus.IDLE,
    val productionSaveStatus: SaveStatus = SaveStatus.IDLE,
    val foodDeleteStatus: SaveStatus = SaveStatus.IDLE,
    val closingSaveStatus: SaveStatus = SaveStatus.IDLE
)

enum class SaveStatus {
    IDLE,
    SAVING,
    SUCCESS,
    ERROR
}

class SemSobraViewModel(application: Application) : AndroidViewModel(application) {
    private val motorPrevisao: MotorPrevisao = PrevisaoPorMediaPonderada()
    private val calculadoraAnalytics = CalculadoraAnalytics()
    private val historicoMapper = HistoricoProducaoMapper()
    private val preparoRepository = PreparoLocalRepository(application)
    private val excluirPreparo = ExcluirPreparoUseCase(preparoRepository)
    private val validarNomePreparo = ValidarNomePreparoUseCase(preparoRepository)
    private val producaoRepository: ProducaoRepository = ProducaoLocalRepository(application)
    private val salvarProducao = SalvarProducaoUseCase(producaoRepository)
    private val fecharProducao = FecharProducaoUseCase(producaoRepository)

    private val _uiState = MutableStateFlow(
        SemSobraUiState(previsaoDemanda = calcularPrevisaoDemanda(emptyList()))
    )
    val uiState: StateFlow<SemSobraUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    init {
        carregarDadosIniciais()
    }

    fun retryInitialLoad() {
        if (_uiState.value.isLoading) return
        _uiState.value = _uiState.value.copy(isLoading = true, loadError = null)
        carregarDadosIniciais()
    }

    private fun carregarDadosIniciais() {
        viewModelScope.launch {
            try {
                val (preparos, historico) = withContext(Dispatchers.IO) {
                    preparoRepository.listarTodos() to producaoRepository.listarHistorico()
                }
                val foods = preparos.map(Preparo::toFoodUiModel)

                updateState(foods, historico)
                _uiState.value = _uiState.value.copy(isLoading = false, loadError = null)
            } catch (error: SQLiteException) {
                reportPersistenceError("carregar dados iniciais", error)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadError = "Não foi possível carregar os dados salvos"
                )
                emitMessage(UiMessage.Persistence("Não foi possível carregar os dados salvos"))
            } catch (error: RuntimeException) {
                reportUnexpectedError("carregar dados iniciais", error)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadError = "Ocorreu um erro ao carregar os dados"
                )
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
        val foods = preparos.map(Preparo::toFoodUiModel)
        val foodsById = foods.associateBy(FoodUiModel::id)
        val summaries = productionSummaries.map { summary ->
            summary.copy(
                items = summary.items.map { display ->
                    val savedFood = foodsById[display.food.id]
                    if (savedFood == null) display else display.copy(food = savedFood.toPreparo())
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
        val currentMask = _uiState.value.foods.firstOrNull { it.id == id }?.diasSemanaMask ?: 0
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
                        Preparo(id, nome, descricao, unidadeMedida, diaDaSemana, currentMask)
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

    fun toggleFoodDay(food: FoodUiModel, day: Int, selected: Boolean) {
        if (day !in 1..7) return
        val bit = 1 shl (day - 1)
        val newMask = if (selected) food.diasSemanaMask or bit else food.diasSemanaMask and bit.inv()
        viewModelScope.launch {
            try {
                val updated = withContext(Dispatchers.IO) {
                    preparoRepository.atualizarDias(food.id, newMask)
                }
                if (!updated) {
                    emitMessage(UiMessage.NotFound("Preparo não encontrado"))
                    return@launch
                }
                val current = _uiState.value
                val updatedFoods = current.foods.map { savedFood ->
                    if (savedFood.id == food.id) savedFood.copy(diasSemanaMask = newMask)
                    else savedFood
                }
                if (day == current.previsaoDemanda.dataPrevisao.dayOfWeek.value) {
                    updateState(updatedFoods, current.productionSummaries)
                } else {
                    _uiState.value = current.copy(foods = updatedFoods)
                }
                emitMessage(
                    UiMessage.Success(
                        if (selected) "${food.nome} adicionado ao cardápio"
                        else "${food.nome} removido do cardápio"
                    )
                )
            } catch (error: RuntimeException) {
                reportUnexpectedError("atualizar cardápio do dia", error)
                emitMessage(UiMessage.Persistence("Não foi possível atualizar o cardápio"))
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
                    salvarProducao.executar(production, quantitiesToSave)
                }
                updateState(_uiState.value.foods, savedHistory)
                setProductionSaveStatus(SaveStatus.SUCCESS)
                emitMessage(UiMessage.Success("Produção salva"))
            } catch (error: IllegalArgumentException) {
                setProductionSaveStatus(SaveStatus.ERROR)
                emitMessage(UiMessage.Validation(error.message ?: "Quantidade produzida inválida"))
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
        if (_uiState.value.closingSaveStatus == SaveStatus.SAVING) return

        val current = _uiState.value
        val production = current.productionSummaries.firstOrNull { it.day.id == productionDayId }
        if (production == null) {
            tryEmitMessage(UiMessage.Conflict("A produção não foi encontrada"))
            return
        }
        val editingHistory = production.fechado

        val closingById = closingItems.associateBy(ProductionItemUiModel::id)
        val itemsToSave = production.items.map { display ->
            val closingItem = closingById[display.item.id] ?: display.item
            closingItem.copy(
                producaoDiaId = productionDayId,
                alimentoId = display.item.alimentoId,
                quantidadeProduzida = display.item.quantidadeProduzida,
                horarioAcabou = closingItem.horarioAcabou
                    ?.trim()
                    ?.takeIf { closingItem.acabouAntesDoFim }
            )
        }
        setClosingSaveStatus(SaveStatus.SAVING)
        viewModelScope.launch {
            try {
                val savedHistory = withContext(Dispatchers.IO) {
                    fecharProducao.executar(
                        producaoId = productionDayId,
                        clientesAtendidos = clientesAtendidos,
                        itens = itemsToSave
                    )
                }
                updateState(_uiState.value.foods, savedHistory)
                setClosingSaveStatus(SaveStatus.SUCCESS)
                emitMessage(
                    UiMessage.Success(
                        if (editingHistory) "Histórico atualizado" else "Fechamento salvo"
                    )
                )
            } catch (error: IllegalArgumentException) {
                setClosingSaveStatus(SaveStatus.ERROR)
                emitMessage(UiMessage.Validation(error.message ?: "Dados do fechamento inválidos"))
            } catch (error: SQLiteConstraintException) {
                reportPersistenceError("salvar fechamento com dados conflitantes", error)
                setClosingSaveStatus(SaveStatus.ERROR)
                emitMessage(UiMessage.Conflict("Os dados do fechamento entram em conflito com a produção salva"))
            } catch (error: SQLiteException) {
                reportPersistenceError("salvar fechamento", error)
                setClosingSaveStatus(SaveStatus.ERROR)
                emitMessage(UiMessage.Persistence("Não foi possível salvar o fechamento"))
            } catch (error: IllegalStateException) {
                Log.w(LOG_TAG, "Produção ou item não encontrado durante fechamento", error)
                setClosingSaveStatus(SaveStatus.ERROR)
                emitMessage(UiMessage.NotFound("A produção ou um de seus itens não foi encontrado"))
            } catch (error: RuntimeException) {
                reportUnexpectedError("salvar fechamento", error)
                setClosingSaveStatus(SaveStatus.ERROR)
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

    private suspend fun updateState(foods: List<FoodUiModel>, summaries: List<ProductionSummary>) {
        val derivedState = withContext(Dispatchers.Default) {
            val previsaoDemanda = calcularPrevisaoDemanda(summaries)
            val foodsNaDataPrevista = foods.filter {
                it.disponivelNoDia(previsaoDemanda.dataPrevisao.dayOfWeek.value)
            }
            DerivedState(
                previsaoDemanda = previsaoDemanda,
                analytics = calculadoraAnalytics.calcular(
                    preparos = foodsNaDataPrevista.map(FoodUiModel::toPreparo),
                    producoes = summaries,
                    clientesPrevistos = previsaoDemanda.clientesPrevistos
                )
            )
        }
        _uiState.value = _uiState.value.copy(
            previsaoDemanda = derivedState.previsaoDemanda,
            foods = foods,
            productionSummaries = summaries,
            analytics = derivedState.analytics
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

    private fun setClosingSaveStatus(status: SaveStatus) {
        _uiState.value = _uiState.value.copy(closingSaveStatus = status)
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

private data class DerivedState(
    val previsaoDemanda: ResultadoPrevisao,
    val analytics: AnalyticsResult
)

private fun Preparo.toFoodUiModel() = FoodUiModel(
    id = id,
    nome = nome,
    descricao = descricao,
    unidadeMedida = unidadeMedida,
    diaDaSemana = diaDaSemana,
    diasSemanaMask = diasSemanaMask
)

private fun FoodUiModel.toPreparo() = Preparo(
    id,
    nome,
    descricao,
    unidadeMedida,
    diaDaSemana,
    diasSemanaMask
)
