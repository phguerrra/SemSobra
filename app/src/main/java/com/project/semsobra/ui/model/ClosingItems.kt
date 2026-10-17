package com.project.semsobra.ui.model

import com.project.semsobra.domain.model.ItemProducao
import com.project.semsobra.domain.model.ProductionSummary

internal fun prepararItensFechamento(
    producao: ProductionSummary,
    itens: List<ItemProducao>
): List<ItemProducao> {
    val itensSalvos = producao.items.associateBy { it.item.id }
    return itens.map { item ->
        item.copy(
            quantidadeProduzida = if (producao.fechado) {
                item.quantidadeProduzida
            } else {
                itensSalvos[item.id]?.item?.quantidadeProduzida ?: item.quantidadeProduzida
            },
            horarioAcabou = item.horarioAcabou?.trim()?.takeIf { item.acabouAntesDoFim }
        )
    }
}
