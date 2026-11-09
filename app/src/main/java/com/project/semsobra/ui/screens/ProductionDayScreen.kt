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
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.semsobra.R
import com.project.semsobra.ui.components.EmptyState
import com.project.semsobra.ui.components.HeaderCard
import com.project.semsobra.domain.model.ProductionSummary
import com.project.semsobra.ui.SaveStatus
import com.project.semsobra.ui.model.FoodUiModel
import com.project.semsobra.ui.model.atualizarRascunhoProducao
import com.project.semsobra.ui.model.prepararItensProducao
import com.project.semsobra.ui.model.validarQuantidadesProducao
import com.project.semsobra.ui.util.formatDate
import com.project.semsobra.ui.util.formatInput
import com.project.semsobra.ui.util.isDecimalInput
import com.project.semsobra.ui.util.parseDoubleOrNull
import java.time.LocalDate

@Composable
fun ProductionDayScreen(
    foods: List<FoodUiModel>,
    saveStatus: SaveStatus,
    onSave: (Map<Long, Double>) -> Unit,
    onGoHome: () -> Unit,
    onGoToClosing: () -> Unit,
    modifier: Modifier = Modifier,
    production: ProductionSummary? = null,
    onSaveResultConsumed: () -> Unit = {}
) {
    val date = production?.day?.data?.let(LocalDate::parse) ?: LocalDate.now()
    val savedQuantities = remember(production) {
        production?.items.orEmpty().associate { it.food.id to formatInput(it.item.quantidadeProduzida) }
    }
    val productionItems = remember(foods, production, date) {
        prepararItensProducao(foods, production, date)
    }
    var quantities by rememberSaveable(date.toString()) { mutableStateOf(savedQuantities) }
    var previousSaved by rememberSaveable(date.toString()) { mutableStateOf(savedQuantities) }
    var updatingSubmission by rememberSaveable(date.toString()) { mutableStateOf(false) }
    val errors = remember { mutableStateMapOf<Long, String>() }
    val closed = production?.fechado == true
    val dayName = stringArrayResource(R.array.weekdays)[date.dayOfWeek.value - 1]
    val invalidNumberMessage = stringResource(R.string.error_invalid_number)

    LaunchedEffect(savedQuantities, closed, saveStatus) {
        quantities = if (closed || saveStatus == SaveStatus.SUCCESS) savedQuantities
        else atualizarRascunhoProducao(quantities, previousSaved, savedQuantities)
        previousSaved = savedQuantities
        if (saveStatus == SaveStatus.SUCCESS && (updatingSubmission || closed)) onSaveResultConsumed()
    }

    if (saveStatus == SaveStatus.SUCCESS && !updatingSubmission && !closed) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.production_saved_title)) },
            text = { Text(stringResource(R.string.production_saved_message)) },
            dismissButton = {
                TextButton(onClick = onGoHome) { Text(stringResource(R.string.action_go_home)) }
            },
            confirmButton = {
                Button(onClick = onGoToClosing) { Text(stringResource(R.string.action_go_closing)) }
            }
        )
    }

    val showSaveAction = !closed && productionItems.any { it.podeEditar } &&
        saveStatus != SaveStatus.SUCCESS
    val canSave = showSaveAction && saveStatus != SaveStatus.SAVING &&
        (production != null || productionItems.any { quantities[it.food.id].orEmpty().isNotBlank() })
    val onSaveAction: () -> Unit = {
        if (canSave) {
            val result = validarQuantidadesProducao(productionItems, quantities, invalidNumberMessage)
            errors.clear()
            errors.putAll(result.errors)
            if (result.errors.isEmpty()) {
                updatingSubmission = production != null
                onSave(result.quantities)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            if (showSaveAction) {
                ExtendedFloatingActionButton(
                    onClick = onSaveAction,
                    modifier = Modifier.semantics { if (!canSave) disabled() },
                    icon = {
                        if (saveStatus == SaveStatus.SAVING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.Save, contentDescription = null)
                        }
                    },
                    text = {
                        Text(
                            stringResource(when {
                                saveStatus == SaveStatus.SAVING -> R.string.status_saving
                                production != null -> R.string.action_update_production
                                else -> R.string.action_save_production
                            }),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    containerColor = if (canSave || saveStatus == SaveStatus.SAVING) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    },
                    contentColor = if (canSave || saveStatus == SaveStatus.SAVING) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 16.dp,
                bottom = if (showSaveAction) 96.dp else 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HeaderCard(
                    title = stringResource(when {
                        closed -> R.string.production_closed_title
                        production != null -> R.string.production_open_title
                        else -> R.string.production_today_title
                    }),
                    value = formatDate(date.toString()),
                    subtitle = when {
                        closed -> stringResource(R.string.production_closed_description)
                        production != null -> stringResource(R.string.production_open_description)
                        else -> stringResource(R.string.production_today_description, dayName)
                    }
                )
            }
            if (saveStatus == SaveStatus.ERROR && !closed) {
                item {
                    Text(stringResource(R.string.production_save_failed), color = MaterialTheme.colorScheme.error)
                }
            }
            if (productionItems.isEmpty()) {
                item { EmptyState(stringResource(R.string.production_empty_menu, dayName)) }
            } else {
                items(productionItems, key = { it.food.id }) { item ->
                    val food = item.food
                    ProductionInputCard(
                        food = food,
                        value = if (item.podeEditar) quantities[food.id].orEmpty()
                        else item.quantidadeSalva?.let(::formatInput).orEmpty(),
                        error = if (item.podeEditar) errors[food.id] else null,
                        enabled = item.podeEditar && saveStatus != SaveStatus.SAVING,
                        inactive = !closed && !item.podeEditar,
                        onValueChange = {
                            quantities = quantities + (food.id to it)
                            errors.remove(food.id)
                        }
                    )
                }
            }
            if (closed) item {
                Button(onClick = onGoToClosing, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_view_closing))
                }
            }
        }
    }
}

@Composable
private fun ProductionInputCard(
    food: FoodUiModel,
    value: String,
    error: String?,
    enabled: Boolean,
    inactive: Boolean,
    onValueChange: (String) -> Unit
) {
    fun addQuantity(delta: Double) {
        val current = parseDoubleOrNull(value) ?: 0.0
        val next = (current + delta).coerceAtLeast(0.0)
        onValueChange(formatInput(next))
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(food.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (food.descricao.isNotBlank()) {
                Text(
                    food.descricao,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (inactive) {
                Text(
                    stringResource(R.string.production_inactive_item),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            OutlinedTextField(
                value = value,
                enabled = enabled,
                onValueChange = { if (isDecimalInput(it)) onValueChange(it) },
                label = { Text(stringResource(R.string.label_quantity_produced, food.unidadeMedida)) },
                placeholder = { Text(stringResource(R.string.hint_quantity)) },
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { message -> { Text(message) } },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.5 to "+0,5", 1.0 to "+1,0", 5.0 to "+5,0").forEach { (amount, label) ->
                    FilterChip(
                        selected = false,
                        enabled = enabled,
                        onClick = { addQuantity(amount) },
                        label = { Text(label, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }
    }
}
