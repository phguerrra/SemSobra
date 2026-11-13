package com.project.semsobra.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.project.semsobra.R
import com.project.semsobra.domain.model.ItemProducao
import com.project.semsobra.domain.model.ProductionItemDisplay
import com.project.semsobra.domain.model.ProductionSummary
import com.project.semsobra.domain.usecase.ValidarDadosProducaoUseCase
import com.project.semsobra.ui.SaveStatus
import com.project.semsobra.ui.components.EmptyState
import com.project.semsobra.ui.components.HeaderCard
import com.project.semsobra.ui.components.ReportExportDialog
import com.project.semsobra.ui.components.SectionTitle
import com.project.semsobra.ui.util.buildWhatsAppSummaryText
import com.project.semsobra.ui.util.formatDate
import com.project.semsobra.ui.util.formatInput
import com.project.semsobra.ui.util.shareText
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

    val leftoversMap = remember { mutableStateMapOf<Long, String>() }
    val ranOutMap = remember { mutableStateMapOf<Long, Boolean>() }
    val ranOutTimeMap = remember { mutableStateMapOf<Long, String>() }

    var loadedSelectionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingSelectionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var clientesErro by remember { mutableStateOf<String?>(null) }
    val leftoverErrors = remember { mutableStateMapOf<Long, String>() }
    val timeErrors = remember { mutableStateMapOf<Long, String>() }
    val validator = remember { ValidarDadosProducaoUseCase() }
    val invalidNumberMessage = stringResource(R.string.error_invalid_number)
    val invalidIntegerMessage = stringResource(R.string.error_invalid_integer)
    val inputsEnabled = saveStatus != SaveStatus.SAVING
    val canEdit = inputsEnabled && selected?.fechado == false

    val hasChanges = selected?.let { summary ->
        loadedSelectionId == summary.day.id && (
            (if (summary.day.clientesAtendidos > 0) clientes.toIntOrNull() != summary.day.clientesAtendidos
            else clientes.isNotEmpty()) ||
                summary.items.any { (item) ->
                    parseDoubleOrNull(leftoversMap[item.id].orEmpty()) != item.quantidadeSobra ||
                        (ranOutMap[item.id] ?: false) != item.acabouAntesDoFim ||
                        ranOutTimeMap[item.id].orEmpty().trim() != item.horarioAcabou.orEmpty().trim()
                }
        )
    } ?: false

    val context = LocalContext.current
    var closingSuccessDismissed by rememberSaveable { mutableStateOf(false) }
    var showExportDialog by rememberSaveable { mutableStateOf(false) }

    if (showExportDialog && selected != null) {
        ReportExportDialog(
            summary = selected,
            onDismiss = { showExportDialog = false }
        )
    }

    if (saveStatus == SaveStatus.SUCCESS && !closingSuccessDismissed) {
        AlertDialog(
            onDismissRequest = { closingSuccessDismissed = true },
            title = { Text("Fechamento Concluído! 🍃") },
            text = { Text("O fechamento de hoje foi salvo com sucesso. Deseja gerar e compartilhar o relatório agora?") },
            dismissButton = {
                TextButton(onClick = { closingSuccessDismissed = true }) { Text("Voltar ao Início") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        closingSuccessDismissed = true
                        showExportDialog = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Assessment,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gerar Relatório", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (pendingSelectionId != null) {
        AlertDialog(
            onDismissRequest = { pendingSelectionId = null },
            title = { Text(stringResource(R.string.closing_discard_title)) },
            text = { Text(stringResource(R.string.closing_discard_message)) },
            confirmButton = {
                TextButton(
                    enabled = inputsEnabled,
                    onClick = { selectedId = pendingSelectionId; pendingSelectionId = null }
                ) { Text(stringResource(R.string.closing_discard_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingSelectionId = null }) {
                    Text(stringResource(R.string.closing_discard_cancel))
                }
            }
        )
    }

    LaunchedEffect(selected?.day?.id) {
        selected?.let { summary ->
            selectedId = summary.day.id
            if (loadedSelectionId == summary.day.id) return@let
            loadedSelectionId = summary.day.id
            clientes = if (summary.day.clientesAtendidos > 0) summary.day.clientesAtendidos.toString() else ""
            leftoversMap.clear()
            ranOutMap.clear()
            ranOutTimeMap.clear()
            clientesErro = null
            leftoverErrors.clear()
            timeErrors.clear()
            summary.items.forEach { display ->
                leftoversMap[display.item.id] = if (display.item.quantidadeSobra > 0) {
                    formatInput(display.item.quantidadeSobra)
                } else {
                    "0"
                }
                ranOutMap[display.item.id] = display.item.acabouAntesDoFim
                ranOutTimeMap[display.item.id] = display.item.horarioAcabou.orEmpty()
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
                    onClick = {
                        if (summary.day.id != selected?.day?.id) {
                            if (hasChanges) pendingSelectionId = summary.day.id
                            else selectedId = summary.day.id
                        }
                    }
                )
            }
            if (selected?.fechado == true) item {
                Text(
                    stringResource(R.string.closing_read_only),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                OutlinedTextField(
                    value = clientes,
                    enabled = canEdit,
                    onValueChange = {
                        if (isIntegerInput(it)) {
                            clientes = it
                            clientesErro = null
                        }
                    },
                    label = { Text(stringResource(R.string.label_customers_served)) },
                    placeholder = { Text("Ex.: 120 clientes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = clientesErro != null,
                    supportingText = clientesErro?.let { message -> { Text(message) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Atalho "Marcar todos com Sobra Zero"
            if (canEdit && selected != null && selected.items.isNotEmpty()) {
                item {
                    OutlinedButton(
                        onClick = {
                            selected.items.forEach { display ->
                                leftoversMap[display.item.id] = "0"
                                leftoverErrors.remove(display.item.id)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("⚡ Marcar todos os pratos com Sobra Zero (0)")
                    }
                }
            }

            selected?.items?.forEach { display ->
                item(key = "closing-item-${display.item.id}") {
                    ClosingItemCard(
                        display = display,
                        leftover = leftoversMap[display.item.id].orEmpty(),
                        ranOut = ranOutMap[display.item.id] ?: false,
                        time = ranOutTimeMap[display.item.id].orEmpty(),
                        leftoverError = leftoverErrors[display.item.id],
                        timeError = timeErrors[display.item.id],
                        enabled = canEdit,
                        onLeftoverChange = {
                            leftoversMap[display.item.id] = it
                            leftoverErrors.remove(display.item.id)
                        },
                        onRanOutChange = { isChecked ->
                            ranOutMap[display.item.id] = isChecked
                            if (!isChecked) {
                                ranOutTimeMap[display.item.id] = ""
                                timeErrors.remove(display.item.id)
                            }
                        },
                        onTimeChange = {
                            ranOutTimeMap[display.item.id] = it
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
                                val didRunOut = ranOutMap[itemId] ?: false
                                val leftover = parseDoubleOrNull(leftoversMap[itemId].orEmpty())

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
                                    validator.validarHorario(didRunOut, ranOutTimeMap[itemId])
                                }.exceptionOrNull()?.message?.let { message ->
                                    novosErrosDeHorario[itemId] = message
                                }

                                if (leftover == null) {
                                    null
                                } else {
                                    display.item.copy(
                                        quantidadeSobra = leftover,
                                        acabouAntesDoFim = didRunOut,
                                        horarioAcabou = ranOutTimeMap[itemId]
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
                    enabled = canEdit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (saveStatus == SaveStatus.SAVING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
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
    var showTimePicker by remember { mutableStateOf(false) }

    if (showTimePicker) {
        ClockTimePickerModal(
            initialTime = time.ifBlank { "13:30" },
            onTimeSelected = { selectedTime ->
                onTimeChange(selectedTime)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(display.food.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = display.food.unidadeMedida,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

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

            val isGram = display.food.unidadeMedida.equals("g", ignoreCase = true)
            OutlinedTextField(
                value = leftover,
                enabled = enabled,
                onValueChange = { if (isDecimalInput(it)) onLeftoverChange(it) },
                label = { Text(stringResource(R.string.label_leftover, display.food.unidadeMedida)) },
                placeholder = { Text(if (isGram) "Ex.: 250" else stringResource(R.string.hint_quantity)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isGram) KeyboardType.Number else KeyboardType.Decimal
                ),
                singleLine = true,
                isError = leftoverError != null,
                supportingText = leftoverError?.let { message -> { Text(message) } },
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = ranOut,
                    onCheckedChange = { isChecked ->
                        onRanOutChange(isChecked)
                        if (isChecked && time.isBlank()) {
                            showTimePicker = true
                        }
                    },
                    enabled = enabled
                )
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
                    trailingIcon = {
                        if (enabled) {
                            IconButton(onClick = { showTimePicker = true }) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = "Selecionar no relógio",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClockTimePickerModal(
    initialTime: String,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialHour = initialTime.split(":").getOrNull(0)?.toIntOrNull() ?: 13
    val initialMinute = initialTime.split(":").getOrNull(1)?.toIntOrNull() ?: 30
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Selecione o horário em que acabou") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            Button(onClick = {
                val formattedHour = timePickerState.hour.toString().padStart(2, '0')
                val formattedMinute = timePickerState.minute.toString().padStart(2, '0')
                onTimeSelected("$formattedHour:$formattedMinute")
            }) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
