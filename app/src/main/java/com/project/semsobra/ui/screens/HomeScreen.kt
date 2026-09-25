package com.project.semsobra.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.semsobra.ui.components.EmptyState
import com.project.semsobra.ui.components.ForecastItemCard
import com.project.semsobra.ui.components.HeaderCard
import com.project.semsobra.ui.components.MetricCard
import com.project.semsobra.ui.components.OperationStepCard
import com.project.semsobra.ui.components.ProductionSummaryCard
import com.project.semsobra.ui.components.SectionTitle
import com.project.semsobra.ui.model.AnalyticsResult
import com.project.semsobra.ui.model.FoodUiModel
import com.project.semsobra.ui.model.ProductionSummary
import com.project.semsobra.ui.util.dayName
import com.project.semsobra.ui.util.formatDate
import java.time.LocalDate

@Composable
fun HomeScreen(
    analytics: AnalyticsResult,
    foods: List<FoodUiModel>,
    summaries: List<ProductionSummary>,
    onOpenProduction: () -> Unit,
    onOpenClosing: () -> Unit,
    onOpenAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val todaySummary = summaries.firstOrNull { it.day.data == today.toString() }
    val nextAction = when {
        todaySummary == null -> Triple(
            "Registrar produção",
            "Comece informando as quantidades preparadas hoje.",
            onOpenProduction
        )
        !todaySummary.fechado -> Triple(
            "Ir para fechamento",
            "A produção já foi registrada. O fechamento é a próxima etapa.",
            onOpenClosing
        )
        else -> Triple(
            "Ver análise do dia",
            "O fluxo de hoje está concluído. Consulte os resultados atualizados.",
            onOpenAnalysis
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeaderCard(
                title = "Operação de hoje",
                value = "${formatDate(today.toString())} - ${dayName(today.dayOfWeek.value)}",
                subtitle = "Planeje as quantidades, acompanhe sobras e registre faltas no fechamento."
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Clientes previstos",
                    value = analytics.forecast.clientesPrevistos.toString(),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Preparos",
                    value = foods.size.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            OperationStepCard(
                productionStatus = if (todaySummary == null) "pendente" else "${todaySummary.items.size} item(ns)",
                closingStatus = if (todaySummary?.fechado == true) "fechado" else "em aberto"
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(20.dp)
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Próxima ação",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(nextAction.second, style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = nextAction.third, modifier = Modifier.fillMaxWidth()) {
                        Text(nextAction.first)
                    }
                }
            }
        }
        item { SectionTitle("Produção recomendada") }
        if (analytics.forecast.items.isEmpty()) {
            item { EmptyState("Cadastre preparos e registre fechamentos para gerar previsões.") }
        } else {
            items(analytics.forecast.items.take(6), key = { it.food.id }) { item ->
                ForecastItemCard(item)
            }
        }
        item { SectionTitle("Alertas") }
        if (analytics.forecast.alerts.isEmpty()) {
            item { EmptyState("Sem alertas recentes de sobra alta ou falta.") }
        } else {
            items(analytics.forecast.alerts) { alert ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(alert, modifier = Modifier.padding(16.dp))
                }
            }
        }
        item { SectionTitle("Últimos dias") }
        if (summaries.isEmpty()) {
            item { EmptyState("Nenhuma produção registrada ainda.") }
        } else {
            items(summaries.take(3), key = { it.day.id }) { summary ->
                ProductionSummaryCard(summary)
            }
        }
    }
}
