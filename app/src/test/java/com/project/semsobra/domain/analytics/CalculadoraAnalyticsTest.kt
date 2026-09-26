package com.project.semsobra.domain.analytics

import com.project.semsobra.domain.model.Preparo
import com.project.semsobra.domain.model.ProductionDayUiModel
import com.project.semsobra.domain.model.ProductionItemDisplay
import com.project.semsobra.domain.model.ProductionItemUiModel
import com.project.semsobra.domain.model.ProductionSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculadoraAnalyticsTest {
    private val calculadora = CalculadoraAnalytics()
    private val arroz = Preparo(1L, "Arroz", "", "kg", Preparo.TODOS_OS_DIAS)

    @Test
    fun calculaPrevisaoESobrasAPartirDeProducoesFechadas() {
        val resultado = calculadora.calcular(
            preparos = listOf(arroz),
            producoes = listOf(producaoFechada()),
            clientesPrevistos = 50
        )

        assertEquals(1, resultado.forecast.items.size)
        assertEquals(4.0, resultado.forecast.items.single().quantidadeRecomendada, 0.001)
        assertFalse(resultado.forecast.items.single().ajusteSegurancaAplicado)
        assertEquals(2.0, resultado.report.alimentosComMaisSobra.single().quantidade, 0.001)
        assertEquals(2.0, resultado.report.totalSobrasPorUnidade.single().quantidade, 0.001)
    }

    @Test
    fun ignoraProducoesAbertasNosCalculos() {
        val resultado = calculadora.calcular(
            preparos = listOf(arroz),
            producoes = listOf(producaoFechada().copy(fechado = false)),
            clientesPrevistos = 50
        )

        assertTrue(resultado.forecast.items.isEmpty())
        assertTrue(resultado.report.alimentosComMaisSobra.isEmpty())
    }

    @Test
    fun aplicaMargemDeSegurancaEEmiteAlertaQuandoPreparoAcaba() {
        val producao = producaoFechada(acabouAntesDoFim = true, sobra = 0.0)

        val resultado = calculadora.calcular(
            preparos = listOf(arroz),
            producoes = listOf(producao),
            clientesPrevistos = 50
        )

        assertTrue(resultado.forecast.items.single().ajusteSegurancaAplicado)
        assertEquals(5.5, resultado.forecast.items.single().quantidadeRecomendada, 0.001)
        assertEquals("Arroz acabou antes do fim do atendimento.", resultado.forecast.alerts.single())
        assertEquals(1.0, resultado.report.alimentosQueMaisAcabaram.single().quantidade, 0.001)
    }

    private fun producaoFechada(
        acabouAntesDoFim: Boolean = false,
        sobra: Double = 2.0
    ) = ProductionSummary(
        day = ProductionDayUiModel(
            id = 1L,
            data = "2026-09-25",
            diaDaSemana = 5,
            clientesAtendidos = 100
        ),
        items = listOf(
            ProductionItemDisplay(
                item = ProductionItemUiModel(
                    id = 1L,
                    producaoDiaId = 1L,
                    alimentoId = arroz.id,
                    quantidadeProduzida = 10.0,
                    quantidadeSobra = sobra,
                    acabouAntesDoFim = acabouAntesDoFim,
                    horarioAcabou = if (acabouAntesDoFim) "13:30" else null
                ),
                food = arroz,
                consumo = 10.0 - sobra
            )
        ),
        fechado = true
    )
}
