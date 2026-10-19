package com.project.semsobra.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.project.semsobra.domain.model.AnalyticsResult
import com.project.semsobra.domain.model.ItemProducao
import com.project.semsobra.domain.model.Preparo
import com.project.semsobra.domain.model.ProducaoDia
import com.project.semsobra.domain.model.ProductionItemDisplay
import com.project.semsobra.domain.model.ProductionSummary
import com.project.semsobra.domain.previsao.PrevisaoPorMediaPonderada
import com.project.semsobra.domain.previsao.model.EntradaPrevisao
import com.project.semsobra.domain.previsao.model.Turno
import com.project.semsobra.ui.SaveStatus
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryEditingTest {
    @get:Rule
    val compose = createComposeRule()

    private val status = mutableStateOf(SaveStatus.IDLE)
    private var saveCalls = 0
    private var savedCustomers = 0
    private var savedItems = emptyList<ItemProducao>()

    @Test
    fun falhaPreservaDigitacaoEPermiteTentarNovamente() {
        showScreen()
        openEditor()
        compose.onNodeWithText("Clientes atendidos").performTextReplacement("120")
        compose.onNodeWithText("Quantidade produzida (kg)").performScrollTo().performTextReplacement("12")
        compose.onNodeWithText("Sobra (kg)").performScrollTo().performTextReplacement("3")
        compose.onNodeWithText("Salvar alterações").performClick()

        compose.runOnIdle { status.value = SaveStatus.ERROR }

        compose.onNodeWithText("Salvar alterações").assertIsDisplayed()
        compose.onNodeWithText("Clientes atendidos").performScrollTo().assertTextContains("120")
        compose.onNodeWithText("Quantidade produzida (kg)").performScrollTo().assertTextContains("12")
        compose.onNodeWithText("Sobra (kg)").performScrollTo().assertTextContains("3")
        compose.onNodeWithText("Salvar alterações").performClick()
        compose.runOnIdle {
            assertEquals(2, saveCalls)
            assertEquals(120, savedCustomers)
            assertEquals(12.0, savedItems.single().quantidadeProduzida, 0.0)
            assertEquals(3.0, savedItems.single().quantidadeSobra, 0.0)
            status.value = SaveStatus.SUCCESS
        }

        compose.onNodeWithText("Salvar alterações").assertDoesNotExist()
    }

    @Test
    fun salvamentoBloqueiaCamposECancelamentoAteReceberResultado() {
        showScreen()
        openEditor()
        compose.onNodeWithText("Salvar alterações").performClick()

        compose.onNodeWithText("Clientes atendidos").assertIsNotEnabled()
        compose.onNodeWithText("Quantidade produzida (kg)").assertIsNotEnabled()
        compose.onNodeWithText("Sobra (kg)").assertIsNotEnabled()
        compose.onNodeWithText("Cancelar").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(1, saveCalls) }

        compose.runOnIdle { status.value = SaveStatus.SUCCESS }
        compose.onNodeWithText("Salvar alterações").assertDoesNotExist()
    }

    @Test
    fun sucessoDeOperacaoAnteriorNaoFechaNovaEdicao() {
        status.value = SaveStatus.SUCCESS
        showScreen()
        openEditor()

        compose.onNodeWithText("Salvar alterações").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(SaveStatus.IDLE, status.value)
            assertEquals(0, saveCalls)
        }
    }

    @Test
    fun permiteCorrigirHorarioJaRegistrado() {
        showScreen(initialShortageTime = "13:40")
        openEditor()
        compose.onNodeWithText("Horário em que acabou").performScrollTo().assertTextContains("13:40")
        compose.onNodeWithText("Horário em que acabou").performTextReplacement("13:55")
        compose.onNodeWithText("Salvar alterações").performClick()

        compose.runOnIdle {
            assertEquals(1, saveCalls)
            assertEquals("13:55", savedItems.single().horarioAcabou)
        }
        compose.onNodeWithText("Horário em que acabou").assertIsNotEnabled()
    }

    @Test
    fun novaFaltaExigeHorarioValidoEPreservaDigitacaoAposFalha() {
        showScreen()
        openEditor()
        compose.onNode(isToggleable()).performScrollTo().performClick()
        compose.onNodeWithText("Salvar alterações").performClick()
        compose.onNodeWithText("Informe o horário em que o preparo acabou").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, saveCalls) }

        compose.onNodeWithText("Horário em que acabou").performScrollTo().performTextReplacement("25:70")
        compose.onNodeWithText("Salvar alterações").performClick()
        compose.onNodeWithText("Informe o horário no formato HH:mm").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, saveCalls) }

        compose.onNodeWithText("Horário em que acabou").performScrollTo().performTextReplacement("13:25")
        compose.onNodeWithText("Salvar alterações").performClick()
        compose.runOnIdle {
            assertEquals(1, saveCalls)
            assertEquals("13:25", savedItems.single().horarioAcabou)
            status.value = SaveStatus.ERROR
        }
        compose.onNodeWithText("Horário em que acabou").performScrollTo().assertTextContains("13:25")
        compose.onNodeWithText("Salvar alterações").assertIsDisplayed()
    }

    @Test
    fun desmarcarFaltaRemoveHorarioDoFechamento() {
        showScreen(initialShortageTime = "13:40")
        openEditor()
        compose.onNode(isToggleable()).performScrollTo().performClick()
        compose.onNodeWithText("Horário em que acabou").assertDoesNotExist()
        compose.onNodeWithText("Salvar alterações").performClick()

        compose.runOnIdle {
            assertEquals(1, saveCalls)
            assertFalse(savedItems.single().acabouAntesDoFim)
            assertNull(savedItems.single().horarioAcabou)
        }
    }

    private fun showScreen(initialShortageTime: String? = null) {
        val today = LocalDate.now()
        val item = ItemProducao(
            id = 20L, producaoDiaId = 30L, alimentoId = 10L,
            quantidadeProduzida = 10.0, quantidadeSobra = 2.0,
            acabouAntesDoFim = initialShortageTime != null,
            horarioAcabou = initialShortageTime
        )
        val summary = ProductionSummary(
            day = ProducaoDia(id = 30L, data = today.toString(), diaDaSemana = today.dayOfWeek.value, clientesAtendidos = 100),
            items = listOf(ProductionItemDisplay(item, Preparo(10L, "Arroz", "", "kg", today.dayOfWeek.value), 8.0)),
            fechado = true
        )
        val forecast = PrevisaoPorMediaPonderada().calcular(
            EntradaPrevisao(today.plusDays(1), Turno.ALMOCO, emptyList())
        )
        compose.setContent {
            MaterialTheme {
                AnalysisScreen(
                    analytics = AnalyticsResult.vazio(),
                    previsaoDemanda = forecast,
                    summaries = listOf(summary),
                    saveStatus = status.value,
                    onSaveHistory = { _, customers, items ->
                        saveCalls++
                        savedCustomers = customers
                        savedItems = items
                        status.value = SaveStatus.SAVING
                    },
                    onSaveHistoryResultConsumed = { status.value = SaveStatus.IDLE }
                )
            }
        }
    }

    private fun openEditor() {
        compose.onNodeWithText("Histórico").performClick()
        compose.onNodeWithText("Reabrir para corrigir").performScrollTo().performClick()
        compose.onNodeWithText("Reabrir fechamento").performClick()
    }
}
