package com.project.semsobra.domain.previsao.model

import java.time.LocalDate

enum class Turno {
    CAFE_DA_MANHA,
    ALMOCO,
    JANTAR
}

data class RegistroHistoricoDemanda(
    val data: LocalDate,
    val turno: Turno,
    val quantidadeClientes: Int,
    val quilosVendidos: Double,
    val quilosPreparados: Double,
    val quilosSobraram: Double,
    val restauranteAberto: Boolean = true
)

data class EntradaPrevisao(
    val dataPrevisao: LocalDate,
    val turno: Turno,
    val historico: List<RegistroHistoricoDemanda>,
    val margemSeguranca: Double = MARGEM_SEGURANCA_PADRAO,
    val percentualProducaoInicial: Double = PERCENTUAL_PRODUCAO_INICIAL_PADRAO
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
