package com.project.semsobra.domain.previsao

import com.project.semsobra.domain.previsao.model.EntradaPrevisao
import com.project.semsobra.domain.previsao.model.FaixaHistoricaClientes
import com.project.semsobra.domain.previsao.model.OrigemHistoricoPrevisao
import com.project.semsobra.domain.previsao.model.QualidadePrevisao
import com.project.semsobra.domain.previsao.model.RegistroHistoricoDemanda
import com.project.semsobra.domain.previsao.model.ResultadoPrevisao
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sqrt

class PrevisaoPorMediaPonderada(
    private val maximoRegistros: Int = 8,
    private val minimoRegistrosComparaveis: Int = 3
) : MotorPrevisao {

    init {
        require(maximoRegistros > 0) { "O máximo de registros deve ser maior que zero" }
        require(minimoRegistrosComparaveis > 0) {
            "O mínimo de registros comparáveis deve ser maior que zero"
        }
    }

    override fun calcular(entrada: EntradaPrevisao): ResultadoPrevisao {
        val margemSeguranca = entrada.margemSeguranca
            .takeIf { it.isFinite() && it >= 0.0 }
            ?: EntradaPrevisao.MARGEM_SEGURANCA_PADRAO
        val percentualProducaoInicial = entrada.percentualProducaoInicial
            .takeIf(Double::isFinite)
            ?.coerceIn(0.0, 1.0)
            ?: EntradaPrevisao.PERCENTUAL_PRODUCAO_INICIAL_PADRAO

        val registrosValidos = entrada.historico.filter { registro ->
            registro.ehValido() && registro.data.isBefore(entrada.dataPrevisao)
        }
        val selecao = selecionarHistorico(entrada, registrosValidos)
        if (selecao.registros.isEmpty()) {
            return resultadoSemHistorico(entrada, margemSeguranca)
        }

        val ponderados = selecao.registros
            .sortedBy { it.data }
            .mapIndexed { index, registro -> RegistroPonderado(registro, (index + 1).toDouble()) }
        val clientesMedia = mediaPonderada(ponderados) { it.quantidadeClientes.toDouble() }
        val clientesPrevistos = clientesMedia
            .coerceAtLeast(0.0)
            .roundToInt()
        val registrosComConsumo = ponderados.filter { it.registro.quantidadeClientes > 0 }
        val consumoMedioSemArredondar = mediaPonderada(registrosComConsumo) {
            it.quilosVendidos / it.quantidadeClientes
        }.valorSeguro()
        val demandaSemArredondar =
            (clientesPrevistos * consumoMedioSemArredondar).valorSeguro()
        val preparoSemArredondar =
            (demandaSemArredondar * (1.0 + margemSeguranca)).valorSeguro()
        val producaoInicialSemArredondar =
            (preparoSemArredondar * percentualProducaoInicial).valorSeguro()

        val preparoRecomendado = arredondarPeso(preparoSemArredondar)
        val producaoInicial = arredondarPeso(producaoInicialSemArredondar)
        val reservaReposicao = arredondarPeso(
            (preparoRecomendado - producaoInicial).coerceAtLeast(0.0)
        )
        val qualidade = calcularQualidade(
            origem = selecao.origem,
            quantidadeRegistros = ponderados.size,
            quantidadeRegistrosComConsumo = registrosComConsumo.size
        )

        return ResultadoPrevisao(
            dataPrevisao = entrada.dataPrevisao,
            turno = entrada.turno,
            clientesPrevistos = clientesPrevistos,
            faixaHistoricaClientes = calcularFaixaHistorica(ponderados, clientesMedia),
            consumoMedioKgPorCliente = arredondarPeso(consumoMedioSemArredondar),
            demandaPrevistaKg = arredondarPeso(demandaSemArredondar),
            preparoRecomendadoKg = preparoRecomendado,
            producaoInicialKg = producaoInicial,
            reservaReposicaoKg = reservaReposicao,
            qualidade = qualidade,
            origemHistorico = selecao.origem,
            quantidadeRegistrosUtilizados = ponderados.size,
            margemSegurancaAplicada = margemSeguranca,
            percentualProducaoInicialAplicado = percentualProducaoInicial,
            mensagem = criarMensagem(
                origem = selecao.origem,
                qualidade = qualidade,
                quantidadeRegistros = ponderados.size,
                quantidadeRegistrosComConsumo = registrosComConsumo.size
            )
        )
    }

    private fun selecionarHistorico(
        entrada: EntradaPrevisao,
        registrosValidos: List<RegistroHistoricoDemanda>
    ): SelecaoHistorico {
        val mesmoDiaETurno = registrosValidos.filter {
            it.data.dayOfWeek == entrada.dataPrevisao.dayOfWeek && it.turno == entrada.turno
        }
        if (mesmoDiaETurno.size >= minimoRegistrosComparaveis) {
            return SelecaoHistorico(
                registros = ultimosRegistros(mesmoDiaETurno),
                origem = OrigemHistoricoPrevisao.MESMO_DIA_DA_SEMANA_E_TURNO
            )
        }

        val mesmoTurno = registrosValidos.filter { it.turno == entrada.turno }
        if (mesmoTurno.size >= minimoRegistrosComparaveis) {
            return SelecaoHistorico(
                registros = ultimosRegistros(mesmoTurno),
                origem = OrigemHistoricoPrevisao.MESMO_TURNO
            )
        }

        return if (registrosValidos.isNotEmpty()) {
            SelecaoHistorico(
                registros = ultimosRegistros(registrosValidos),
                origem = OrigemHistoricoPrevisao.TODOS_OS_REGISTROS_VALIDOS
            )
        } else {
            SelecaoHistorico(emptyList(), OrigemHistoricoPrevisao.SEM_HISTORICO)
        }
    }

    private fun ultimosRegistros(
        registros: List<RegistroHistoricoDemanda>
    ): List<RegistroHistoricoDemanda> = registros
        .sortedByDescending { it.data }
        .take(maximoRegistros)
        .sortedBy { it.data }

    private fun mediaPonderada(
        registros: List<RegistroPonderado>,
        valor: (RegistroHistoricoDemanda) -> Double
    ): Double {
        if (registros.isEmpty()) return 0.0
        val somaPesos = registros.sumOf { it.peso }
        if (somaPesos <= 0.0 || !somaPesos.isFinite()) return 0.0
        val somaPonderada = registros.sumOf { valor(it.registro) * it.peso }
        return (somaPonderada / somaPesos).valorSeguro()
    }

    private fun calcularFaixaHistorica(
        registros: List<RegistroPonderado>,
        mediaClientes: Double
    ): FaixaHistoricaClientes {
        val somaPesos = registros.sumOf { it.peso }
        if (registros.isEmpty() || somaPesos <= 0.0 || !somaPesos.isFinite()) {
            return FaixaHistoricaClientes(0, 0)
        }
        val variancia = registros.sumOf {
            val diferenca = it.registro.quantidadeClientes - mediaClientes
            diferenca * diferenca * it.peso
        } / somaPesos
        val desvio = sqrt(variancia.coerceAtLeast(0.0)).valorSeguro()
        val minimo = (mediaClientes - desvio).coerceAtLeast(0.0).roundToInt()
        val maximo = (mediaClientes + desvio).coerceAtLeast(0.0).roundToInt()
        return FaixaHistoricaClientes(
            minimoEstimado = minimo,
            maximoEstimado = maximo.coerceAtLeast(minimo)
        )
    }

    private fun calcularQualidade(
        origem: OrigemHistoricoPrevisao,
        quantidadeRegistros: Int,
        quantidadeRegistrosComConsumo: Int
    ): QualidadePrevisao {
        if (
            quantidadeRegistros < minimoRegistrosComparaveis ||
            quantidadeRegistrosComConsumo < minimoRegistrosComparaveis
        ) {
            return QualidadePrevisao.BAIXA
        }
        return when (origem) {
            OrigemHistoricoPrevisao.MESMO_DIA_DA_SEMANA_E_TURNO -> {
                if (quantidadeRegistros >= 6) QualidadePrevisao.ALTA else QualidadePrevisao.MEDIA
            }
            OrigemHistoricoPrevisao.MESMO_TURNO -> {
                if (quantidadeRegistros >= 6) QualidadePrevisao.MEDIA else QualidadePrevisao.BAIXA
            }
            OrigemHistoricoPrevisao.TODOS_OS_REGISTROS_VALIDOS,
            OrigemHistoricoPrevisao.SEM_HISTORICO -> QualidadePrevisao.BAIXA
        }
    }

    private fun criarMensagem(
        origem: OrigemHistoricoPrevisao,
        qualidade: QualidadePrevisao,
        quantidadeRegistros: Int,
        quantidadeRegistrosComConsumo: Int
    ): String {
        if (quantidadeRegistrosComConsumo == 0) {
            return "Há histórico de clientes, mas ainda não há consumo válido por cliente."
        }
        return when (origem) {
            OrigemHistoricoPrevisao.MESMO_DIA_DA_SEMANA_E_TURNO -> {
                if (qualidade == QualidadePrevisao.ALTA) {
                    "Previsão baseada nos $quantidadeRegistros dias mais comparáveis."
                } else {
                    "Previsão inicial baseada em $quantidadeRegistros dias comparáveis; a faixa é uma estimativa histórica."
                }
            }
            OrigemHistoricoPrevisao.MESMO_TURNO ->
                "Sem dias equivalentes suficientes; foram usados $quantidadeRegistros registros do mesmo turno."
            OrigemHistoricoPrevisao.TODOS_OS_REGISTROS_VALIDOS ->
                "Histórico comparável insuficiente; foram usados $quantidadeRegistros registros disponíveis."
            OrigemHistoricoPrevisao.SEM_HISTORICO ->
                "Ainda não há histórico válido suficiente para calcular a previsão."
        }
    }

    private fun resultadoSemHistorico(
        entrada: EntradaPrevisao,
        margemSeguranca: Double
    ) = ResultadoPrevisao(
        dataPrevisao = entrada.dataPrevisao,
        turno = entrada.turno,
        clientesPrevistos = 0,
        faixaHistoricaClientes = FaixaHistoricaClientes(0, 0),
        consumoMedioKgPorCliente = 0.0,
        demandaPrevistaKg = 0.0,
        preparoRecomendadoKg = 0.0,
        producaoInicialKg = 0.0,
        reservaReposicaoKg = 0.0,
        qualidade = QualidadePrevisao.BAIXA,
        origemHistorico = OrigemHistoricoPrevisao.SEM_HISTORICO,
        quantidadeRegistrosUtilizados = 0,
        margemSegurancaAplicada = margemSeguranca,
        percentualProducaoInicialAplicado = entrada.percentualProducaoInicial
            .takeIf(Double::isFinite)
            ?.coerceIn(0.0, 1.0)
            ?: EntradaPrevisao.PERCENTUAL_PRODUCAO_INICIAL_PADRAO,
        mensagem = "Ainda não há histórico válido suficiente para calcular a previsão."
    )

    private fun RegistroHistoricoDemanda.ehValido(): Boolean =
        restauranteAberto &&
            quantidadeClientes >= 0 &&
            quilosVendidos.ehPesoValido() &&
            quilosPreparados.ehPesoValido() &&
            quilosSobraram.ehPesoValido()

    private fun Double.ehPesoValido(): Boolean = isFinite() && this >= 0.0

    private fun Double.valorSeguro(): Double =
        if (isFinite() && this >= 0.0) this else 0.0

    private fun arredondarPeso(valor: Double): Double {
        val seguro = valor.valorSeguro()
        return (round(seguro * 10.0) / 10.0).valorSeguro()
    }

    private data class RegistroPonderado(
        val registro: RegistroHistoricoDemanda,
        val peso: Double
    )

    private data class SelecaoHistorico(
        val registros: List<RegistroHistoricoDemanda>,
        val origem: OrigemHistoricoPrevisao
    )
}
