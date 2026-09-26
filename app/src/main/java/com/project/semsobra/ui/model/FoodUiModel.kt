package com.project.semsobra.ui.model

data class FoodUiModel(
    val id: Long = 0,
    val nome: String,
    val descricao: String = "",
    val unidadeMedida: String = "kg",
    val diaDaSemana: Int = TODOS_OS_DIAS,
    val diasSemanaMask: Int = if (diaDaSemana == TODOS_OS_DIAS) {
        TODOS_OS_DIAS_MASK
    } else {
        1 shl (diaDaSemana - 1)
    }
) {
    companion object {
        const val TODOS_OS_DIAS = 0
        const val TODOS_OS_DIAS_MASK = 127
    }
}

/**
 * Mantém compatibilidade com preparos antigos, sem dia definido, considerando-os
 * disponíveis todos os dias. Novos cadastros recebem um dia entre segunda e domingo.
 */
fun FoodUiModel.disponivelNoDia(dia: Int): Boolean =
    dia in 1..7 && (diasSemanaMask and (1 shl (dia - 1))) != 0
