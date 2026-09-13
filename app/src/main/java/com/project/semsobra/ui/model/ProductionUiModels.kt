package com.project.semsobra.ui.model

import com.project.semsobra.domain.previsao.model.Turno

data class ProductionDayUiModel(
    val id: Long = 0,
    val data: String,
    val diaDaSemana: Int,
    val clientesAtendidos: Int = 0,
    val turno: Turno = Turno.ALMOCO,
    val restauranteAberto: Boolean = true
)

data class ProductionItemUiModel(
    val id: Long = 0,
    val producaoDiaId: Long,
    val alimentoId: Long,
    val quantidadeProduzida: Double,
    val quantidadeSobra: Double = 0.0,
    val acabouAntesDoFim: Boolean = false,
    val horarioAcabou: String? = null
)

data class ProductionItemDisplay(
    val item: ProductionItemUiModel,
    val food: FoodUiModel,
    val consumo: Double
)

data class ProductionSummary(
    val day: ProductionDayUiModel,
    val items: List<ProductionItemDisplay>,
    val totalSobra: Double,
    val fechado: Boolean
)
