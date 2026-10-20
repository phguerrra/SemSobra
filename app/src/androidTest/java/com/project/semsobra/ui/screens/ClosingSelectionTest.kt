package com.project.semsobra.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.StateRestorationTester
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClosingSelectionTest {
    @get:Rule val compose = createComposeRule()
    private val restoration = StateRestorationTester(compose)

    @Test
    fun cancelarPreservaRascunhoEConfirmarDescartaMesmoAposRecriacao() {
        showScreen()
        field("Clientes atendidos").performTextReplacement("120")
        field("Sobra (kg)").performTextReplacement("3")
        toggleShortage()
        field("Horário em que acabou").performTextReplacement("13:55")
        select("18/10/2026")
        compose.onNodeWithText("Descartar alterações?").assertDoesNotExist()
        select("19/10/2026")
        compose.onNodeWithText("Descartar alterações?").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Continuar editando").performClick()
        field("Clientes atendidos").assertTextContains("120")
        field("Sobra (kg)").assertTextContains("3")
        field("Horário em que acabou").assertTextContains("13:55")
        compose.onNode(isToggleable()).assertIsOn()
        select("19/10/2026")
        compose.onNodeWithText("Descartar e trocar").performClick()
        field("Clientes atendidos").assertTextContains("80")
        field("Sobra (kg)").assertTextContains("1")
        compose.onNodeWithText("Horário em que acabou").assertDoesNotExist()
        select("18/10/2026")
        field("Clientes atendidos").assertTextContains("100")
        field("Sobra (kg)").assertTextContains("2")
    }

    @Test
    fun trocarSemAlteracoesOuAposReverterDigitacaoNaoPedeConfirmacao() {
        showScreen()
        select("19/10/2026")
        compose.onNodeWithText("Descartar alterações?").assertDoesNotExist()
        field("Clientes atendidos").assertTextContains("80")
        select("18/10/2026")
        field("Clientes atendidos").performTextReplacement("120")
        field("Clientes atendidos").performTextReplacement("100")
        field("Sobra (kg)").performTextReplacement("3")
        field("Sobra (kg)").performTextReplacement("2")
        toggleShortage()
        field("Horário em que acabou").performTextReplacement("13:55")
        toggleShortage()
        field("Clientes atendidos").performTextReplacement("0100")
        field("Sobra (kg)").performTextReplacement("2,0")
        select("19/10/2026")
        compose.onNodeWithText("Descartar alterações?").assertDoesNotExist()
        field("Clientes atendidos").assertTextContains("80")
    }

    private fun select(date: String) {
        val selector = hasText(date) and hasClickAction()
        compose.onNode(hasScrollAction()).performScrollToNode(selector)
        compose.onNode(selector).performClick()
    }

    private fun toggleShortage() {
        compose.onNode(hasScrollAction()).performScrollToNode(isToggleable())
        compose.onNode(isToggleable()).performClick()
    }

    private fun field(label: String) = compose.onNodeWithText(label).also {
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(label))
    }

    private fun showScreen() = restoration.setContent {
        MaterialTheme {
            ClosingScreen(
                summaries = listOf(
                    summary(30, "2026-10-18", 100, 2.0),
                    summary(31, "2026-10-19", 80, 1.0)
                ),
                saveStatus = SaveStatus.IDLE,
                onClose = { _, _, _ -> }
            )
        }
    }

    private fun summary(id: Long, date: String, customers: Int, leftover: Double) = ProductionSummary(
        ProducaoDia(id = id, data = date, diaDaSemana = java.time.LocalDate.parse(date).dayOfWeek.value, clientesAtendidos = customers),
        listOf(ProductionItemDisplay(ItemProducao(id, id, 10, 10.0, leftover), Preparo(10, "Arroz", "", "kg", 0), 10.0 - leftover)),
        fechado = false
    )
}
