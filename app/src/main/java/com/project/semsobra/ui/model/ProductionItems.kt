package com.project.semsobra.ui.model

import com.project.semsobra.domain.model.ProductionSummary
import com.project.semsobra.domain.usecase.ValidarDadosProducaoUseCase
import com.project.semsobra.ui.util.parseDoubleOrNull
import java.time.LocalDate

internal data class ProductionFoodUiModel(
    val food: FoodUiModel,
    val quantidadeSalva: Double?,
    val podeEditar: Boolean
)

internal fun prepararItensProducao(
    foods: List<FoodUiModel>,
    production: ProductionSummary?,
    date: LocalDate
): List<ProductionFoodUiModel> {
    val ativos = foods.associateBy { it.id }
    val salvos = production?.items.orEmpty().map { display ->
        val food = display.food
        ProductionFoodUiModel(
            food = ativos[food.id] ?: FoodUiModel(
                food.id, food.nome, food.descricao, food.unidadeMedida,
                food.diaDaSemana, food.diasSemanaMask
            ),
            quantidadeSalva = display.item.quantidadeProduzida,
            podeEditar = production?.fechado != true && food.id in ativos
        )
    }
    if (production?.fechado == true) return salvos

    val novos = foods.filter { it.disponivelNoDia(date.dayOfWeek.value) }
        .map { ProductionFoodUiModel(it, quantidadeSalva = null, podeEditar = true) }
    return (salvos + novos).distinctBy { it.food.id }
}

internal data class ProductionQuantities(
    val quantities: Map<Long, Double>,
    val errors: Map<Long, String>
)

internal fun validarQuantidadesProducao(
    items: List<ProductionFoodUiModel>,
    inputs: Map<Long, String>,
    invalidNumberMessage: String
): ProductionQuantities {
    val quantities = mutableMapOf<Long, Double>()
    val errors = mutableMapOf<Long, String>()
    val validator = ValidarDadosProducaoUseCase()
    items.filter { it.podeEditar }.forEach { item ->
        val input = inputs[item.food.id].orEmpty()
        if (input.isBlank() && item.quantidadeSalva == null) return@forEach
        val quantity = parseDoubleOrNull(input)
        val error = if (quantity == null) invalidNumberMessage else runCatching {
            validator.validarQuantidadeProduzida(quantity)
        }.exceptionOrNull()?.message
        if (error != null) errors[item.food.id] = error
        else quantities[item.food.id] = quantity!!
    }
    return ProductionQuantities(quantities, errors)
}

/** Atualiza valores sem alterações locais, preservando o que o usuário ainda está digitando. */
internal fun atualizarRascunhoProducao(
    draft: Map<Long, String>,
    previousSaved: Map<Long, String>,
    currentSaved: Map<Long, String>
): Map<Long, String> = draft + currentSaved.filter { (id, _) ->
    id !in draft || draft[id] == previousSaved[id]
}
