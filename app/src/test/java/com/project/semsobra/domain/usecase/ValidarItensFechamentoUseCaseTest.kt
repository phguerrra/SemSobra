package com.project.semsobra.domain.usecase

import com.project.semsobra.domain.model.ItemProducao
import org.junit.Assert.assertThrows
import org.junit.Test

class ValidarItensFechamentoUseCaseTest {
    private val validar = ValidarItensFechamentoUseCase()
    private val arroz = ItemProducao(id = 10, producaoDiaId = 30, alimentoId = 1, quantidadeProduzida = 10.0)
    private val feijao = ItemProducao(id = 20, producaoDiaId = 30, alimentoId = 2, quantidadeProduzida = 5.0)
    private val esperados = mapOf(10L to 1L, 20L to 2L)

    @Test
    fun aceitaListaCompletaMesmoEmOutraOrdem() {
        validar.executar(30, listOf(feijao, arroz), esperados)
    }

    @Test
    fun rejeitaProducaoSemIdValido() {
        assertThrows(IllegalArgumentException::class.java) {
            validar.executar(0, listOf(arroz, feijao), esperados)
        }
    }

    @Test
    fun rejeitaListaVazia() {
        assertThrows(IllegalArgumentException::class.java) {
            validar.executar(30, emptyList(), esperados)
        }
    }

    @Test
    fun rejeitaItemSemIdValido() {
        assertThrows(IllegalArgumentException::class.java) {
            validar.executar(30, listOf(arroz.copy(id = 0), feijao), esperados)
        }
    }

    @Test
    fun rejeitaPreparoSemIdValido() {
        assertThrows(IllegalArgumentException::class.java) {
            validar.executar(30, listOf(arroz.copy(alimentoId = 0), feijao), esperados)
        }
    }

    @Test
    fun rejeitaItemRepetidoMesmoComQuantidadeDiferente() {
        assertThrows(IllegalArgumentException::class.java) {
            validar.executar(30, listOf(arroz, arroz.copy(quantidadeSobra = 1.0), feijao), esperados)
        }
    }

    @Test
    fun rejeitaItemDeOutraProducao() {
        assertThrows(IllegalArgumentException::class.java) {
            validar.executar(30, listOf(arroz.copy(producaoDiaId = 31), feijao), esperados)
        }
    }

    @Test
    fun rejeitaListaIncompleta() {
        assertThrows(IllegalArgumentException::class.java) {
            validar.executar(30, listOf(arroz), esperados)
        }
    }

    @Test
    fun rejeitaItemDesconhecidoComMesmoTamanhoDeLista() {
        assertThrows(IllegalArgumentException::class.java) {
            validar.executar(30, listOf(arroz, feijao.copy(id = 21)), esperados)
        }
    }

    @Test
    fun rejeitaPreparoQueNaoCorrespondeAoItemSalvo() {
        assertThrows(IllegalArgumentException::class.java) {
            validar.executar(30, listOf(arroz.copy(alimentoId = 2), feijao), esperados)
        }
    }
}
