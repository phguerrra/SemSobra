package com.example.semsobra.ui.model

data class ForecastItem(
    val food: FoodUiModel,
    val quantidadeRecomendada: Double,
    val consumoMedioPorCliente: Double,
    val ajusteSegurancaAplicado: Boolean
)

data class ForecastResult(
    val clientesPrevistos: Int,
    val items: List<ForecastItem>,
    val alerts: List<String>
)

data class FoodMetric(
    val food: FoodUiModel,
    val quantidade: Double
)

data class ReportSummary(
    val totalSobras: Double,
    val alimentosComMaisSobra: List<FoodMetric>,
    val alimentosQueMaisAcabaram: List<FoodMetric>
)

data class AnalyticsResult(
    val forecast: ForecastResult,
    val report: ReportSummary
)
