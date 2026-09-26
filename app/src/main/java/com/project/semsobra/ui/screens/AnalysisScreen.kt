package com.project.semsobra.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.project.semsobra.domain.previsao.model.QualidadePrevisao
import com.project.semsobra.domain.previsao.model.ResultadoPrevisao
import com.project.semsobra.domain.previsao.model.Turno
import com.project.semsobra.ui.components.EmptyState
import com.project.semsobra.ui.components.HeaderCard
import com.project.semsobra.ui.components.MetricCard
import com.project.semsobra.ui.components.ProductionSummaryCard
import com.project.semsobra.ui.components.SectionTitle
import com.project.semsobra.ui.components.SimpleMetricRow
import com.project.semsobra.ui.model.AnalyticsResult
import com.project.semsobra.domain.model.ProductionSummary
import com.project.semsobra.domain.model.ProductionItemUiModel
import com.project.semsobra.domain.model.QuantityPolicy
import com.project.semsobra.ui.util.formatQuantity
import com.project.semsobra.ui.util.formatDate
import com.project.semsobra.ui.util.formatPercentage
import com.project.semsobra.ui.util.formatWeightOneDecimal
import com.project.semsobra.ui.components.FormCard
import androidx.compose.ui.text.font.FontWeight
import com.project.semsobra.ui.util.dayName
import com.project.semsobra.ui.util.formatInput
import com.project.semsobra.ui.util.parseDoubleOrNull
import com.project.semsobra.ui.SaveStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun AnalysisScreen(
    analytics: AnalyticsResult,
    previsaoDemanda: ResultadoPrevisao,
    summaries: List<ProductionSummary>,
    saveStatus: SaveStatus,
    onSaveHistory: (Long, Int, List<ProductionItemUiModel>) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var editingSummary by remember { mutableStateOf<ProductionSummary?>(null) }
    var pendingReopen by remember { mutableStateOf<ProductionSummary?>(null) }
    val report = analytics.report
    val totalFaltas = report.alimentosQueMaisAcabaram.sumOf { it.quantidade }.toInt()
    val totalSobras = report.totalSobrasPorUnidade
        .joinToString(" • ") { "${formatQuantity(it.quantidade)} ${it.unidadeMedida}" }
        .ifBlank { "0" }

    editingSummary?.let { summary ->
        HistoryEditDialog(
            summary = summary,
            saving = saveStatus == SaveStatus.SAVING,
            onDismiss = { if (saveStatus != SaveStatus.SAVING) editingSummary = null },
            onSave = { customers, items ->
                onSaveHistory(summary.day.id, customers, items)
                editingSummary = null
            }
        )
    }
    pendingReopen?.let { summary ->
        ReopenConfirmationDialog(
            summary = summary,
            onDismiss = { pendingReopen = null },
            onConfirm = {
                pendingReopen = null
                editingSummary = summary
            }
        )
    }

    androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.background
        ) {
            listOf("Resumo", "Cálculo", "Histórico").forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        if (selectedTab == 2) {
            AnalysisHistory(
                summaries = summaries,
                onEdit = { pendingReopen = it },
                modifier = Modifier.fillMaxSize()
            )
            return@Column
        }
        if (selectedTab == 1) {
            CalculationDetails(
                previsaoDemanda = previsaoDemanda,
                modifier = Modifier.fillMaxSize()
            )
            return@Column
        }
        LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeaderCard(
                title = "Próximo serviço • ${previsaoDemanda.turno.label()}",
                value = "${previsaoDemanda.clientesPrevistos} clientes previstos",
                subtitle = formatDate(previsaoDemanda.dataPrevisao.toString())
            )
        }
        item { SectionTitle("Plano recomendado") }
        item {
            HeaderCard(
                title = "Total para deixar pronto",
                value = "${formatWeightOneDecimal(previsaoDemanda.preparoRecomendadoKg)} kg",
                subtitle = "Comece com ${formatWeightOneDecimal(previsaoDemanda.producaoInicialKg)} kg e reserve " +
                    "${formatWeightOneDecimal(previsaoDemanda.reservaReposicaoKg)} kg para repor quando necessário."
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Coloque primeiro",
                    value = "${formatWeightOneDecimal(previsaoDemanda.producaoInicialKg)} kg",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Guarde para repor",
                    value = "${formatWeightOneDecimal(previsaoDemanda.reservaReposicaoKg)} kg",
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item { EmptyState("Confira o movimento antes de usar a reserva. Assim você reduz sobras sem deixar faltar.") }
        item { SectionTitle("Plano por alimento") }
        if (analytics.forecast.items.isEmpty()) {
            item { EmptyState("Cadastre preparos e feche alguns dias para receber recomendações por item.") }
        } else {
            items(analytics.forecast.items, key = { "forecast-${it.food.id}" }) { item ->
                val initial = QuantityPolicy.normalize(
                    item.quantidadeRecomendada * previsaoDemanda.percentualProducaoInicialAplicado
                )
                val reserve = QuantityPolicy.subtract(item.quantidadeRecomendada, initial)
                PreparationFoodCard(
                    name = item.food.nome,
                    unit = item.food.unidadeMedida,
                    initial = initial,
                    reserve = reserve,
                    total = item.quantidadeRecomendada
                )
            }
        }
        item { SectionTitle("Base da previsão") }
        item {
            HistoricalAverageCard(
                summaries = summaries,
                previsaoDemanda = previsaoDemanda
            )
        }
        item { SectionTitle("Pontos de atenção") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Sobra registrada",
                    value = totalSobras,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Faltas",
                    value = totalFaltas.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item { SectionTitle("Preparos com mais sobra") }
        if (report.alimentosComMaisSobra.isEmpty()) {
            item { EmptyState("Ainda não há sobras registradas.") }
        } else {
            items(report.alimentosComMaisSobra, key = { "leftover-${it.food.id}" }) { metric ->
                SimpleMetricRow(metric.food.nome, "${formatQuantity(metric.quantidade)} ${metric.food.unidadeMedida}")
            }
        }
        item { SectionTitle("Preparos que mais acabaram") }
        if (report.alimentosQueMaisAcabaram.isEmpty()) {
            item { EmptyState("Nenhum preparo marcado como falta até agora.") }
        } else {
            items(report.alimentosQueMaisAcabaram, key = { "shortage-${it.food.id}" }) { metric ->
                SimpleMetricRow(metric.food.nome, "${metric.quantidade.toInt()} ocorrência(s)")
            }
        }
        }
    }
}

