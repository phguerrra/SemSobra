package com.example.semsobra.ui.model

data class ProductionDayUiModel(
    val id: Long = 0,
    val data: String,
    val diaDaSemana: Int,
    val clientesAtendidos: Int = 0
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
