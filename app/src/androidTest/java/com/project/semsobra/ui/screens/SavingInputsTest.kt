package com.project.semsobra.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
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

    private fun field(label: String) = compose.onNodeWithText(label).also {
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(label))
    }
}