@Composable
private fun ReopenConfirmationDialog(
    summary: ProductionSummary,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val date = remember(summary.day.data) {
        runCatching { LocalDate.parse(summary.day.data) }.getOrNull()
    }
    val isOld = date?.let { ChronoUnit.DAYS.between(it, LocalDate.now()) > 7 } == true
    val expectedConfirmation = formatDate(summary.day.data)
    var confirmation by remember(summary.day.id) { mutableStateOf("") }
    val allowed = !isOld || confirmation.trim() == expectedConfirmation

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reabrir este fechamento?") },
        text = {
            androidx.compose.foundation.layout.Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "As informações de ${formatDate(summary.day.data)} poderão ser alteradas. " +
                        "Ao salvar, o histórico, as métricas e a previsão serão recalculados."
                )
                if (isOld) {
                    Text(
                        "Este é um registro antigo. Para evitar alterações acidentais, " +
                            "digite a data $expectedConfirmation.",
                        color = MaterialTheme.colorScheme.error
                    )
                    OutlinedTextField(
                        value = confirmation,
                        onValueChange = { confirmation = it },
                        label = { Text("Confirme a data") },
                        placeholder = { Text("dd/mm/aaaa") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        confirmButton = {
            Button(onClick = onConfirm, enabled = allowed) { Text("Reabrir fechamento") }
        }
    )
}

@Composable
private fun PreparationFoodCard(
    name: String,
    unit: String,
    initial: Double,
    reserve: Double,
    total: Double
) {
    FormCard {
        Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Prepare ${formatQuantity(total)} $unit no total.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SimpleMetricRow("Coloque agora", "${formatQuantity(initial)} $unit")
        SimpleMetricRow("Deixe reservado", "${formatQuantity(reserve)} $unit")
    }
}

@Composable
private fun HistoricalAverageCard(
    summaries: List<ProductionSummary>,
    previsaoDemanda: ResultadoPrevisao
) {
    val targetDay = previsaoDemanda.dataPrevisao.dayOfWeek.value
    val comparable = summaries
        .asSequence()
        .filter { summary ->
            summary.fechado &&
                summary.day.restauranteAberto &&
                summary.day.clientesAtendidos > 0 &&
                summary.day.diaDaSemana == targetDay &&
                summary.day.turno == previsaoDemanda.turno
        }
        .take(4)
        .toList()

    if (comparable.isEmpty()) {
        EmptyState(
            "Ainda não há ${dayName(targetDay)}s fechadas neste turno. " +
                "A previsão será mais clara após novos registros."
        )
        return
    }

    val averageCustomers = comparable.sumOf { it.day.clientesAtendidos }.toDouble() / comparable.size
    val averageFood = QuantityPolicy.normalize(
        comparable.sumOf { summary ->
            QuantityPolicy.sum(
                summary.items
                    .filter { it.food.unidadeMedida.equals("kg", ignoreCase = true) }
                    .map { it.consumo }
            )
        } / comparable.size
    )
    val pluralDay = dayName(targetDay).replace("-feira", "-feiras") +
        if (targetDay >= 6) "s" else ""

    FormCard {
        Text(
            "Últimas ${comparable.size} $pluralDay",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Em média, foram atendidas ${formatWeightOneDecimal(averageCustomers)} pessoas " +
                "e consumidos ${formatWeightOneDecimal(averageFood)} kg de comida.",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            "Para o próximo serviço, a previsão é de ${previsaoDemanda.clientesPrevistos} pessoas.",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CalculationDetails(
    previsaoDemanda: ResultadoPrevisao,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeaderCard(
                title = "Como chegamos à recomendação",
                value = "${previsaoDemanda.quantidadeRegistrosUtilizados} registros analisados",
                subtitle = "Detalhes para você conferir a previsão."
            )
        }
        item { EmptyState(previsaoDemanda.mensagem) }
        item { SectionTitle("Dados considerados") }
        item {
            SimpleMetricRow(
                "Faixa de clientes",
                "${previsaoDemanda.faixaHistoricaClientes.minimoEstimado} a " +
                    previsaoDemanda.faixaHistoricaClientes.maximoEstimado
            )
        }
        item {
            SimpleMetricRow(
                "Consumo médio por cliente",
                "${formatWeightOneDecimal(previsaoDemanda.consumoMedioKgPorCliente)} kg"
            )
        }
        item {
            SimpleMetricRow(
                "Necessidade estimada",
                "${formatWeightOneDecimal(previsaoDemanda.demandaPrevistaKg)} kg"
            )
        }
        item { SectionTitle("Margens aplicadas") }
        item {
            SimpleMetricRow(
                "Margem de segurança",
                formatPercentage(previsaoDemanda.margemSegurancaAplicada)
            )
        }
        item {
            SimpleMetricRow(
                "Parcela colocada primeiro",
                formatPercentage(previsaoDemanda.percentualProducaoInicialAplicado)
            )
        }
        item {
            SimpleMetricRow("Confiança da previsão", previsaoDemanda.qualidade.label())
        }
    }
}

@Composable
private fun AnalysisHistory(
    summaries: List<ProductionSummary>,
    onEdit: (ProductionSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeaderCard(
                title = "Histórico de produção",
                value = "${summaries.size} ${if (summaries.size == 1) "registro" else "registros"}",
                subtitle = "Produção, consumo e sobras organizados por dia."
            )
        }
        if (summaries.isEmpty()) {
            item { EmptyState("O histórico aparecerá depois da primeira produção fechada.") }
        } else {
            items(summaries, key = { "history-${it.day.id}" }) { summary ->
                ProductionSummaryCard(
                    summary = summary,
                    showItems = true,
                    onEdit = { onEdit(summary) }
                )
            }
        }
    }
}

@Composable
private fun HistoryEditDialog(
    summary: ProductionSummary,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (Int, List<ProductionItemUiModel>) -> Unit
) {
    var customers by remember(summary.day.id) {
        mutableStateOf(summary.day.clientesAtendidos.toString())
    }
    val produced = remember(summary.day.id) {
        mutableStateMapOf<Long, String>().apply {
            summary.items.forEach { put(it.item.id, formatInput(it.item.quantidadeProduzida)) }
        }
    }
    val leftovers = remember(summary.day.id) {
        mutableStateMapOf<Long, String>().apply {
            summary.items.forEach { put(it.item.id, formatInput(it.item.quantidadeSobra)) }
        }
    }
    val shortages = remember(summary.day.id) {
        mutableStateMapOf<Long, Boolean>().apply {
            summary.items.forEach { put(it.item.id, it.item.acabouAntesDoFim) }
        }
    }
    var error by remember(summary.day.id) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar ${formatDate(summary.day.data)}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text(
                        "Corrija os dados registrados. A previsão será recalculada após salvar.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    OutlinedTextField(
                        value = customers,
                        onValueChange = { customers = it; error = null },
                        label = { Text("Clientes atendidos") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                items(summary.items, key = { "edit-${it.item.id}" }) { display ->
                    FormCard {
                        Text(display.food.nome, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = produced[display.item.id].orEmpty(),
                            onValueChange = { produced[display.item.id] = it; error = null },
                            label = { Text("Quantidade produzida (${display.food.unidadeMedida})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = leftovers[display.item.id].orEmpty(),
                            onValueChange = { leftovers[display.item.id] = it; error = null },
                            label = { Text("Sobra (${display.food.unidadeMedida})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row {
                            Checkbox(
                                checked = shortages[display.item.id] == true,
                                onCheckedChange = { shortages[display.item.id] = it }
                            )
                            Text("Acabou antes do fim", modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                }
                error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancelar") }
        },
        confirmButton = {
            Button(
                enabled = !saving,
                onClick = {
                    val parsedCustomers = customers.toIntOrNull()
                    val parsedItems = summary.items.mapNotNull { display ->
                        val parsedProduced = parseDoubleOrNull(produced[display.item.id].orEmpty())
                        val parsedLeftover = parseDoubleOrNull(leftovers[display.item.id].orEmpty())
                        if (parsedProduced != null && parsedLeftover != null) {
                            display.item.copy(
                                quantidadeProduzida = parsedProduced,
                                quantidadeSobra = parsedLeftover,
                                acabouAntesDoFim = shortages[display.item.id] == true,
                                horarioAcabou = display.item.horarioAcabou
                                    ?.takeIf { shortages[display.item.id] == true }
                            )
                        } else null
                    }
                    error = when {
                        parsedCustomers == null || parsedCustomers <= 0 -> "Informe uma quantidade válida de clientes."
                        parsedItems.size != summary.items.size -> "Confira as quantidades informadas."
                        parsedItems.any { it.quantidadeProduzida <= 0.0 } -> "A quantidade produzida deve ser maior que zero."
                        parsedItems.any { it.quantidadeSobra > it.quantidadeProduzida } -> "A sobra não pode ser maior que a quantidade produzida."
                        else -> null
                    }
                    if (error == null) onSave(parsedCustomers!!, parsedItems)
                }
            ) {
                if (saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Salvar alterações")
            }
        }
    )
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
