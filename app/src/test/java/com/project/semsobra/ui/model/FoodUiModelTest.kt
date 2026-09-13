package com.project.semsobra.ui.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodUiModelTest {

    @Test
    fun `preparo especifico aparece somente no dia programado`() {
        val preparoDeSegunda = FoodUiModel(
            nome = "Língua ao molho",
            descricao = "Língua, molho, cebola e temperos",
            diaDaSemana = 1
        )

        assertTrue(preparoDeSegunda.disponivelNoDia(1))
        assertFalse(preparoDeSegunda.disponivelNoDia(2))
    }

    @Test
    fun `preparo antigo sem dia definido permanece disponivel todos os dias`() {
        val preparoAntigo = FoodUiModel(nome = "Arroz")

        (1..7).forEach { dia ->
            assertTrue(preparoAntigo.disponivelNoDia(dia))
        }
    }

    @Test
    fun `dia de consulta invalido nao retorna preparo`() {
        val preparo = FoodUiModel(nome = "Feijão", diaDaSemana = 3)

        assertFalse(preparo.disponivelNoDia(0))
        assertFalse(preparo.disponivelNoDia(8))
    }
}
