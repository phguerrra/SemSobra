package com.project.semsobra.domain.model

data class ForecastItem(
    val food: Preparo,
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
    val food: Preparo,
    val quantidade: Double
)

data class QuantityByUnit(
    val unidadeMedida: String,
    val quantidade: Double
)

data class ReportSummary(
    val totalSobrasPorUnidade: List<QuantityByUnit>,
    val alimentosComMaisSobra: List<FoodMetric>,
    val alimentosQueMaisAcabaram: List<FoodMetric>
)

data class AnalyticsResult(
    val forecast: ForecastResult,
    val report: ReportSummary
) {
    companion object {
        fun vazio() = AnalyticsResult(
            forecast = ForecastResult(
                clientesPrevistos = 0,
                items = emptyList(),
                alerts = emptyList()
            ),
            report = ReportSummary(
                totalSobrasPorUnidade = emptyList(),
                alimentosComMaisSobra = emptyList(),
                alimentosQueMaisAcabaram = emptyList()
            )
        )
    }
}
