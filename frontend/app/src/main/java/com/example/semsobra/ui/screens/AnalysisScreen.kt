package com.example.semsobra.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.semsobra.ui.components.EmptyState
import com.example.semsobra.ui.components.ForecastItemCard
import com.example.semsobra.ui.components.HeaderCard
import com.example.semsobra.ui.components.MetricCard
import com.example.semsobra.ui.components.ProductionSummaryCard
import com.example.semsobra.ui.components.SectionTitle
import com.example.semsobra.ui.components.SimpleMetricRow
import com.example.semsobra.ui.model.AnalyticsResult
import com.example.semsobra.ui.model.ProductionSummary
import com.example.semsobra.ui.util.formatQuantity

@Composable
fun AnalysisScreen(
    analytics: AnalyticsResult,
    summaries: List<ProductionSummary>,
    modifier: Modifier = Modifier
) {
    val report = analytics.report
    val totalFaltas = report.alimentosQueMaisAcabaram.sumOf { it.quantidade }.toInt()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HeaderCard(
                title = "Analise da operacao",
                value = "${analytics.forecast.clientesPrevistos} clientes previstos",
                subtitle = "Historico, previsao e pontos de atencao para o proximo preparo."
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Sobra registrada",
                    value = "${formatQuantity(report.totalSobras)} kg",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Faltas",
                    value = totalFaltas.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item { SectionTitle("Previsao por preparo") }
        if (analytics.forecast.items.isEmpty()) {
            item { EmptyState("Cadastre preparos e feche alguns dias para gerar previsoes.") }
        } else {
            items(analytics.forecast.items, key = { it.food.id }) { item ->
                ForecastItemCard(item, explain = true)
            }
        }
        item { SectionTitle("Preparos com mais sobra") }
        if (report.alimentosComMaisSobra.isEmpty()) {
            item { EmptyState("Ainda nao ha sobras registradas.") }
        } else {
            items(report.alimentosComMaisSobra, key = { it.food.id }) { metric ->
                SimpleMetricRow(metric.food.nome, "${formatQuantity(metric.quantidade)} ${metric.food.unidadeMedida}")
            }
        }
        item { SectionTitle("Preparos que mais acabaram") }
        if (report.alimentosQueMaisAcabaram.isEmpty()) {
            item { EmptyState("Nenhum preparo marcado como falta ate agora.") }
        } else {
            items(report.alimentosQueMaisAcabaram, key = { it.food.id }) { metric ->
                SimpleMetricRow(metric.food.nome, "${metric.quantidade.toInt()} ocorrencia(s)")
            }
        }
        item { SectionTitle("Historico recente") }
        if (summaries.isEmpty()) {
            item { EmptyState("O historico aparecera depois da primeira producao.") }
        } else {
            items(summaries.take(5), key = { it.day.id }) { summary ->
                ProductionSummaryCard(summary, showItems = true)
            }
        }
    }
}
