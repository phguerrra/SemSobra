package com.project.semsobra.ui.navigation

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.project.semsobra.data.backup.SemSobraBackupRepository
import com.project.semsobra.data.local.room.SemSobraDatabase
import com.project.semsobra.data.mapper.HistoricoProducaoMapper
import com.project.semsobra.data.repository.PreparoLocalRepository
import com.project.semsobra.data.repository.ProducaoLocalRepository
import com.project.semsobra.domain.analytics.CalculadoraAnalytics
import com.project.semsobra.domain.model.Preparo
import com.project.semsobra.domain.model.ProducaoDia
import com.project.semsobra.domain.previsao.PrevisaoPorMediaPonderada
import com.project.semsobra.domain.usecase.AlterarDiasPreparoUseCase
import com.project.semsobra.domain.usecase.AtualizarPreparoUseCase
import com.project.semsobra.domain.usecase.CadastrarPreparoUseCase
import com.project.semsobra.domain.usecase.ExcluirPreparoUseCase
import com.project.semsobra.domain.usecase.FecharProducaoUseCase
import com.project.semsobra.domain.usecase.ListarHistoricoProducaoUseCase
import com.project.semsobra.domain.usecase.ListarPreparosUseCase
import com.project.semsobra.domain.usecase.SalvarProducaoUseCase
import com.project.semsobra.domain.usecase.ValidarNomePreparoUseCase
import com.project.semsobra.ui.SemSobraApp
import com.project.semsobra.ui.SemSobraTheme
import com.project.semsobra.ui.SemSobraViewModel
import com.project.semsobra.ui.SaveStatus
import com.project.semsobra.ui.util.dayName
import com.project.semsobra.ui.util.formatDate
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnalysisNavigationTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var database: SemSobraDatabase
    private lateinit var preparos: PreparoLocalRepository
    private lateinit var producoes: ProducaoLocalRepository
    private lateinit var viewModel: SemSobraViewModel
    private val viewModelStore = ViewModelStore()

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            SemSobraDatabase::class.java
        ).allowMainThreadQueries().build()
        preparos = PreparoLocalRepository(database.preparoDao())
        producoes = ProducaoLocalRepository(database)
    }

    @After
    fun teardown() {
        compose.runOnIdle { viewModelStore.clear() }
        database.close()
    }

    @Test
    fun semProducoesPermiteConsultarAnalisesHistoricoEVoltarAoInicio() {
        showApp()

        openAnalysisAndHistory()
        compose.onNodeWithText("O histórico aparecerá depois da primeira produção fechada.")
            .assertIsDisplayed()
        navigateTo("Início")
        compose.onNodeWithText("OPERAÇÃO DE HOJE").assertIsDisplayed()
        assertEquals(0, producoes.listarHistorico().size)
    }

    @Test
    fun historicoAnteriorEstaAcessivelAntesDeIniciarProducaoDeHoje() {
        val yesterday = LocalDate.now().minusDays(1)
        showApp(productionDate = yesterday, closed = true)

        openAnalysisAndHistory()
        assertHistoryDate(yesterday)
        assertEquals(yesterday.toString(), producoes.listarHistorico().single().day.data)
    }

    @Test
    fun producaoAbertaPermiteConsultarHistoricoSemFecharOuAlterarDados() {
        val today = LocalDate.now()
        showApp(productionDate = today)
        val before = database.producaoDao().listarTodasParaBackup()
        val itemsBefore = database.producaoDao().listarTodosItensParaBackup()

        openAnalysisAndHistory()
        assertHistoryDate(today)
        navigateTo("Início")
        navigateTo("Análises")
        compose.onNodeWithText("Histórico").assertIsSelected()

        assertEquals(before, database.producaoDao().listarTodasParaBackup())
        assertEquals(itemsBefore, database.producaoDao().listarTodosItensParaBackup())
    }

    @Test
    fun producaoFechadaMantemAnalisesSelecionadasEPermiteRetornarPeloAtalhoDoInicio() {
        val today = LocalDate.now()
        showApp(productionDate = today, closed = true)
        val before = database.producaoDao().listarTodasParaBackup()

        openAnalysisAndHistory()
        assertHistoryDate(today)
        navigationItem("Produção").assertIsNotSelected()
        navigateTo("Início")
        compose.onNodeWithText("Ver Análise do Dia").performScrollTo().performClick()

        navigationItem("Análises").assertIsSelected()
        compose.onNodeWithText("Histórico").assertIsSelected()
        assertEquals(before, database.producaoDao().listarTodasParaBackup())
    }

    @Test
    fun rotaDeProducaoFechadaPermiteConsultaSemEdicao() {
        showApp(productionDate = LocalDate.now(), closed = true)

        navigateTo("Produção")

        navigationItem("Produção").assertIsSelected()
        compose.onNodeWithText("PRODUÇÃO FECHADA").assertIsDisplayed()
        compose.onNodeWithText("Quantidade produzida (kg)").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Atualizar produção").assertDoesNotExist()
    }

    @Test
    fun alternarPreparosAnalisesEInicioPreservaFormularioEAbaDoHistorico() {
        showApp()
        navigateTo("Preparo")
        compose.onNodeWithText("Nome do preparo").performTextReplacement("Rascunho de preparo")

        openAnalysisAndHistory()
        navigateTo("Início")
        navigateTo("Preparo")
        compose.onNodeWithText("Nome do preparo").assertTextContains("Rascunho de preparo")
        navigateTo("Análises")
        compose.onNodeWithText("Histórico").assertIsSelected()
        navigateTo("Análises")
        compose.onNodeWithText("Histórico").assertIsSelected()
        assertEquals(0, preparos.listarTodos().size)
    }

    @Test
    fun atalhoDeProducaoNoInicioPreservaQuantidadeDigitadaAposConsultarAnalises() {
        showApp(withFood = true)
        navigateTo("Produção")
        compose.onNodeWithText("Quantidade produzida (kg)").performScrollTo()
            .performTextReplacement("8,5")

        openAnalysisAndHistory()
        navigateTo("Início")
        compose.onNodeWithText("Registrar Preparo de Hoje").performScrollTo().performClick()

        navigationItem("Produção").assertIsSelected()
        compose.onNodeWithText("Quantidade produzida (kg)").performScrollTo()
            .assertTextContains("8,5")
        assertEquals(0, producoes.listarHistorico().size)
    }

    @Test
    fun alternarFechamentoEAnalisesPreservaClientesESobrasNaoSalvos() {
        showApp(productionDate = LocalDate.now())
        navigateTo("Fechar")
        compose.onNodeWithText("Clientes atendidos").performScrollTo()
            .performTextReplacement("123")
        compose.onNodeWithText("Sobra (kg)").performScrollTo().performTextReplacement("2,5")

        openAnalysisAndHistory()
        navigateTo("Fechar")

        compose.onNodeWithText("Clientes atendidos").performScrollTo().assertTextContains("123")
        compose.onNodeWithText("Sobra (kg)").performScrollTo().assertTextContains("2,5")
        assertEquals(false, producoes.listarHistorico().single().fechado)
        assertEquals(0, producoes.listarHistorico().single().day.clientesAtendidos)
    }

    @Test
    fun editarProducaoAbertaPreservaRascunhoNaNavegacaoESalvaMesmoId() {
        showApp(productionDate = LocalDate.now())
        val before = producoes.listarHistorico().single()
        navigateTo("Produção")
        compose.onNodeWithText("Quantidade produzida (kg)").performScrollTo()
            .assertTextContains("10").performTextReplacement("12,5")
        navigateTo("Análises")
        navigateTo("Produção")
        compose.onNodeWithText("Quantidade produzida (kg)").performScrollTo().assertTextContains("12,5")
        compose.onNodeWithText("Atualizar produção").performScrollTo().performClick()
        waitForProductionUpdate(12.5)

        val updated = producoes.listarHistorico().single()
        assertEquals(before.day.id, updated.day.id)
        assertEquals(before.items.single().item.id, updated.items.single().item.id)
        navigateTo("Fechar")
        compose.onNodeWithText("Produzido: 12,5 kg").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun fechamentoUsaAtualizacaoENovoItemPreservandoRascunho() {
        showApp(productionDate = LocalDate.now(), withExtraFood = true)
        navigateTo("Fechar")
        compose.onNodeWithText("Clientes atendidos").performScrollTo().performTextReplacement("123")
        compose.onNodeWithText("Sobra (kg)").performScrollTo().performTextReplacement("2,5")
        navigateTo("Produção")
        compose.onNodeWithText("Quantidade produzida (kg)").performScrollTo().performTextReplacement("12")
        compose.onNodeWithText("Quantidade produzida (un)").performScrollTo().performTextReplacement("3")
        compose.onNodeWithText("Atualizar produção").performScrollTo().performClick()
        waitForProductionUpdate(12.0)
        navigateTo("Fechar")
        compose.onNodeWithText("Clientes atendidos").performScrollTo().assertTextContains("123")
        compose.onNodeWithText("Sobra (kg)").performScrollTo().assertTextContains("2,5")
        compose.onNodeWithText("Sobra (un)").performScrollTo().assertTextContains("0")
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Produção atualizada").fetchSemanticsNodes().isEmpty()
        }
        // A lista ganhou um item ao restaurar a rota; role até o botão com um gesto real.
        compose.onNode(hasScrollAction()).performTouchInput { swipeUp() }
        compose.onNodeWithText("Salvar fechamento").assertIsDisplayed().performClick()
        compose.waitUntil(timeoutMillis = 10_000) { viewModel.uiState.value.productionSummaries.single().fechado }

        val closed = producoes.listarHistorico().single()
        assertEquals(123, closed.day.clientesAtendidos)
        assertEquals(2, closed.items.size)
        assertEquals(12.0, closed.items.single { it.food.unidadeMedida == "kg" }.item.quantidadeProduzida, 0.0)
    }

    @Test
    fun viewModelRejeitaAtualizacaoDeProducaoFechadaSemAlterarBanco() {
        showApp(productionDate = LocalDate.now(), closed = true)
        val before = database.producaoDao().listarHistorico()
        val summary = producoes.listarHistorico().single()
        compose.runOnIdle {
            viewModel.saveProductionToday(mapOf(summary.items.single().food.id to 12.0), summary.day.id)
        }
        assertEquals(SaveStatus.ERROR, viewModel.uiState.value.productionSaveStatus)
        assertEquals(before, database.producaoDao().listarHistorico())
    }

    private fun waitForProductionUpdate(quantity: Double) {
        compose.waitUntil(timeoutMillis = 10_000) {
            viewModel.uiState.value.productionSaveStatus == SaveStatus.IDLE &&
                viewModel.uiState.value.productionSummaries.single().items
                    .single { it.food.unidadeMedida == "kg" }.item.quantidadeProduzida == quantity
        }
        compose.waitForIdle()
    }

    @Test
    fun falhaNoBancoPreservaTotalAnteriorERascunhoParaNovaTentativa() {
        showApp(productionDate = LocalDate.now())
        val before = database.producaoDao().listarHistorico()
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER falha_edicao BEFORE UPDATE OF quantidade_produzida ON itens_producao " +
                "BEGIN SELECT RAISE(ABORT, 'Falha simulada'); END"
        )
        navigateTo("Produção")
        compose.onNodeWithText("Quantidade produzida (kg)").performScrollTo().performTextReplacement("12")
        compose.onNodeWithText("Atualizar produção").performScrollTo().performClick()
        compose.waitUntil(timeoutMillis = 10_000) { viewModel.uiState.value.productionSaveStatus == SaveStatus.ERROR }

        assertEquals(before, database.producaoDao().listarHistorico())
        compose.onNodeWithText("Quantidade produzida (kg)").performScrollTo().assertTextContains("12")
        database.openHelper.writableDatabase.execSQL("DROP TRIGGER falha_edicao")
        compose.onNodeWithText("Atualizar produção").performScrollTo().performClick()
        waitForProductionUpdate(12.0)
        assertEquals(before.single().producaoId, producoes.listarHistorico().single().day.id)
    }

    @Test
    fun conflitoNoFechamentoRecarregaProducaoPreservaRascunhoEPermiteTentarNovamente() {
        showApp(productionDate = LocalDate.now())
        navigateTo("Fechar")
        compose.onNodeWithText("Clientes atendidos").performScrollTo().performTextReplacement("123")
        compose.onNodeWithText("Sobra (kg)").performScrollTo().performTextReplacement("2,5")
        val anterior = producoes.listarHistorico().single()
        // Simula uma atualização persistida depois de a interface carregar o fechamento.
        producoes.salvarProducao(anterior.day, mapOf(anterior.items.single().food.id to 12.0))

        compose.onNodeWithText("Salvar fechamento").performScrollTo().performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            viewModel.uiState.value.closingSaveStatus == SaveStatus.ERROR &&
                viewModel.uiState.value.productionSummaries.single().items.single().item.quantidadeProduzida == 12.0
        }

        val aberta = producoes.listarHistorico().single()
        assertEquals(false, aberta.fechado)
        assertEquals(0, aberta.day.clientesAtendidos)
        assertEquals(anterior.items.single().item.id, aberta.items.single().item.id)
        val mensagem = "A produção foi alterada. Os dados atuais foram carregados; confira o fechamento e salve novamente."
        compose.onNodeWithText(mensagem).assertIsDisplayed()
        compose.onNodeWithText("Clientes atendidos").performScrollTo().assertTextContains("123")
        compose.onNodeWithText("Sobra (kg)").performScrollTo().assertTextContains("2,5")
        compose.onNodeWithText("Produzido: 12 kg").performScrollTo().assertIsDisplayed()

        // O snackbar fica sobre a parte inferior da lista enquanto a mensagem é exibida.
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(mensagem).fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithText("Salvar fechamento").performScrollTo().performClick()
        compose.waitUntil(timeoutMillis = 10_000) { viewModel.uiState.value.productionSummaries.single().fechado }
        val fechado = producoes.listarHistorico().single()
        assertEquals(123, fechado.day.clientesAtendidos)
        assertEquals(12.0, fechado.items.single().item.quantidadeProduzida, 0.0)
        assertEquals(2.5, fechado.items.single().item.quantidadeSobra, 0.0)
    }

    private fun navigationItem(label: String) = compose.onNode(hasText(label) and isSelectable())

    private fun navigateTo(label: String) {
        navigationItem(label).performClick()
        compose.waitForIdle()
    }

    private fun openAnalysisAndHistory() {
        navigateTo("Análises")
        navigationItem("Análises").assertIsSelected()
        compose.onNodeWithText("Histórico").performClick().assertIsSelected()
        compose.onNodeWithText("HISTÓRICO DE PRODUÇÃO").assertIsDisplayed()
    }

    private fun assertHistoryDate(date: LocalDate) {
        compose.onNodeWithText("${formatDate(date.toString())} - ${dayName(date.dayOfWeek.value)}")
            .performScrollTo().assertIsDisplayed()
    }

    private fun showApp(
        productionDate: LocalDate? = null,
        closed: Boolean = false,
        withFood: Boolean = productionDate != null,
        withExtraFood: Boolean = false
    ) {
        if (withFood) {
            val foodId = preparos.inserir(Preparo(0, "Arroz", "", "kg", 0, 127))
            if (withExtraFood) preparos.inserir(Preparo(0, "Salada", "", "un", 0, 127))
            if (productionDate != null) {
                val productionId = producoes.salvarProducao(
                    ProducaoDia(data = productionDate.toString(), diaDaSemana = productionDate.dayOfWeek.value),
                    mapOf(foodId to 10.0)
                )
                if (closed) {
                    val item = producoes.listarHistorico().single().items.single().item
                    producoes.fecharProducao(productionId, 100, listOf(item.copy(quantidadeSobra = 2.0)))
                }
            }
        }

        compose.runOnIdle {
            viewModel = ViewModelProvider(viewModelStore, object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val validarNome = ValidarNomePreparoUseCase(preparos)
                    @Suppress("UNCHECKED_CAST")
                    return SemSobraViewModel(
                        alterarDiasPreparo = AlterarDiasPreparoUseCase(preparos),
                        atualizarPreparoUseCase = AtualizarPreparoUseCase(preparos, validarNome),
                        cadastrarPreparo = CadastrarPreparoUseCase(preparos, validarNome),
                        excluirPreparo = ExcluirPreparoUseCase(preparos),
                        listarPreparos = ListarPreparosUseCase(preparos),
                        listarHistoricoProducao = ListarHistoricoProducaoUseCase(producoes),
                        salvarProducao = SalvarProducaoUseCase(producoes),
                        fecharProducao = FecharProducaoUseCase(producoes),
                        motorPrevisao = PrevisaoPorMediaPonderada(),
                        calculadoraAnalytics = CalculadoraAnalytics(),
                        historicoMapper = HistoricoProducaoMapper(),
                        backupRepository = SemSobraBackupRepository(database)
                    ) as T
                }
            })[SemSobraViewModel::class.java]
        }
        compose.setContent { SemSobraTheme { SemSobraApp(viewModel) } }
        compose.waitUntil(timeoutMillis = 10_000) { !viewModel.uiState.value.isLoading }
        compose.waitForIdle()
        assertNull(viewModel.uiState.value.loadError)
        navigationItem("Início").assertIsSelected()
    }
}
