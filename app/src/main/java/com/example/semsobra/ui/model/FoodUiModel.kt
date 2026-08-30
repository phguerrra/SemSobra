package com.example.semsobra.ui.model

data class FoodUiModel(
    val id: Long = 0,
    val nome: String,
    val descricao: String = "",
    val unidadeMedida: String = "kg"
)
