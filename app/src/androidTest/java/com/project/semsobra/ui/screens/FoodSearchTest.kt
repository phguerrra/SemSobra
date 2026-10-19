package com.project.semsobra.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.project.semsobra.ui.SaveStatus
import com.project.semsobra.ui.model.FoodUiModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoodSearchTest {
    @get:Rule val compose = createComposeRule()
    private val restoration = StateRestorationTester(compose)
    private var toggledId: Long? = null

    @Test
    fun buscaFiltraNomeIgnoraCaixaEEspacosERestauraAoLimpar() {
        showScreen()
        scrollTo("Buscar preparo pelo nome")
        compose.onNodeWithText("Buscar preparo pelo nome").performTextReplacement("  ARROZ  ")
        scrollTo("Arroz integral")
        compose.onNodeWithText("Feijão").assertDoesNotExist()
        restoration.emulateSavedInstanceStateRestore()
        scrollTo("Buscar preparo pelo nome")
        compose.onNodeWithText("Buscar preparo pelo nome").assertTextContains("  ARROZ  ")
        compose.onNodeWithText("Limpar busca").performClick()
        scrollTo("Feijão")
    }

    @Test
    fun buscaNoCardapioPermiteSelecionarEInformaQuandoNaoHaResultado() {
        showScreen()
        compose.onNodeWithText("Cardápio por dia").performClick()
        scrollTo("Buscar preparo pelo nome")
        compose.onNodeWithText("Buscar preparo pelo nome").performTextReplacement("arroz")
        scrollTo("Arroz integral")
        compose.onNodeWithText("Feijão").assertDoesNotExist()
        compose.onNodeWithText("Arroz integral").performClick()
        compose.runOnIdle { assertEquals(1L, toggledId) }
        scrollTo("Buscar preparo pelo nome")
        compose.onNodeWithText("Buscar preparo pelo nome").performTextReplacement("batata")
        scrollTo("Nenhum preparo encontrado com esse nome.")
        compose.onNodeWithText("Arroz integral").assertDoesNotExist()
    }

    private fun scrollTo(text: String) {
        compose.onNodeWithTag("preparations_list").performScrollToNode(hasText(text))
        compose.onNodeWithText(text).assertIsDisplayed()
    }

    private fun showScreen() = restoration.setContent {
        MaterialTheme {
            FoodScreen(
                foods = listOf(
                    FoodUiModel(id = 1, nome = "Arroz integral"),
                    FoodUiModel(id = 2, nome = "Feijão", descricao = "Arroz")
                ),
                saveStatus = SaveStatus.IDLE,
                deleteStatus = SaveStatus.IDLE,
                onSave = { _, _, _, _, _ -> },
                onSaveResultConsumed = {},
                onDelete = {},
                onDeleteResultConsumed = {},
                onToggleDay = { food, _, _ -> toggledId = food.id }
            )
        }
    }
}
