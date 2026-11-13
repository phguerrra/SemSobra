package com.project.semsobra.domain.previsao.model

import java.time.DayOfWeek
import java.time.LocalDate

enum class Turno {
    CAFE_DA_MANHA,
    ALMOCO,
    JANTAR
}

enum class CondicaoClima {
    ENSOLARADO,
    NUBLADO,
    CHUVOSO,
    TEMPESTADE;

    val reduzFluxoClientes: Boolean
        get() = this == CHUVOSO || this == TEMPESTADE
}

fun LocalDate.calcularQuintoDiaUtil(): LocalDate {
    var count = 0
    var date = this.withDayOfMonth(1)
    while (true) {
        if (date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY) {
            count++
            if (count == 5) return date
        }
        date = date.plusDays(1)
    }
}

fun LocalDate.ehPeriodoPagamento5DiaUtil(): Boolean {
    val quintoDiaUtil = this.calcularQuintoDiaUtil()
    val inicioJanela = quintoDiaUtil.minusDays(2)
    val fimJanela = quintoDiaUtil.plusDays(2)
    return !this.isBefore(inicioJanela) && !this.isAfter(fimJanela)
}

data class RegistroHistoricoDemanda(
    val data: LocalDate,
    val turno: Turno,
    val quantidadeClientes: Int,
    val quilosVendidos: Double,
    val quilosPreparados: Double,
    val quilosSobraram: Double,
    val restauranteAberto: Boolean = true,
    val ehFeriadoOuAtipico: Boolean = false,
    val descricaoAtipica: String? = null,
    val clima: CondicaoClima = CondicaoClima.ENSOLARADO,
    val ehPeriodoPagamento: Boolean = data.ehPeriodoPagamento5DiaUtil()
)

data class EntradaPrevisao(
    val dataPrevisao: LocalDate,
    val turno: Turno,
    val historico: List<RegistroHistoricoDemanda>,
    val margemSeguranca: Double = MARGEM_SEGURANCA_PADRAO,
    val percentualProducaoInicial: Double = PERCENTUAL_PRODUCAO_INICIAL_PADRAO,
    val ehDiaFeriadoOuAtipico: Boolean = false,
    val considerarFeriadosEAtipicos: Boolean = true,
    val climaPrevisto: CondicaoClima = CondicaoClima.ENSOLARADO,
    val ehPeriodoPagamento: Boolean = dataPrevisao.ehPeriodoPagamento5DiaUtil(),
    val restauranteAbertoAosDomingos: Boolean = false
) {
    companion object {
        const val MARGEM_SEGURANCA_PADRAO = 0.08
        const val PERCENTUAL_PRODUCAO_INICIAL_PADRAO = 0.75
    }
}

data class FaixaHistoricaClientes(
    val minimoEstimado: Int,
    val maximoEstimado: Int
)

enum class QualidadePrevisao {
    BAIXA,
    MEDIA,
    ALTA
}

enum class OrigemHistoricoPrevisao {
    MESMO_DIA_DA_SEMANA_E_TURNO,
    MESMO_TURNO,
    TODOS_OS_REGISTROS_VALIDOS,
    SEM_HISTORICO
}

data class ResultadoPrevisao(
    val dataPrevisao: LocalDate,
    val turno: Turno,
    val clientesPrevistos: Int,
    val faixaHistoricaClientes: FaixaHistoricaClientes,
    val consumoMedioKgPorCliente: Double,
    val demandaPrevistaKg: Double,
    val preparoRecomendadoKg: Double,
    val producaoInicialKg: Double,
    val reservaReposicaoKg: Double,
    val qualidade: QualidadePrevisao,
    val origemHistorico: OrigemHistoricoPrevisao,
    val quantidadeRegistrosUtilizados: Int,
    val margemSegurancaAplicada: Double,
    val percentualProducaoInicialAplicado: Double,
    val mensagem: String
)
