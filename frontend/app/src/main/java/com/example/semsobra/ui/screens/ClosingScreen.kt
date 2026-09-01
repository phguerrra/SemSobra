package com.example.semsobra.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.semsobra.ui.components.EmptyState
import com.example.semsobra.ui.components.HeaderCard
import com.example.semsobra.ui.components.SectionTitle
import com.example.semsobra.ui.model.ProductionItemDisplay
import com.example.semsobra.ui.model.ProductionItemUiModel
import com.example.semsobra.ui.model.ProductionSummary
import com.example.semsobra.ui.util.formatDate
import com.example.semsobra.ui.util.formatInput
import com.example.semsobra.ui.util.formatQuantity
import com.example.semsobra.ui.util.parseDouble

@Composable
fun ClosingScreen(
    summaries: List<ProductionSummary>,
    onClose: (Long, Int, List<ProductionItemUiModel>) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedId by rememberSaveable { androidx.compose.runtime.mutableStateOf<Long?>(null) }
    val selected = summaries.firstOrNull { it.day.id == selectedId } ?: summaries.firstOrNull()
    var clientes by rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    val leftovers = remember { mutableStateMapOf<Long, String>() }
    val ranOut = remember { mutableStateMapOf<Long, Boolean>() }
    val ranOutTime = remember { mutableStateMapOf<Long, String>() }

    LaunchedEffect(selected?.day?.id) {
        selected?.let { summary ->
            selectedId = summary.day.id
            clientes = if (summary.day.clientesAtendidos > 0) summary.day.clientesAtendidos.toString() else ""
            leftovers.clear()
            ranOut.clear()
            ranOutTime.clear()
            summary.items.forEach { display ->
                leftovers[display.item.id] = if (display.item.quantidadeSobra > 0) {
                    formatInput(display.item.quantidadeSobra)
                } else {
                    ""
                }
                ranOut[display.item.id] = display.item.acabouAntesDoFim
                ranOutTime[display.item.id] = display.item.horarioAcabou.orEmpty()
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (summaries.isEmpty()) {
            item { EmptyState("Nenhuma producao registrada para fechar.") }
        } else {
            item {
                HeaderCard(
                    title = "Fechamento do buffet",
                    value = selected?.day?.data?.let(::formatDate).orEmpty(),
                    subtitle = "Informe clientes, sobra em kg e faltas antes do fim do atendimento."
                )
            }
            item { SectionTitle("Producao selecionada") }
            items(summaries, key = { it.day.id }) { summary ->
                ProductionSelectorCard(
                    summary = summary,
                    selected = summary.day.id == selected?.day?.id,
                    onClick = { selectedId = summary.day.id }
                )
            }
            item {
                OutlinedTextField(
                    value = clientes,
                    onValueChange = { clientes = it },
                    label = { Text("Clientes atendidos") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            selected?.items?.forEach { display ->
                item(key = display.item.id) {
                    ClosingItemCard(
                        display = display,
                        leftover = leftovers[display.item.id].orEmpty(),
                        ranOut = ranOut[display.item.id] ?: false,
                        time = ranOutTime[display.item.id].orEmpty(),
                        onLeftoverChange = { leftovers[display.item.id] = it },
                        onRanOutChange = { ranOut[display.item.id] = it },
                        onTimeChange = { ranOutTime[display.item.id] = it }
                    )
                }
            }
            item {
                Button(
                    onClick = {
                        selected?.let { summary ->
                            val items = summary.items.map { display ->
                                val didRunOut = ranOut[display.item.id] ?: false
                                display.item.copy(
                                    quantidadeSobra = parseDouble(leftovers[display.item.id].orEmpty()),
                                    acabouAntesDoFim = didRunOut,
                                    horarioAcabou = ranOutTime[display.item.id]?.takeIf { didRunOut && it.isNotBlank() }
                                )
                            }
                            onClose(summary.day.id, clientes.toIntOrNull() ?: 0, items)
                        }
                    },
                    enabled = selected != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Salvar fechamento")
                }
            }
        }
    }
}

@Composable
private fun ProductionSelectorCard(
    summary: ProductionSummary,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(formatDate(summary.day.data), fontWeight = FontWeight.Bold)
                Text("${summary.items.size} preparo(s)", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilterChip(
                selected = selected,
                onClick = onClick,
                label = { Text(if (summary.fechado) "Fechado" else "Aberto") }
            )
        }
    }
}

@Composable
private fun ClosingItemCard(
    display: ProductionItemDisplay,
    leftover: String,
    ranOut: Boolean,
    time: String,
    onLeftoverChange: (String) -> Unit,
    onRanOutChange: (Boolean) -> Unit,
    onTimeChange: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(display.food.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (display.food.descricao.isNotBlank()) {
                Text(
                    display.food.descricao,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("Produzido: ${formatQuantity(display.item.quantidadeProduzida)} ${display.food.unidadeMedida}")
            HorizontalDivider()
            OutlinedTextField(
                value = leftover,
                onValueChange = onLeftoverChange,
                label = { Text("Sobra (${display.food.unidadeMedida})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = ranOut, onCheckedChange = onRanOutChange)
                Text("Acabou antes do fim")
            }
            if (ranOut) {
                OutlinedTextField(
                    value = time,
                    onValueChange = onTimeChange,
                    label = { Text("Horario em que acabou") },
                    placeholder = { Text("Ex.: 13:40") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
