package com.project.semsobra.ui.navigation

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
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
    fun rotaDeProducaoFechadaContinuaRedirecionandoParaAnalises() {
        showApp(productionDate = LocalDate.now(), closed = true)

        navigateTo("Produção")

        navigationItem("Análises").assertIsSelected()
        navigationItem("Produção").assertIsNotSelected()
        compose.onNodeWithText("Resumo").assertIsSelected()
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
        withFood: Boolean = productionDate != null
    ) {
        if (withFood) {
            val foodId = preparos.inserir(Preparo(0, "Arroz", "", "kg", 0, 127))
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

        lateinit var viewModel: SemSobraViewModel
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
