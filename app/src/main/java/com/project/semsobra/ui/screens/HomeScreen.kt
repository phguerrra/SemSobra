package com.project.semsobra.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.project.semsobra.R
import com.project.semsobra.ui.components.EmptyState
import com.project.semsobra.ui.components.ForecastItemCard
import com.project.semsobra.ui.components.HeaderCard
import com.project.semsobra.ui.components.MetricCard
import com.project.semsobra.ui.components.OperationStepCard
import com.project.semsobra.ui.components.ProductionSummaryCard
import com.project.semsobra.ui.components.SectionTitle
import com.project.semsobra.domain.model.AnalyticsResult
import com.project.semsobra.ui.model.FoodUiModel
import com.project.semsobra.domain.model.ProductionSummary
import com.project.semsobra.ui.util.dayName
import com.project.semsobra.ui.util.formatDate
import java.time.LocalDate

@Composable
fun HomeScreen(
    analytics: AnalyticsResult,
    forecastDate: LocalDate,
    foods: List<FoodUiModel>,
    summaries: List<ProductionSummary>,
    onOpenProduction: () -> Unit,
    onOpenClosing: () -> Unit,
    onOpenAnalysis: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    backupEnabled: Boolean,
    backupBusy: Boolean,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val formattedForecastDate = formatDate(forecastDate.toString())
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
                    title = stringResource(R.string.home_forecast_customers, formattedForecastDate),
                    value = analytics.forecast.clientesPrevistos.toString(),
                    icon = Icons.Filled.People,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Preparos",
                    value = foods.size.toString(),
                    icon = Icons.Filled.Restaurant,
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
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(
                                text = when {
                                    todaySummary == null -> "PASSO 1 DE 2"
                                    !todaySummary.fechado -> "PASSO 2 DE 2"
                                    else -> "DIA CONCLUÍDO"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    androidx.compose.foundation.layout.Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            nextAction.first,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            nextAction.second,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = nextAction.third,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            when {
                                todaySummary == null -> "Registrar Preparo de Hoje"
                                !todaySummary.fechado -> "Registrar Sobras de Hoje"
                                else -> "Ver Análise do Dia"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        item { SectionTitle(stringResource(R.string.home_forecast_production, formattedForecastDate)) }
        if (analytics.forecast.items.isEmpty()) {
            item {
                EmptyState(
                    "Cadastre preparos e registre fechamentos para gerar previsões.",
                    icon = Icons.Filled.Assessment
                )
            }
        } else {
            items(analytics.forecast.items.take(6), key = { "forecast-${it.food.id}" }) { item ->
                ForecastItemCard(item)
            }
        }
        item { SectionTitle("Alertas") }
        if (analytics.forecast.alerts.isEmpty()) {
            item {
                EmptyState(
                    "Sem alertas recentes de sobra alta ou falta.",
                    icon = Icons.Filled.CheckCircle
                )
            }
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
            item {
                EmptyState(
                    "Nenhuma produção registrada ainda.",
                    icon = Icons.Filled.History
                )
            }
        } else {
            items(summaries.take(3), key = { "summary-${it.day.id}" }) { summary ->
                ProductionSummaryCard(summary)
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp)
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Backup dos dados", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Exporte uma cópia para guardar ou restaure um arquivo salvo.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = onExportBackup,
                        enabled = backupEnabled,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (backupBusy) "Processando…" else "Exportar backup")
                    }
                    OutlinedButton(
                        onClick = onImportBackup,
                        enabled = backupEnabled,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (backupBusy) "Processando…" else "Restaurar backup")
                    }
                }
            }
        }
    }
}
