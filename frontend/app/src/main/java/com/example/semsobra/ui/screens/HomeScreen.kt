package com.example.semsobra.ui.screens

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
import com.example.semsobra.ui.components.EmptyState
import com.example.semsobra.ui.components.ForecastItemCard
import com.example.semsobra.ui.components.HeaderCard
import com.example.semsobra.ui.components.MetricCard
import com.example.semsobra.ui.components.OperationStepCard
import com.example.semsobra.ui.components.ProductionSummaryCard
import com.example.semsobra.ui.components.SectionTitle
import com.example.semsobra.ui.model.AnalyticsResult
import com.example.semsobra.ui.model.FoodUiModel
import com.example.semsobra.ui.model.ProductionSummary
import com.example.semsobra.ui.util.dayName
import com.example.semsobra.ui.util.formatDate
import java.time.LocalDate

@Composable
fun HomeScreen(
    analytics: AnalyticsResult,
    foods: List<FoodUiModel>,
    summaries: List<ProductionSummary>,
    onRegisterProduction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val todaySummary = summaries.firstOrNull { it.day.data == today.toString() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HeaderCard(
                title = "Operacao de hoje",
                value = "${formatDate(today.toString())} - ${dayName(today.dayOfWeek.value)}",
                subtitle = "Planeje a producao em kg, acompanhe sobras e registre faltas no fechamento."
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
            Button(onClick = onRegisterProduction, modifier = Modifier.fillMaxWidth()) {
                Text("Registrar producao do dia")
            }
        }
        item { SectionTitle("Producao recomendada") }
        if (analytics.forecast.items.isEmpty()) {
            item { EmptyState("Cadastre preparos e registre fechamentos para gerar previsoes.") }
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
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(alert, modifier = Modifier.padding(12.dp))
                }
            }
        }
        item { SectionTitle("Ultimos dias") }
        if (summaries.isEmpty()) {
            item { EmptyState("Nenhuma producao registrada ainda.") }
        } else {
            items(summaries.take(3), key = { it.day.id }) { summary ->
                ProductionSummaryCard(summary)
            }
        }
    }
}
