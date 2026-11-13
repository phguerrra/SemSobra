package com.project.semsobra.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.sp
import com.project.semsobra.R
import com.project.semsobra.domain.model.AnalyticsResult
import com.project.semsobra.domain.model.Cidade
import com.project.semsobra.domain.model.ProductionSummary
import com.project.semsobra.domain.previsao.model.CondicaoClima
import com.project.semsobra.ui.components.CitySelectorCard
import com.project.semsobra.ui.components.EmptyState
import com.project.semsobra.ui.components.ForecastItemCard
import com.project.semsobra.ui.components.HeaderCard
import com.project.semsobra.ui.components.MetricCard
import com.project.semsobra.ui.components.OperationStepCard
import com.project.semsobra.ui.components.ProductionSummaryCard
import com.project.semsobra.ui.components.SectionTitle
import com.project.semsobra.ui.model.FoodUiModel
import com.project.semsobra.ui.util.dayName
import com.project.semsobra.ui.util.formatDate
import java.time.LocalDate

@Composable
fun HomeScreen(
    analytics: AnalyticsResult,
    forecastDate: LocalDate,
    foods: List<FoodUiModel>,
    summaries: List<ProductionSummary>,
    cidadeAtual: Cidade = Cidade.SANTIAGO_RS,
    climaPrevisto: CondicaoClima = CondicaoClima.ENSOLARADO,
    onCidadeSelecionada: (Cidade) -> Unit = {},
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val progressPercent = when {
            todaySummary == null -> 0.0f
            !todaySummary.fechado -> 0.5f
            else -> 1.0f
        }
        val progressLabel = when {
            todaySummary == null -> "Progresso: Nenhuma etapa iniciada"
            !todaySummary.fechado -> "Progresso: Produção salva (50%)"
            else -> "Progresso: Dia 100% concluído!"
        }

        item {
            HeaderCard(
                title = "Operação de hoje",
                value = "${formatDate(today.toString())} - ${dayName(today.dayOfWeek.value)}",
                subtitle = "Acompanhe e registre a produção e as sobras do seu buffet.",
                progressPercent = progressPercent,
                progressLabel = progressLabel
            )
        }

        item {
            CitySelectorCard(
                cidadeAtual = cidadeAtual,
                climaPrevisto = climaPrevisto,
                onCidadeSelecionada = onCidadeSelecionada
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Hero Card 1: Cozinha / Preparo
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenProduction),
                    colors = CardDefaults.cardColors(
                        containerColor = if (todaySummary == null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        if (todaySummary == null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "1",
                                style = MaterialTheme.typography.headlineLarge,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Restaurant,
                                    contentDescription = null,
                                    modifier = Modifier.padding(6.dp).size(20.dp)
                                )
                            }
                        }
                        Text(
                            "1. Cozinha",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (todaySummary == null) "Registrar comida de hoje" else "Registrado (${todaySummary.items.size} itens)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (todaySummary == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (todaySummary == null) "Fazer agora ➔" else "Concluído ✓",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (todaySummary == null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Hero Card 2: Fechamento / Sobras
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenClosing),
                    colors = CardDefaults.cardColors(
                        containerColor = if (todaySummary != null && !todaySummary.fechado) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        if (todaySummary != null && !todaySummary.fechado) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "2",
                                style = MaterialTheme.typography.headlineLarge,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = if (todaySummary?.fechado == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.padding(6.dp).size(20.dp)
                                )
                            }
                        }
                        Text(
                            "2. Sobras",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (todaySummary?.fechado == true) "Fechado com sucesso" else if (todaySummary != null) "Registrar o que sobrou" else "Aguardando fim do dia",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                todaySummary?.fechado == true -> MaterialTheme.colorScheme.surfaceVariant
                                todaySummary != null -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            }
                        ) {
                            Text(
                                text = when {
                                    todaySummary?.fechado == true -> "Concluído ✓"
                                    todaySummary != null -> "Fazer agora ➔"
                                    else -> "Aguardando"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    todaySummary?.fechado == true -> MaterialTheme.colorScheme.onSurfaceVariant
                                    todaySummary != null -> MaterialTheme.colorScheme.onPrimary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
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
