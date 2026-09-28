package com.project.semsobra.domain.model

import com.project.semsobra.domain.previsao.model.Turno

data class ProducaoDia(
    val id: Long = 0,
    val data: String,
    val diaDaSemana: Int,
    val clientesAtendidos: Int = 0,
    val turno: Turno = Turno.ALMOCO,
    val restauranteAberto: Boolean = true,
    val alteradoEm: String? = null
)

data class ItemProducao(
    val id: Long = 0,
    val producaoDiaId: Long,
    val alimentoId: Long,
    val quantidadeProduzida: Double,
    val quantidadeSobra: Double = 0.0,
    val acabouAntesDoFim: Boolean = false,
    val horarioAcabou: String? = null
)

data class ProductionItemDisplay(
    val item: ItemProducao,
    val food: Preparo,
    val consumo: Double
)

data class ProductionSummary(
    val day: ProducaoDia,
    val items: List<ProductionItemDisplay>,
    val fechado: Boolean
)
