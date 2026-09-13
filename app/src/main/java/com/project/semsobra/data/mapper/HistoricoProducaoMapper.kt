package com.project.semsobra.data.mapper

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
            RegistroHistoricoDemanda(
                data = data,
                turno = summary.day.turno,
                quantidadeClientes = summary.day.clientesAtendidos,
                quilosVendidos = summary.items.sumOf { it.consumo },
                quilosPreparados = summary.items.sumOf { it.item.quantidadeProduzida },
                quilosSobraram = summary.items.sumOf { it.item.quantidadeSobra },
                restauranteAberto = summary.day.restauranteAberto
            )
        }
        .toList()
}
