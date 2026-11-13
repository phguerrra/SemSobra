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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.project.semsobra.R
import com.project.semsobra.domain.model.ProductionSummary
import com.project.semsobra.ui.SaveStatus
import com.project.semsobra.ui.components.EmptyState
import com.project.semsobra.ui.components.HeaderCard
import com.project.semsobra.ui.components.StatusBadgeCard
import com.project.semsobra.ui.model.FoodUiModel
import com.project.semsobra.ui.model.atualizarRascunhoProducao
import com.project.semsobra.ui.model.prepararItensProducao
import com.project.semsobra.ui.model.validarQuantidadesProducao
import com.project.semsobra.ui.util.formatDate
import com.project.semsobra.ui.util.formatQuantity
import com.project.semsobra.ui.util.isDecimalInput
import java.time.LocalDate

@Composable
fun ProductionDayScreen(
    foods: List<FoodUiModel>,
    saveStatus: SaveStatus,
    production: ProductionSummary? = null,
    onSave: (Map<Long, Double>) -> Unit,
    onSaveResultConsumed: () -> Unit = {},
    onGoHome: () -> Unit,
    onGoToClosing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val todayName = stringArrayResource(R.array.weekdays)[today.dayOfWeek.value - 1]
    val items = remember(foods, production) { prepararItensProducao(foods, production, today) }
    val isClosed = production?.fechado == true
    val savedQuantities = remember(items) {
        items.mapNotNull { item ->
            item.quantidadeSalva?.let { item.food.id to formatQuantity(it) }
        }.toMap()
    }
    var draftQuantities by rememberSaveable { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var previousSavedQuantities by remember { mutableStateOf(savedQuantities) }
    val errors = remember { mutableStateMapOf<Long, String>() }
    val invalidNumberMessage = stringResource(R.string.error_invalid_number)

    LaunchedEffect(savedQuantities) {
        draftQuantities = atualizarRascunhoProducao(draftQuantities, previousSavedQuantities, savedQuantities)
        previousSavedQuantities = savedQuantities
    }

    if (saveStatus == SaveStatus.SUCCESS) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.production_saved_title)) },
            text = { Text(stringResource(R.string.production_saved_message)) },
            dismissButton = {
                TextButton(onClick = {
                    onSaveResultConsumed()
                    onGoHome()
                }) { Text(stringResource(R.string.action_go_home)) }
            },
            confirmButton = {
                Button(onClick = {
                    onSaveResultConsumed()
                    onGoToClosing()
                }) { Text(stringResource(R.string.action_go_closing)) }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeaderCard(
                title = stringResource(R.string.production_today_title),
                value = formatDate(today.toString()),
                subtitle = stringResource(R.string.production_today_description, todayName)
            )
        }

        if (isClosed) {
            item {
                StatusBadgeCard(
                    title = stringResource(R.string.production_closed_title),
                    message = stringResource(R.string.production_closed_description),
                    icon = Icons.Filled.Lock,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (production != null) {
            item {
                StatusBadgeCard(
                    title = stringResource(R.string.production_open_title),
                    message = stringResource(R.string.production_open_description),
                    icon = Icons.Filled.CheckCircle,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        if (items.isEmpty()) {
            item { EmptyState(stringResource(R.string.production_empty_menu, todayName)) }
        } else {
            items(items, key = { it.food.id }) { itemDisplay ->
                val food = itemDisplay.food
                val displayValue = draftQuantities[food.id] ?: savedQuantities[food.id].orEmpty()
                ProductionInputCard(
                    food = food,
                    value = displayValue,
                    error = errors[food.id],
                    enabled = itemDisplay.podeEditar && saveStatus != SaveStatus.SAVING,
                    onValueChange = { newValue ->
                        draftQuantities = draftQuantities + (food.id to newValue)
                        errors.remove(food.id)
                    }
                )
            }

            item {
                if (isClosed) {
                    Button(
                        onClick = onGoToClosing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.action_view_closing))
                    }
                } else {
                    val activeDraft = items.filter { it.podeEditar }.associate { item ->
                        item.food.id to (draftQuantities[item.food.id] ?: savedQuantities[item.food.id].orEmpty())
                    }
                    Button(
                        onClick = {
                            val result = validarQuantidadesProducao(items, activeDraft, invalidNumberMessage)
                            errors.clear()
                            errors.putAll(result.errors)
                            if (result.errors.isEmpty() && result.quantities.isNotEmpty()) {
                                onSave(result.quantities)
                            }
                        },
                        enabled = activeDraft.values.any(String::isNotBlank) && saveStatus != SaveStatus.SAVING,
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
                            Text(
                                if (production == null) stringResource(R.string.action_save_production)
                                else stringResource(R.string.action_update_production)
                            )
                        }
                    }
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
    onValueChange: (String) -> Unit
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
            OutlinedTextField(
                value = value,
                enabled = enabled,
                onValueChange = { if (isDecimalInput(it)) onValueChange(it) },
                label = { Text(stringResource(R.string.label_quantity_produced, food.unidadeMedida)) },
                placeholder = { Text(stringResource(R.string.hint_quantity)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { message -> { Text(message) } },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
