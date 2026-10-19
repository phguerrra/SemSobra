package com.project.semsobra.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.project.semsobra.domain.model.ItemProducao
import com.project.semsobra.domain.model.Preparo
import com.project.semsobra.domain.model.ProducaoDia
import com.project.semsobra.domain.model.ProductionItemDisplay
import com.project.semsobra.domain.model.ProductionSummary
import com.project.semsobra.ui.SaveStatus
import com.project.semsobra.ui.model.FoodUiModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SavingInputsTest {
    @get:Rule val compose = createComposeRule()
    private val status = mutableStateOf(SaveStatus.IDLE)

    @Test
    fun producaoBloqueiaQuantidadeDuranteSalvamentoELiberaAposErro() {
        var saved = emptyMap<Long, Double>()
        compose.setContent {
            MaterialTheme {
                ProductionDayScreen(
                    foods = listOf(FoodUiModel(id = 1, nome = "Arroz")),
                    saveStatus = status.value,
                    onSave = { saved = it; status.value = SaveStatus.SAVING },
                    onGoHome = {}, onGoToClosing = {}
                )
            }
        }
        field("Quantidade produzida (kg)").performTextReplacement("10")
        field("Salvar produção").performClick()
        field("Quantidade produzida (kg)").assertIsNotEnabled().assertTextContains("10")
        field("Salvando…").assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(mapOf(1L to 10.0), saved)
            status.value = SaveStatus.ERROR
        }
        field("Quantidade produzida (kg)").assertIsEnabled().assertTextContains("10")
        field("Quantidade produzida (kg)").performTextReplacement("12")
        field("Salvar produção").performClick()
        compose.runOnIdle { assertEquals(mapOf(1L to 12.0), saved) }
    }

    @Test
    fun fechamentoBloqueiaCamposESelecaoELiberaRascunhoAposErro() {
        val item = ItemProducao(20, 30, 10, 10.0, 2.0, true, "13:40")
        val summary = ProductionSummary(
            ProducaoDia(id = 30, data = "2026-10-18", diaDaSemana = 7, clientesAtendidos = 100),
            listOf(ProductionItemDisplay(item, Preparo(10, "Arroz", "", "kg", 0), 8.0)),
            fechado = false
        )
        var savedItems = emptyList<ItemProducao>()
        var savedCustomers = 0
        compose.setContent {
            MaterialTheme {
                ClosingScreen(
                    summaries = listOf(summary, summary.copy(day = summary.day.copy(id = 31, data = "2026-10-19"))),
                    saveStatus = status.value,
                    onClose = { _, customers, items ->
                        savedCustomers = customers
                        savedItems = items
                        status.value = SaveStatus.SAVING
                    }
                )
            }
        }
        field("Clientes atendidos").performTextReplacement("120")
        field("Sobra (kg)").performTextReplacement("3")
        field("Horário em que acabou").performTextReplacement("13:55")
        field("Salvar fechamento").performClick()
        listOf("Clientes atendidos", "Sobra (kg)", "Horário em que acabou", "19/10/2026", "Salvando…")
            .forEach { field(it).assertIsNotEnabled() }
        compose.onNode(hasScrollAction()).performScrollToNode(isToggleable())
        compose.onNode(isToggleable()).assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(120, savedCustomers)
            assertEquals(item.copy(quantidadeSobra = 3.0, horarioAcabou = "13:55"), savedItems.single())
            status.value = SaveStatus.ERROR
        }
        listOf("Clientes atendidos" to "120", "Sobra (kg)" to "3", "Horário em que acabou" to "13:55")
            .forEach { (label, value) -> field(label).assertIsEnabled().assertTextContains(value) }
        compose.onNode(hasScrollAction()).performScrollToNode(isToggleable())
        compose.onNode(isToggleable()).assertIsEnabled()
        field("19/10/2026").assertIsEnabled()
        field("Salvar fechamento").assertIsEnabled()
    }

    private fun field(label: String) = compose.onNodeWithText(label).also {
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(label))
    }
}
