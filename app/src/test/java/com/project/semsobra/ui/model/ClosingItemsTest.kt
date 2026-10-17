package com.project.semsobra.ui.model

import com.project.semsobra.domain.model.ItemProducao
import com.project.semsobra.domain.model.Preparo
import com.project.semsobra.domain.model.ProducaoDia
import com.project.semsobra.domain.model.ProductionItemDisplay
import com.project.semsobra.domain.model.ProductionSummary
import com.project.semsobra.domain.usecase.ValidarDadosProducaoUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ClosingItemsTest {
    private val itemSalvo = ItemProducao(
        id = 20L,
        producaoDiaId = 30L,
        alimentoId = 10L,
        quantidadeProduzida = 10.0,
        quantidadeSobra = 2.0
    )

    @Test
    fun `correcao do historico preserva a quantidade produzida digitada`() {
        listOf(8.0, 12.0).forEach { quantidadeEditada ->
            val editado = itemSalvo.copy(quantidadeProduzida = quantidadeEditada)

            val resultado = prepararItensFechamento(producao(fechada = true), listOf(editado)).single()

            assertEquals(quantidadeEditada, resultado.quantidadeProduzida, 0.0)
            assertEquals(itemSalvo.quantidadeSobra, resultado.quantidadeSobra, 0.0)
            assertEquals(10.0, itemSalvo.quantidadeProduzida, 0.0)
        }
    }

    @Test
    fun `primeiro fechamento mantem a quantidade da producao registrada`() {
        val fechamento = itemSalvo.copy(quantidadeProduzida = 12.0, quantidadeSobra = 3.0)

        val resultado = prepararItensFechamento(producao(fechada = false), listOf(fechamento)).single()

        assertEquals(10.0, resultado.quantidadeProduzida, 0.0)
        assertEquals(3.0, resultado.quantidadeSobra, 0.0)
    }

    @Test
    fun `correcao menor que a sobra nao e ocultada pelo valor antigo`() {
        val editado = itemSalvo.copy(quantidadeProduzida = 1.0, quantidadeSobra = 2.0)
        val resultado = prepararItensFechamento(producao(fechada = true), listOf(editado)).single()

        assertThrows(IllegalArgumentException::class.java) {
            ValidarDadosProducaoUseCase().validarSobra(
                resultado.quantidadeProduzida,
                resultado.quantidadeSobra
            )
        }
    }

    @Test
    fun `quantidade corrigida que arredonda para zero continua sujeita a validacao`() {
        val editado = itemSalvo.copy(quantidadeProduzida = 0.0001, quantidadeSobra = 0.0)
        val resultado = prepararItensFechamento(producao(fechada = true), listOf(editado)).single()

        assertThrows(IllegalArgumentException::class.java) {
            ValidarDadosProducaoUseCase().validarQuantidadeProduzida(resultado.quantidadeProduzida)
        }
    }

    @Test
    fun `horario de falta e preservado sem espacos`() {
        val editado = itemSalvo.copy(acabouAntesDoFim = true, horarioAcabou = " 13:40 ")

        val resultado = prepararItensFechamento(producao(fechada = true), listOf(editado)).single()

        assertEquals("13:40", resultado.horarioAcabou)
    }

    @Test
    fun `remover falta tambem remove horario antigo`() {
        val editado = itemSalvo.copy(acabouAntesDoFim = false, horarioAcabou = "13:40")

        val resultado = prepararItensFechamento(producao(fechada = true), listOf(editado)).single()

        assertNull(resultado.horarioAcabou)
    }

    private fun producao(fechada: Boolean) = ProductionSummary(
        day = ProducaoDia(id = 30L, data = "2026-10-16", diaDaSemana = 5),
        items = listOf(
            ProductionItemDisplay(
                item = itemSalvo,
                food = Preparo(10L, "Arroz", "", "kg", 5),
                consumo = 8.0
            )
        ),
        fechado = fechada
    )
}
