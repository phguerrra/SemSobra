package com.project.semsobra.ui.model

import com.project.semsobra.domain.model.ItemProducao
import com.project.semsobra.domain.model.Preparo
import com.project.semsobra.domain.model.ProducaoDia
import com.project.semsobra.domain.model.ProductionItemDisplay
import com.project.semsobra.domain.model.ProductionSummary
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionItemsTest {
    private val date = LocalDate.of(2026, 11, 9)
    private val arroz = FoodUiModel(1, "Arroz", diasSemanaMask = 1)
    private val feijao = FoodUiModel(2, "Feijão", diasSemanaMask = 1)

    @Test
    fun novaProducaoListaApenasCardapioDaDataSemQuantidadesInventadas() {
        val items = prepararItensProducao(listOf(arroz, feijao.copy(diasSemanaMask = 2)), null, date)
        assertEquals(listOf(1L), items.map { it.food.id })
        assertEquals(null, items.single().quantidadeSalva)
        assertTrue(items.single().podeEditar)
    }

    @Test
    fun producaoAbertaCarregaTotaisSemDuplicarItensDoCardapio() {
        val items = prepararItensProducao(listOf(arroz, feijao), summary(), date)
        assertEquals(listOf(1L, 2L), items.map { it.food.id })
        assertEquals(10.0, items.first().quantidadeSalva!!, 0.0)
        assertEquals(null, items.last().quantidadeSalva)
    }

    @Test
    fun itemRetiradoDoCardapioContinuaVisivelEEditavelNaProducao() {
        val items = prepararItensProducao(listOf(arroz.copy(diasSemanaMask = 0)), summary(), date)
        assertEquals(1L, items.single().food.id)
        assertTrue(items.single().podeEditar)
    }

    @Test
    fun itemInativoContinuaVisivelComQuantidadePreservadaESemEdicao() {
        val item = prepararItensProducao(emptyList(), summary(), date).single()
        assertEquals(1L, item.food.id)
        assertEquals(10.0, item.quantidadeSalva!!, 0.0)
        assertFalse(item.podeEditar)
    }

    @Test
    fun producaoFechadaExibeSomenteItensSalvosSemPermitirEdicao() {
        val items = prepararItensProducao(listOf(arroz, feijao), summary(closed = true), date)
        assertEquals(listOf(1L), items.map { it.food.id })
        assertFalse(items.single().podeEditar)
    }

    @Test
    fun reposicaoRepresentaNovoTotalSemSomarAoValorAnterior() {
        val items = prepararItensProducao(listOf(arroz), summary(), date)
        val result = validarQuantidadesProducao(items, mapOf(1L to "12,5"), "Número inválido")
        assertTrue(result.errors.isEmpty())
        assertEquals(mapOf(1L to 12.5), result.quantities)
    }

    @Test
    fun campoVazioDeItemExistenteImpedeAtualizacao() {
        val items = prepararItensProducao(listOf(arroz), summary(), date)
        val result = validarQuantidadesProducao(items, mapOf(1L to ""), "Informe um número válido")
        assertEquals(mapOf(1L to "Informe um número válido"), result.errors)
        assertTrue(result.quantities.isEmpty())
    }

    @Test
    fun campoVazioDeItemNovoNaoApagaNemZeraItensExistentes() {
        val items = prepararItensProducao(listOf(arroz, feijao), summary(), date)
        val result = validarQuantidadesProducao(items, mapOf(1L to "10", 2L to ""), "Número inválido")
        assertTrue(result.errors.isEmpty())
        assertEquals(mapOf(1L to 10.0), result.quantities)
    }

    @Test
    fun rejeitaZeroValorMuitoPequenoELimiteExcedido() {
        val items = prepararItensProducao(listOf(arroz), summary(), date)
        listOf("0", "0,0001", "10001", "NaN", "abc").forEach { value ->
            val result = validarQuantidadesProducao(items, mapOf(1L to value), "Número inválido")
            assertTrue("Deveria rejeitar $value", 1L in result.errors)
        }
    }

    @Test
    fun itensInativosEFechadosNaoSaoEnviadosMesmoComRascunhoAntigo() {
        val inactive = prepararItensProducao(emptyList(), summary(), date)
        val closed = prepararItensProducao(listOf(arroz), summary(closed = true), date)
        listOf(inactive, closed).forEach { items ->
            assertTrue(validarQuantidadesProducao(items, mapOf(1L to "99"), "Inválido").quantities.isEmpty())
        }
    }

    @Test
    fun atualizacaoDoBancoPreservaDigitacaoEInicializaNovosItens() {
        val result = atualizarRascunhoProducao(
            draft = mapOf(1L to "12,", 2L to "5"),
            previousSaved = mapOf(1L to "10", 2L to "5"),
            currentSaved = mapOf(1L to "11", 2L to "7", 3L to "3")
        )
        assertEquals(mapOf(1L to "12,", 2L to "7", 3L to "3"), result)
    }

    @Test
    fun rascunhoComCampoVazioPermaneceVazioAposRecarregarDados() {
        assertEquals(mapOf(1L to ""), atualizarRascunhoProducao(
            mapOf(1L to ""), mapOf(1L to "10"), mapOf(1L to "10")
        ))
    }

    private fun summary(closed: Boolean = false) = ProductionSummary(
        ProducaoDia(30, date.toString(), date.dayOfWeek.value),
        listOf(ProductionItemDisplay(
            ItemProducao(20, 30, 1, 10.0),
            Preparo(1, "Arroz", "", "kg", 0, 1),
            10.0
        )),
        fechado = closed
    )
}
