package com.project.semsobra.data.mapper

import com.project.semsobra.domain.model.QuantityPolicy
import com.project.semsobra.domain.previsao.model.RegistroHistoricoDemanda
import com.project.semsobra.ui.model.ProductionSummary
import java.time.LocalDate

class HistoricoProducaoMapper {
    fun mapear(summaries: List<ProductionSummary>): List<RegistroHistoricoDemanda> = summaries
        .asSequence()
        .filter { it.fechado }
        .mapNotNull { summary ->
            val data = runCatching { LocalDate.parse(summary.day.data) }.getOrNull()
                ?: return@mapNotNull null
            val itensEmQuilos = summary.items.filter {
                it.food.unidadeMedida.equals("kg", ignoreCase = true)
            }
            RegistroHistoricoDemanda(
                data = data,
                turno = summary.day.turno,
                quantidadeClientes = summary.day.clientesAtendidos,
                quilosVendidos = QuantityPolicy.sum(itensEmQuilos.map { it.consumo }),
                quilosPreparados = QuantityPolicy.sum(
                    itensEmQuilos.map { it.item.quantidadeProduzida }
                ),
                quilosSobraram = QuantityPolicy.sum(
                    itensEmQuilos.map { it.item.quantidadeSobra }
                ),
                restauranteAberto = summary.day.restauranteAberto
            )
        }
        .toList()
}
