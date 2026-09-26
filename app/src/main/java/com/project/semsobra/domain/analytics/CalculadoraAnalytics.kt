package com.project.semsobra.domain.analytics

import com.project.semsobra.domain.model.AnalyticsResult
import com.project.semsobra.domain.model.FoodMetric
import com.project.semsobra.domain.model.ForecastItem
import com.project.semsobra.domain.model.ForecastResult
import com.project.semsobra.domain.model.Preparo
import com.project.semsobra.domain.model.ProductionItemDisplay
import com.project.semsobra.domain.model.ProductionSummary
import com.project.semsobra.domain.model.QuantityByUnit
import com.project.semsobra.domain.model.QuantityPolicy
import com.project.semsobra.domain.model.ReportSummary

class CalculadoraAnalytics {
    fun calcular(
        preparos: List<Preparo>,
        producoes: List<ProductionSummary>,
        clientesPrevistos: Int
    ): AnalyticsResult {
        val producoesFechadas = producoes.filter {
            it.fechado && it.day.clientesAtendidos > 0
        }
        val historicoPorPreparo = producoesFechadas
            .flatMap { producao ->
                producao.items.map { item ->
                    Triple(item.food.id, producao.day.clientesAtendidos, item)
                }
            }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second to it.third })

        val itensPrevistos = calcularPrevisaoPorPreparo(
            preparos,
            historicoPorPreparo,
            clientesPrevistos
        )
        val alertas = criarAlertas(producoesFechadas)
        val itensFechados = producoesFechadas.flatMap { it.items }

        return AnalyticsResult(
            forecast = ForecastResult(
                clientesPrevistos = clientesPrevistos,
                items = itensPrevistos,
                alerts = alertas
            ),
            report = ReportSummary(
                totalSobrasPorUnidade = calcularSobrasPorUnidade(itensFechados),
                alimentosComMaisSobra = calcularPreparosComMaisSobra(itensFechados),
                alimentosQueMaisAcabaram = calcularPreparosQueMaisAcabaram(itensFechados)
            )
        )
    }

    private fun calcularPrevisaoPorPreparo(
        preparos: List<Preparo>,
        historicoPorPreparo: Map<Long, List<Pair<Int, ProductionItemDisplay>>>,
        clientesPrevistos: Int
    ): List<ForecastItem> {
        if (clientesPrevistos == 0) return emptyList()

        return preparos.mapNotNull { preparo ->
            val historico = historicoPorPreparo[preparo.id].orEmpty()
            val totalClientes = historico.sumOf { it.first }
            if (totalClientes == 0) return@mapNotNull null

            val consumoMedio = historico.sumOf { it.second.consumo } / totalClientes
            val teveFalta = historico.take(3).any { it.second.item.acabouAntesDoFim }
            val fatorSeguranca = if (teveFalta) 1.1 else 1.0
            ForecastItem(
                food = preparo,
                quantidadeRecomendada = QuantityPolicy.normalize(
                    consumoMedio * clientesPrevistos * fatorSeguranca
                ),
                consumoMedioPorCliente = QuantityPolicy.normalize(consumoMedio),
                ajusteSegurancaAplicado = teveFalta
            )
        }
    }

    private fun criarAlertas(producoesFechadas: List<ProductionSummary>): List<String> =
        producoesFechadas.take(3).flatMap { producao ->
            producao.items.mapNotNull { item ->
                when {
                    item.item.acabouAntesDoFim ->
                        "${item.food.nome} acabou antes do fim do atendimento."
                    item.item.quantidadeProduzida > 0 &&
                        item.item.quantidadeSobra / item.item.quantidadeProduzida >= 0.2 ->
                        "${item.food.nome} teve sobra acima de 20%."
                    else -> null
                }
            }
        }.distinct().take(5)

    private fun calcularPreparosComMaisSobra(
        itens: List<ProductionItemDisplay>
    ): List<FoodMetric> = itens
        .groupBy { it.food.id }
        .mapNotNull { (_, itensDoPreparo) ->
            val quantidade = QuantityPolicy.sum(
                itensDoPreparo.map { it.item.quantidadeSobra }
            )
            itensDoPreparo.firstOrNull()?.food?.takeIf { quantidade > 0.0 }?.let {
                FoodMetric(it, quantidade)
            }
        }
        .sortedByDescending(FoodMetric::quantidade)

    private fun calcularPreparosQueMaisAcabaram(
        itens: List<ProductionItemDisplay>
    ): List<FoodMetric> = itens
        .filter { it.item.acabouAntesDoFim }
        .groupBy { it.food.id }
        .mapNotNull { (_, itensDoPreparo) ->
            itensDoPreparo.firstOrNull()?.food?.let {
                FoodMetric(it, itensDoPreparo.size.toDouble())
            }
        }
        .sortedByDescending(FoodMetric::quantidade)

    private fun calcularSobrasPorUnidade(
        itens: List<ProductionItemDisplay>
    ): List<QuantityByUnit> = itens
        .groupBy { it.food.unidadeMedida.trim().lowercase() }
        .map { (unidade, itensDaUnidade) ->
            QuantityByUnit(
                unidadeMedida = unidade,
                quantidade = QuantityPolicy.sum(
                    itensDaUnidade.map { it.item.quantidadeSobra }
                )
            )
        }
        .filter { it.quantidade > 0.0 }
        .sortedBy(QuantityByUnit::unidadeMedida)
}
