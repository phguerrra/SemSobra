package com.project.semsobra.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.project.semsobra.R
import com.project.semsobra.domain.usecase.ValidarDadosProducaoUseCase
import com.project.semsobra.ui.SaveStatus
import com.project.semsobra.ui.components.EmptyState
import com.project.semsobra.ui.components.HeaderCard
import com.project.semsobra.ui.components.SectionTitle
import com.project.semsobra.domain.model.ProductionItemDisplay
import com.project.semsobra.domain.model.ItemProducao
import com.project.semsobra.domain.model.ProductionSummary
import com.project.semsobra.ui.util.formatDate
import com.project.semsobra.ui.util.formatInput
import com.project.semsobra.ui.util.formatQuantity
import com.project.semsobra.ui.util.isDecimalInput
import com.project.semsobra.ui.util.isIntegerInput
import com.project.semsobra.ui.util.parseDoubleOrNull

@Composable
fun ClosingScreen(
    summaries: List<ProductionSummary>,
    saveStatus: SaveStatus,
    onClose: (Long, Int, List<ItemProducao>) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedId by rememberSaveable { androidx.compose.runtime.mutableStateOf<Long?>(null) }
    val selected = summaries.firstOrNull { it.day.id == selectedId }
        ?: summaries.firstOrNull { !it.fechado }
        ?: summaries.firstOrNull()
    var clientes by rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    var leftovers by rememberSaveable { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var ranOut by rememberSaveable { mutableStateOf<Map<Long, Boolean>>(emptyMap()) }
    var ranOutTime by rememberSaveable { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var loadedSelectionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var clientesErro by remember { mutableStateOf<String?>(null) }
    val leftoverErrors = remember { mutableStateMapOf<Long, String>() }
    val timeErrors = remember { mutableStateMapOf<Long, String>() }
    val validator = remember { ValidarDadosProducaoUseCase() }
    val invalidNumberMessage = stringResource(R.string.error_invalid_number)
    val invalidIntegerMessage = stringResource(R.string.error_invalid_integer)
    val inputsEnabled = saveStatus != SaveStatus.SAVING

    LaunchedEffect(selected?.day?.id) {
        selected?.let { summary ->
            selectedId = summary.day.id
            if (loadedSelectionId == summary.day.id) return@let
            loadedSelectionId = summary.day.id
            clientes = if (summary.day.clientesAtendidos > 0) summary.day.clientesAtendidos.toString() else ""
            leftovers = emptyMap()
            ranOut = emptyMap()
            ranOutTime = emptyMap()
            clientesErro = null
            leftoverErrors.clear()
            timeErrors.clear()
            summary.items.forEach { display ->
                leftovers = leftovers + (display.item.id to if (display.item.quantidadeSobra > 0) {
                    formatInput(display.item.quantidadeSobra)
                } else {
                    "0"
                })
                ranOut = ranOut + (display.item.id to display.item.acabouAntesDoFim)
                ranOutTime = ranOutTime + (display.item.id to display.item.horarioAcabou.orEmpty())
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (summaries.isEmpty()) {
            item { EmptyState(stringResource(R.string.closing_empty)) }
        } else {
            item {
                HeaderCard(
                    title = stringResource(R.string.closing_title),
                    value = selected?.day?.data?.let(::formatDate).orEmpty(),
                    subtitle = stringResource(R.string.closing_description)
                )
            }
            item { SectionTitle(stringResource(R.string.closing_selected_production)) }
            items(summaries, key = { "production-${it.day.id}" }) { summary ->
                ProductionSelectorCard(
                    summary = summary,
                    selected = summary.day.id == selected?.day?.id,
                    enabled = inputsEnabled,
                    onClick = { selectedId = summary.day.id }
                )
            }
            item {
                OutlinedTextField(
                    value = clientes,
                    enabled = inputsEnabled,
                    onValueChange = {
                        if (isIntegerInput(it)) {
                            clientes = it
                            clientesErro = null
                        }
                    },
                    label = { Text(stringResource(R.string.label_customers_served)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = clientesErro != null,
                    supportingText = clientesErro?.let { message -> { Text(message) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            selected?.items?.forEach { display ->
                item(key = "closing-item-${display.item.id}") {
                    ClosingItemCard(
                        display = display,
                        leftover = leftovers[display.item.id].orEmpty(),
                        ranOut = ranOut[display.item.id] ?: false,
                        time = ranOutTime[display.item.id].orEmpty(),
                        leftoverError = leftoverErrors[display.item.id],
                        timeError = timeErrors[display.item.id],
                        enabled = inputsEnabled,
                        onLeftoverChange = {
                            leftovers = leftovers + (display.item.id to it)
                            leftoverErrors.remove(display.item.id)
                        },
                        onRanOutChange = {
                            ranOut = ranOut + (display.item.id to it)
                            if (!it) {
                                ranOutTime = ranOutTime + (display.item.id to "")
                                timeErrors.remove(display.item.id)
                            }
                        },
                        onTimeChange = {
                            ranOutTime = ranOutTime + (display.item.id to it)
                            timeErrors.remove(display.item.id)
                        }
                    )
                }
            }
            item {
                Button(
                    onClick = {
                        selected?.let { summary ->
                            val clientesAtendidos = clientes.toIntOrNull()
                            clientesErro = if (clientesAtendidos == null) {
                                invalidIntegerMessage
                            } else {
                                runCatching { validator.validarClientesAtendidos(clientesAtendidos) }
                                    .exceptionOrNull()
                                    ?.message
                            }

                            val novosErrosDeSobra = mutableMapOf<Long, String>()
                            val novosErrosDeHorario = mutableMapOf<Long, String>()
                            val items = summary.items.mapNotNull { display ->
                                val itemId = display.item.id
                                val didRunOut = ranOut[display.item.id] ?: false
                                val leftover = parseDoubleOrNull(leftovers[itemId].orEmpty())

                                if (leftover == null) {
                                    novosErrosDeSobra[itemId] = invalidNumberMessage
                                } else {
                                    runCatching {
                                        validator.validarSobra(display.item.quantidadeProduzida, leftover)
                                    }.exceptionOrNull()?.message?.let { message ->
                                        novosErrosDeSobra[itemId] = message
                                    }
                                }

                                runCatching {
                                    validator.validarHorario(didRunOut, ranOutTime[itemId])
                                }.exceptionOrNull()?.message?.let { message ->
                                    novosErrosDeHorario[itemId] = message
                                }

                                if (leftover == null) {
                                    null
                                } else {
                                    display.item.copy(
                                        quantidadeSobra = leftover,
                                        acabouAntesDoFim = didRunOut,
                                        horarioAcabou = ranOutTime[itemId]
                                            ?.trim()
                                            ?.takeIf { didRunOut && it.isNotBlank() }
                                    )
                                }
                            }

                            leftoverErrors.clear()
                            leftoverErrors.putAll(novosErrosDeSobra)
                            timeErrors.clear()
                            timeErrors.putAll(novosErrosDeHorario)

                            if (
                                clientesErro == null &&
                                novosErrosDeSobra.isEmpty() &&
                                novosErrosDeHorario.isEmpty() &&
                                items.size == summary.items.size
                            ) {
                                onClose(summary.day.id, clientesAtendidos!!, items)
                            }
                        }
                    },
                    enabled = selected != null && !selected.fechado &&
                        saveStatus != SaveStatus.SAVING,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (saveStatus == SaveStatus.SAVING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.status_saving))
                    } else {
                        Text(stringResource(R.string.action_save_closing))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductionSelectorCard(summary: ProductionSummary, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(formatDate(summary.day.data), fontWeight = FontWeight.Bold)
                Text(
                    pluralStringResource(R.plurals.preparation_count, summary.items.size, summary.items.size),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilterChip(
                selected = selected,
                enabled = enabled,
                onClick = onClick,
                label = { Text(stringResource(if (summary.fechado) R.string.status_closed else R.string.status_open)) }
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
    leftoverError: String?,
    timeError: String?,
    enabled: Boolean,
    onLeftoverChange: (String) -> Unit,
    onRanOutChange: (Boolean) -> Unit,
    onTimeChange: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(display.food.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (display.food.descricao.isNotBlank()) {
                Text(
                    display.food.descricao,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                stringResource(
                    R.string.quantity_produced_summary,
                    formatQuantity(display.item.quantidadeProduzida),
                    display.food.unidadeMedida
                )
            )
            HorizontalDivider()
            OutlinedTextField(
                value = leftover,
                enabled = enabled,
                onValueChange = { if (isDecimalInput(it)) onLeftoverChange(it) },
                label = { Text(stringResource(R.string.label_leftover, display.food.unidadeMedida)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = leftoverError != null,
                supportingText = leftoverError?.let { message -> { Text(message) } },
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = ranOut, onCheckedChange = onRanOutChange, enabled = enabled)
                Text(stringResource(R.string.label_ran_out_early))
            }
            if (ranOut) {
                OutlinedTextField(
                    value = time,
                    enabled = enabled,
                    onValueChange = onTimeChange,
                    label = { Text(stringResource(R.string.label_ran_out_time)) },
                    placeholder = { Text(stringResource(R.string.hint_ran_out_time)) },
                    singleLine = true,
                    isError = timeError != null,
                    supportingText = timeError?.let { message -> { Text(message) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
