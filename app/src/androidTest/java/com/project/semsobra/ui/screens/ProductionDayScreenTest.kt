package com.project.semsobra.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.project.semsobra.domain.model.ItemProducao
import com.project.semsobra.domain.model.Preparo
import com.project.semsobra.domain.model.ProducaoDia
import com.project.semsobra.domain.model.ProductionItemDisplay
import com.project.semsobra.domain.model.ProductionSummary
import com.project.semsobra.ui.SaveStatus
import com.project.semsobra.ui.model.FoodUiModel
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductionDayScreenTest {
    @get:Rule val compose = createComposeRule()
    private val restoration = StateRestorationTester(compose)
    private val status = mutableStateOf(SaveStatus.IDLE)
    private val production = mutableStateOf<ProductionSummary?>(null)
    private var calls = 0
    private var saved = emptyMap<Long, Double>()

    @Test
    fun novaProducaoMantemCriacaoComItensOpcionaisVazios() {
        showScreen(existing = false)
        field("Quantidade produzida (kg)").performTextReplacement("8,5")
        field("Salvar produção").performClick()
        compose.runOnIdle {
            assertEquals(1, calls)
            assertEquals(mapOf(1L to 8.5), saved)
        }
    }

    @Test
    fun producaoAbertaCarregaQuantidadeEEnviaNovoTotal() {
        showScreen()
        compose.onNodeWithText("PRODUÇÃO ABERTA").assertIsDisplayed()
        field("Quantidade produzida (kg)").assertTextContains("10")
            .performTextReplacement("12")
        field("Atualizar produção").performClick()
        compose.runOnIdle {
            assertEquals(1, calls)
            assertEquals(mapOf(1L to 12.0), saved)
        }
    }

    @Test
    fun apagarQuantidadeExistenteNaoSalvaNemRemoveItem() {
        showScreen()
        field("Quantidade produzida (kg)").performTextReplacement("")
        field("Atualizar produção").performClick()
        compose.onNodeWithText("Informe um número válido").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, calls) }
    }

    @Test
    fun falhaPreservaRascunhoInclusiveAposRestauracaoDaTela() {
        showScreen()
        field("Quantidade produzida (kg)").performTextReplacement("12,5")
        field("Atualizar produção").performClick()
        field("Quantidade produzida (kg)").assertIsNotEnabled()
        compose.runOnIdle { status.value = SaveStatus.ERROR }
        restoration.emulateSavedInstanceStateRestore()

        field("Quantidade produzida (kg)").assertIsEnabled().assertTextContains("12,5")
        field("Atualizar produção").performClick()
        compose.runOnIdle {
            assertEquals(2, calls)
            assertEquals(mapOf(1L to 12.5), saved)
        }
    }

    @Test
    fun sucessoDeAtualizacaoMantemTelaAbertaComTotalPersistido() {
        showScreen()
        field("Quantidade produzida (kg)").performTextReplacement("12")
        field("Atualizar produção").performClick()
        compose.runOnIdle {
            production.value = summary(quantity = 12.0)
            status.value = SaveStatus.SUCCESS
        }

        field("Atualizar produção").assertIsEnabled()
        field("Quantidade produzida (kg)").assertTextContains("12")
        compose.runOnIdle { assertEquals(SaveStatus.IDLE, status.value) }
    }

    @Test
    fun producaoFechadaPermiteSomenteConsulta() {
        showScreen(closed = true)
        compose.onNodeWithText("PRODUÇÃO FECHADA").assertIsDisplayed()
        field("Quantidade produzida (kg)").assertIsNotEnabled().assertTextContains("10")
        compose.onNodeWithText("Atualizar produção").assertDoesNotExist()
        compose.onNodeWithText("Salvar produção").assertDoesNotExist()
        field("Consultar fechamento").assertIsEnabled()
        compose.runOnIdle { assertEquals(0, calls) }
    }

    @Test
    fun fechamentoInicializaNovosItensSemApagarRascunhoExistente() {
        val summaries = mutableStateOf(listOf(summary()))
        var closedItems = emptyList<ItemProducao>()
        var customers = 0
        compose.setContent {
            MaterialTheme {
                ClosingScreen(summaries.value, SaveStatus.IDLE, { _, count, items ->
                    customers = count
                    closedItems = items
                })
            }
        }
        field("Clientes atendidos").performTextReplacement("123")
        field("Sobra (kg)").performTextReplacement("2,5")
        compose.runOnIdle {
            val updated = summary(quantity = 12.0)
            summaries.value = listOf(updated.copy(items = updated.items + ProductionItemDisplay(
                ItemProducao(21, 30, 2, 3.0), Preparo(2, "Salada", "", "un", 0), 3.0
            )))
        }

        field("Clientes atendidos").assertTextContains("123")
        field("Sobra (kg)").assertTextContains("2,5")
        field("Sobra (un)").assertTextContains("0")
        field("Salvar fechamento").performClick()
        compose.runOnIdle {
            assertEquals(123, customers)
            assertEquals(setOf(20L, 21L), closedItems.map { it.id }.toSet())
            assertEquals(12.0, closedItems.single { it.id == 20L }.quantidadeProduzida, 0.0)
        }
    }

    private fun showScreen(existing: Boolean = true, closed: Boolean = false) {
        production.value = if (existing) summary().copy(fechado = closed) else null
        restoration.setContent {
            MaterialTheme {
                ProductionDayScreen(
                    foods = listOf(FoodUiModel(1, "Arroz"), FoodUiModel(2, "Salada", unidadeMedida = "un")),
                    saveStatus = status.value,
                    production = production.value,
                    onSave = { saved = it; calls++; status.value = SaveStatus.SAVING },
                    onSaveResultConsumed = { status.value = SaveStatus.IDLE },
                    onGoHome = {}, onGoToClosing = {}
                )
            }
        }
    }

    private fun field(label: String) = compose.onNodeWithText(label).also { it.performScrollTo() }

    private fun summary(quantity: Double = 10.0): ProductionSummary {
        val today = LocalDate.now()
        return ProductionSummary(
            ProducaoDia(30, today.toString(), today.dayOfWeek.value),
            listOf(ProductionItemDisplay(
                ItemProducao(20, 30, 1, quantity), Preparo(1, "Arroz", "", "kg", 0), quantity
            )),
            fechado = false
        )
    }
}
