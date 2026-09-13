package com.project.semsobra.domain.previsao

import com.project.semsobra.domain.previsao.model.EntradaPrevisao
import com.project.semsobra.domain.previsao.model.OrigemHistoricoPrevisao
import com.project.semsobra.domain.previsao.model.QualidadePrevisao
import com.project.semsobra.domain.previsao.model.RegistroHistoricoDemanda
import com.project.semsobra.domain.previsao.model.ResultadoPrevisao
import com.project.semsobra.domain.previsao.model.Turno
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrevisaoPorMediaPonderadaTest {
    private val motor = PrevisaoPorMediaPonderada()
    private val dataPrevisao = LocalDate.of(2026, 9, 14)

    @Test
    fun `calcula previsao normal com registros comparaveis`() {
        val historico = (1L..6L).map { semanas ->
            registro(
                data = dataPrevisao.minusWeeks(semanas),
                clientes = 100,
                vendidos = 50.0,
                preparados = 54.0,
                sobras = 4.0
            )
        }

        val resultado = calcular(historico)

        assertEquals(100, resultado.clientesPrevistos)
        assertEquals(100, resultado.faixaHistoricaClientes.minimoEstimado)
        assertEquals(100, resultado.faixaHistoricaClientes.maximoEstimado)
        assertEquals(0.5, resultado.consumoMedioKgPorCliente, 0.001)
        assertEquals(50.0, resultado.demandaPrevistaKg, 0.001)
        assertEquals(54.0, resultado.preparoRecomendadoKg, 0.001)
        assertEquals(QualidadePrevisao.ALTA, resultado.qualidade)
    }

    @Test
    fun `atribui peso maior aos registros mais recentes`() {
        val historico = listOf(
            registro(dataPrevisao.minusWeeks(3), clientes = 80, vendidos = 40.0),
            registro(dataPrevisao.minusWeeks(2), clientes = 100, vendidos = 50.0),
            registro(dataPrevisao.minusWeeks(1), clientes = 120, vendidos = 60.0)
        )

        val resultado = calcular(historico)

        assertEquals(107, resultado.clientesPrevistos)
        assertEquals(53.5, resultado.demandaPrevistaKg, 0.001)
        assertEquals(
            OrigemHistoricoPrevisao.MESMO_DIA_DA_SEMANA_E_TURNO,
            resultado.origemHistorico
        )
    }

    @Test
    fun `aplica margem de seguranca configurada`() {
        val historico = tresRegistrosIguais(clientes = 100, vendidos = 50.0)

        val resultado = calcular(historico, margemSeguranca = 0.20)

        assertEquals(50.0, resultado.demandaPrevistaKg, 0.001)
        assertEquals(60.0, resultado.preparoRecomendadoKg, 0.001)
        assertEquals(0.20, resultado.margemSegurancaAplicada, 0.001)
    }

    @Test
    fun `divide preparo entre producao inicial e reserva`() {
        val resultado = calcular(tresRegistrosIguais(clientes = 100, vendidos = 50.0))

        assertEquals(54.0, resultado.preparoRecomendadoKg, 0.001)
        assertEquals(40.5, resultado.producaoInicialKg, 0.001)
        assertEquals(13.5, resultado.reservaReposicaoKg, 0.001)
        assertEquals(
            resultado.preparoRecomendadoKg,
            resultado.producaoInicialKg + resultado.reservaReposicaoKg,
            0.001
        )
    }

    @Test
    fun `marca qualidade baixa quando historico e insuficiente`() {
        val historico = listOf(
            registro(dataPrevisao.minusWeeks(2), clientes = 90, vendidos = 45.0),
            registro(dataPrevisao.minusWeeks(1), clientes = 100, vendidos = 50.0)
        )

        val resultado = calcular(historico)

        assertEquals(QualidadePrevisao.BAIXA, resultado.qualidade)
        assertEquals(2, resultado.quantidadeRegistrosUtilizados)
        assertEquals(
            OrigemHistoricoPrevisao.TODOS_OS_REGISTROS_VALIDOS,
            resultado.origemHistorico
        )
        assertTrue(resultado.mensagem.contains("insuficiente", ignoreCase = true))
    }

    @Test
    fun `usa registros do mesmo turno quando faltam dias da semana equivalentes`() {
        val historico = listOf(
            registro(dataPrevisao.minusDays(6), clientes = 80, vendidos = 40.0),
            registro(dataPrevisao.minusDays(5), clientes = 90, vendidos = 45.0),
            registro(dataPrevisao.minusDays(4), clientes = 100, vendidos = 50.0),
            registro(dataPrevisao.minusDays(3), clientes = 110, vendidos = 55.0)
        )

        val resultado = calcular(historico)

        assertEquals(OrigemHistoricoPrevisao.MESMO_TURNO, resultado.origemHistorico)
        assertEquals(4, resultado.quantidadeRegistrosUtilizados)
        assertEquals(100, resultado.clientesPrevistos)
    }

    @Test
    fun `ignora dias em que restaurante estava fechado`() {
        val historico = tresRegistrosIguais(clientes = 100, vendidos = 50.0) +
            registro(
                data = dataPrevisao.minusWeeks(4),
                clientes = 1_000,
                vendidos = 900.0,
                aberto = false
            )

        val resultado = calcular(historico)

        assertEquals(3, resultado.quantidadeRegistrosUtilizados)
        assertEquals(100, resultado.clientesPrevistos)
        assertEquals(50.0, resultado.demandaPrevistaKg, 0.001)
    }

    @Test
    fun `nao usa registro com zero clientes no consumo por cliente`() {
        val historico = listOf(
            registro(dataPrevisao.minusWeeks(3), clientes = 0, vendidos = 10.0),
            registro(dataPrevisao.minusWeeks(2), clientes = 100, vendidos = 50.0),
            registro(dataPrevisao.minusWeeks(1), clientes = 100, vendidos = 50.0)
        )

        val resultado = calcular(historico)

        assertEquals(83, resultado.clientesPrevistos)
        assertEquals(0.5, resultado.consumoMedioKgPorCliente, 0.001)
        assertEquals(41.5, resultado.demandaPrevistaKg, 0.001)
        assertPesosFinitos(resultado)
    }

    @Test
    fun `ignora clientes e pesos negativos`() {
        val historico = tresRegistrosIguais(clientes = 100, vendidos = 50.0) + listOf(
            registro(dataPrevisao.minusDays(1), clientes = -1, vendidos = 10.0),
            registro(dataPrevisao.minusDays(2), clientes = 10, vendidos = -1.0),
            registro(dataPrevisao.minusDays(3), clientes = 10, vendidos = 5.0, preparados = -1.0),
            registro(dataPrevisao.minusDays(4), clientes = 10, vendidos = 5.0, sobras = -1.0)
        )

        val resultado = calcular(historico)

        assertEquals(3, resultado.quantidadeRegistrosUtilizados)
        assertEquals(100, resultado.clientesPrevistos)
        assertPesosFinitos(resultado)
    }

    @Test
    fun `arredonda clientes para inteiro e pesos para uma casa decimal`() {
        val historico = listOf(
            registro(dataPrevisao.minusWeeks(3), clientes = 100, vendidos = 45.6),
            registro(dataPrevisao.minusWeeks(2), clientes = 101, vendidos = 46.056),
            registro(dataPrevisao.minusWeeks(1), clientes = 102, vendidos = 46.512)
        )

        val resultado = calcular(historico)

        assertEquals(101, resultado.clientesPrevistos)
        assertEquals(0.5, resultado.consumoMedioKgPorCliente, 0.001)
        assertEquals(46.1, resultado.demandaPrevistaKg, 0.001)
        assertEquals(49.7, resultado.preparoRecomendadoKg, 0.001)
        assertEquals(37.3, resultado.producaoInicialKg, 0.001)
        assertEquals(12.4, resultado.reservaReposicaoKg, 0.001)
    }

    @Test
    fun `retorna resultado seguro quando historico esta vazio`() {
        val resultado = calcular(emptyList())

        assertEquals(0, resultado.clientesPrevistos)
        assertEquals(0, resultado.quantidadeRegistrosUtilizados)
        assertEquals(QualidadePrevisao.BAIXA, resultado.qualidade)
        assertEquals(OrigemHistoricoPrevisao.SEM_HISTORICO, resultado.origemHistorico)
        assertTrue(resultado.mensagem.isNotBlank())
        assertPesosFinitos(resultado)
    }

    @Test
    fun `limita calculo aos oito registros mais recentes`() {
        val historico = (1L..10L).map { semanas ->
            registro(
                data = dataPrevisao.minusWeeks(semanas),
                clientes = if (semanas > 8) 1_000 else 100,
                vendidos = if (semanas > 8) 900.0 else 50.0
            )
        }

        val resultado = calcular(historico)

        assertEquals(8, resultado.quantidadeRegistrosUtilizados)
        assertEquals(100, resultado.clientesPrevistos)
        assertEquals(50.0, resultado.demandaPrevistaKg, 0.001)
    }

    private fun calcular(
        historico: List<RegistroHistoricoDemanda>,
        margemSeguranca: Double = EntradaPrevisao.MARGEM_SEGURANCA_PADRAO
    ): ResultadoPrevisao = motor.calcular(
        EntradaPrevisao(
            dataPrevisao = dataPrevisao,
            turno = Turno.ALMOCO,
            historico = historico,
            margemSeguranca = margemSeguranca
        )
    )

    private fun tresRegistrosIguais(
        clientes: Int,
        vendidos: Double
    ): List<RegistroHistoricoDemanda> = (1L..3L).map { semanas ->
        registro(
            data = dataPrevisao.minusWeeks(semanas),
            clientes = clientes,
            vendidos = vendidos
        )
    }

    private fun registro(
        data: LocalDate,
        clientes: Int,
        vendidos: Double,
        preparados: Double = vendidos,
        sobras: Double = 0.0,
        turno: Turno = Turno.ALMOCO,
        aberto: Boolean = true
    ) = RegistroHistoricoDemanda(
        data = data,
        turno = turno,
        quantidadeClientes = clientes,
        quilosVendidos = vendidos,
        quilosPreparados = preparados,
        quilosSobraram = sobras,
        restauranteAberto = aberto
    )

    private fun assertPesosFinitos(resultado: ResultadoPrevisao) {
        listOf(
            resultado.consumoMedioKgPorCliente,
            resultado.demandaPrevistaKg,
            resultado.preparoRecomendadoKg,
            resultado.producaoInicialKg,
            resultado.reservaReposicaoKg
        ).forEach { valor ->
            assertTrue("Esperava valor finito, mas recebeu $valor", valor.isFinite())
        }
    }
}
