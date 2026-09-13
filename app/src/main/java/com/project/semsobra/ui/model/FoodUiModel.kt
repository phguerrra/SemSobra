package com.project.semsobra.ui.model

data class FoodUiModel(
    val id: Long = 0,
    val nome: String,
    val descricao: String = "",
    val unidadeMedida: String = "kg",
    val diaDaSemana: Int = TODOS_OS_DIAS
) {
    companion object {
        const val TODOS_OS_DIAS = 0
    }
}

/**
 * Mantém compatibilidade com preparos antigos, sem dia definido, considerando-os
 * disponíveis todos os dias. Novos cadastros recebem um dia entre segunda e domingo.
 */
fun FoodUiModel.disponivelNoDia(dia: Int): Boolean =
    dia in 1..7 && (diaDaSemana == FoodUiModel.TODOS_OS_DIAS || diaDaSemana == dia)
