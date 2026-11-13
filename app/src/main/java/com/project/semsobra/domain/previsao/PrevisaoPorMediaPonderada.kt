package com.project.semsobra.domain.previsao

import com.project.semsobra.domain.model.QuantityPolicy
import com.project.semsobra.domain.previsao.model.CondicaoClima
import com.project.semsobra.domain.previsao.model.EntradaPrevisao
import com.project.semsobra.domain.previsao.model.FaixaHistoricaClientes
import com.project.semsobra.domain.previsao.model.OrigemHistoricoPrevisao
import com.project.semsobra.domain.previsao.model.QualidadePrevisao
import com.project.semsobra.domain.previsao.model.RegistroHistoricoDemanda
import com.project.semsobra.domain.previsao.model.ResultadoPrevisao
import java.time.DayOfWeek
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

        // Se o restaurante não abre aos domingos e a previsão é para um domingo
        if (entrada.dataPrevisao.dayOfWeek == DayOfWeek.SUNDAY && !entrada.restauranteAbertoAosDomingos) {
            return ResultadoPrevisao(
                dataPrevisao = entrada.dataPrevisao,
                turno = entrada.turno,
                clientesPrevistos = 0,
                faixaHistoricaClientes = FaixaHistoricaClientes(0, 0),
                consumoMedioKgPorCliente = 0.0,
                demandaPrevistaKg = 0.0,
                preparoRecomendadoKg = 0.0,
                producaoInicialKg = 0.0,
                reservaReposicaoKg = 0.0,
                qualidade = QualidadePrevisao.ALTA,
                origemHistorico = OrigemHistoricoPrevisao.SEM_HISTORICO,
                quantidadeRegistrosUtilizados = 0,
                margemSegurancaAplicada = margemSeguranca,
                percentualProducaoInicialAplicado = percentualProducaoInicial,
                mensagem = "O restaurante não abre aos domingos. Previsão de demanda zerada."
            )
        }

        val registrosValidos = filtrarRegistrosValidos(entrada)
        val selecao = selecionarHistorico(entrada, registrosValidos)
        if (selecao.registros.isEmpty()) {
            return resultadoSemHistorico(entrada, margemSeguranca)
        }

        val ponderados = ponderarRegistros(entrada, selecao)
        val clientesBase = mediaPonderada(ponderados) { it.quantidadeClientes.toDouble() }

        // Fatores de ajuste para Pagamento e Clima quando não há histórico idêntico suficiente
        val fatorPagamento = if (entrada.ehPeriodoPagamento && ponderados.count { it.registro.ehPeriodoPagamento } < 2) 1.15 else 1.0
        val fatorClima = when {
            entrada.climaPrevisto == CondicaoClima.TEMPESTADE && ponderados.count { it.registro.clima == CondicaoClima.TEMPESTADE } < 2 -> 0.70
            entrada.climaPrevisto == CondicaoClima.CHUVOSO && ponderados.count { it.registro.clima.reduzFluxoClientes } < 2 -> 0.85
            else -> 1.0
        }

        val clientesMedia = (clientesBase * fatorPagamento * fatorClima).valorSeguro()
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
                entrada = entrada,
                origem = selecao.origem,
                qualidade = qualidade,
                quantidadeRegistros = ponderados.size,
                quantidadeRegistrosComConsumo = registrosComConsumo.size
            )
        )
    }

    private fun filtrarRegistrosValidos(entrada: EntradaPrevisao): List<RegistroHistoricoDemanda> {
        val base = entrada.historico.filter { registro ->
            registro.ehValido() && registro.data.isBefore(entrada.dataPrevisao)
        }
        if (!entrada.ehDiaFeriadoOuAtipico && !entrada.considerarFeriadosEAtipicos) {
            val apenasRegulares = base.filter { !it.ehFeriadoOuAtipico }
            if (apenasRegulares.size >= minimoRegistrosComparaveis) {
                return apenasRegulares
            }
        }
        return base
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

    private fun ponderarRegistros(
        entrada: EntradaPrevisao,
        selecao: SelecaoHistorico
    ): List<RegistroPonderado> {
        val ordenados = selecao.registros.sortedBy { it.data }
        return ordenados.mapIndexed { index, registro ->
            val baseRecencia = (index + 1).toDouble()

            val multiplicadorDiaDaSemana = if (
                selecao.origem != OrigemHistoricoPrevisao.MESMO_DIA_DA_SEMANA_E_TURNO &&
                registro.data.dayOfWeek == entrada.dataPrevisao.dayOfWeek
            ) {
                2.5
            } else {
                1.0
            }

            val multiplicadorAtipico = when {
                registro.ehFeriadoOuAtipico && entrada.ehDiaFeriadoOuAtipico -> 3.0
                registro.ehFeriadoOuAtipico && !entrada.ehDiaFeriadoOuAtipico -> 0.3
                !registro.ehFeriadoOuAtipico && entrada.ehDiaFeriadoOuAtipico -> 0.5
                else -> 1.0
            }

            val multiplicadorPagamento = if (entrada.ehPeriodoPagamento && registro.ehPeriodoPagamento) {
                2.0
            } else {
                1.0
            }

            val multiplicadorClima = if (registro.clima == entrada.climaPrevisto) {
                2.5
            } else if (entrada.climaPrevisto.reduzFluxoClientes && registro.clima.reduzFluxoClientes) {
                2.0
            } else {
                1.0
            }

            val pesoFinal = baseRecencia * multiplicadorDiaDaSemana * multiplicadorAtipico * multiplicadorPagamento * multiplicadorClima
            RegistroPonderado(registro, pesoFinal)
        }
    }

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
        entrada: EntradaPrevisao,
        origem: OrigemHistoricoPrevisao,
        qualidade: QualidadePrevisao,
        quantidadeRegistros: Int,
        quantidadeRegistrosComConsumo: Int
    ): String {
        if (quantidadeRegistrosComConsumo == 0) {
            return "Há histórico de clientes, mas ainda não há consumo válido por cliente."
        }
        val notas = mutableListOf<String>()
        if (entrada.ehPeriodoPagamento) {
            notas.add("período 5º dia útil/pagamento")
        }
        if (entrada.climaPrevisto.reduzFluxoClientes) {
            notas.add("clima ${entrada.climaPrevisto.name.lowercase()}")
        }
        if (entrada.ehDiaFeriadoOuAtipico) {
            notas.add("feriado/dia atípico")
        }
        val sufixoNotas = if (notas.isNotEmpty()) " (${notas.joinToString(", ")})" else ""

        return when (origem) {
            OrigemHistoricoPrevisao.MESMO_DIA_DA_SEMANA_E_TURNO -> {
                if (qualidade == QualidadePrevisao.ALTA) {
                    "Previsão baseada nos $quantidadeRegistros dias mais comparáveis do mesmo dia da semana.$sufixoNotas"
                } else {
                    "Previsão inicial baseada em $quantidadeRegistros dias comparáveis; a faixa é uma estimativa histórica.$sufixoNotas"
                }
            }
            OrigemHistoricoPrevisao.MESMO_TURNO ->
                "Sem dias equivalentes suficientes; foram usados $quantidadeRegistros registros do mesmo turno, ponderando dias equivalentes.$sufixoNotas"
            OrigemHistoricoPrevisao.TODOS_OS_REGISTROS_VALIDOS ->
                "Histórico comparável insuficiente; foram usados $quantidadeRegistros registros disponíveis com pesos ajustados.$sufixoNotas"
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
        return QuantityPolicy.normalize(seguro, scale = 1).valorSeguro()
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
