package com.project.semsobra.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.semsobra.domain.previsao.model.QualidadePrevisao
import com.project.semsobra.domain.previsao.model.ResultadoPrevisao
import com.project.semsobra.domain.previsao.model.Turno
import com.project.semsobra.ui.components.EmptyState
import com.project.semsobra.ui.components.ForecastItemCard
import com.project.semsobra.ui.components.HeaderCard
import com.project.semsobra.ui.components.MetricCard
import com.project.semsobra.ui.components.ProductionSummaryCard
import com.project.semsobra.ui.components.SectionTitle
import com.project.semsobra.ui.components.SimpleMetricRow
import com.project.semsobra.ui.model.AnalyticsResult
import com.project.semsobra.ui.model.ProductionSummary
import com.project.semsobra.ui.util.formatQuantity
import com.project.semsobra.ui.util.formatDate
import com.project.semsobra.ui.util.formatPercentage
import com.project.semsobra.ui.util.formatWeightOneDecimal

@Composable
fun AnalysisScreen(
    analytics: AnalyticsResult,
    previsaoDemanda: ResultadoPrevisao,
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
                title = "Análise da operação",
                value = "${previsaoDemanda.clientesPrevistos} clientes previstos",
                subtitle = "Histórico, previsão e pontos de atenção para o próximo preparo."
            )
        }
        item { SectionTitle("Previsão de demanda") }
        item {
            HeaderCard(
                title = "${formatDate(previsaoDemanda.dataPrevisao.toString())} • ${previsaoDemanda.turno.label()}",
                value = "${previsaoDemanda.faixaHistoricaClientes.minimoEstimado} a " +
                    "${previsaoDemanda.faixaHistoricaClientes.maximoEstimado} clientes",
                subtitle = "Faixa histórica estimada; não representa uma garantia estatística."
            )
        }
        item { EmptyState(previsaoDemanda.mensagem) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Consumo por cliente",
                    value = "${formatWeightOneDecimal(previsaoDemanda.consumoMedioKgPorCliente)} kg",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Demanda prevista",
                    value = "${formatWeightOneDecimal(previsaoDemanda.demandaPrevistaKg)} kg",
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            SimpleMetricRow(
                "Preparo recomendado (+${formatPercentage(previsaoDemanda.margemSegurancaAplicada)}%)",
                "${formatWeightOneDecimal(previsaoDemanda.preparoRecomendadoKg)} kg"
            )
        }
        item {
            SimpleMetricRow(
                "Produção inicial (${formatPercentage(previsaoDemanda.percentualProducaoInicialAplicado)}%)",
                "${formatWeightOneDecimal(previsaoDemanda.producaoInicialKg)} kg"
            )
        }
        item {
            SimpleMetricRow(
                "Reserva para reposição (" +
                    "${formatPercentage(1.0 - previsaoDemanda.percentualProducaoInicialAplicado)}%)",
                "${formatWeightOneDecimal(previsaoDemanda.reservaReposicaoKg)} kg"
            )
        }
        item {
            SimpleMetricRow(
                "Qualidade da previsão",
                previsaoDemanda.qualidade.label()
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
        item { SectionTitle("Previsão por preparo") }
        if (analytics.forecast.items.isEmpty()) {
            item { EmptyState("Cadastre preparos e feche alguns dias para gerar previsões.") }
        } else {
            items(analytics.forecast.items, key = { it.food.id }) { item ->
                ForecastItemCard(item, explain = true)
            }
        }
        item { SectionTitle("Preparos com mais sobra") }
        if (report.alimentosComMaisSobra.isEmpty()) {
            item { EmptyState("Ainda não há sobras registradas.") }
        } else {
            items(report.alimentosComMaisSobra, key = { it.food.id }) { metric ->
                SimpleMetricRow(metric.food.nome, "${formatQuantity(metric.quantidade)} ${metric.food.unidadeMedida}")
            }
        }
        item { SectionTitle("Preparos que mais acabaram") }
        if (report.alimentosQueMaisAcabaram.isEmpty()) {
            item { EmptyState("Nenhum preparo marcado como falta até agora.") }
        } else {
            items(report.alimentosQueMaisAcabaram, key = { it.food.id }) { metric ->
                SimpleMetricRow(metric.food.nome, "${metric.quantidade.toInt()} ocorrência(s)")
            }
        }
        item { SectionTitle("Histórico recente") }
        if (summaries.isEmpty()) {
            item { EmptyState("O histórico aparecerá depois da primeira produção.") }
        } else {
            items(summaries.take(5), key = { it.day.id }) { summary ->
                ProductionSummaryCard(summary, showItems = true)
            }
        }
    }
}

private fun QualidadePrevisao.label(): String = when (this) {
    QualidadePrevisao.BAIXA -> "Baixa"
    QualidadePrevisao.MEDIA -> "Média"
    QualidadePrevisao.ALTA -> "Alta"
}

private fun Turno.label(): String = when (this) {
    Turno.CAFE_DA_MANHA -> "Café da manhã"
    Turno.ALMOCO -> "Almoço"
    Turno.JANTAR -> "Jantar"
}
