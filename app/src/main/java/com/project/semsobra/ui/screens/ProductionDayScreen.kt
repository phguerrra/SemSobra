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
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.project.semsobra.R
import com.project.semsobra.ui.components.EmptyState
import com.project.semsobra.ui.components.HeaderCard
import com.project.semsobra.domain.usecase.ValidarDadosProducaoUseCase
import com.project.semsobra.ui.SaveStatus
import com.project.semsobra.ui.model.FoodUiModel
import com.project.semsobra.ui.util.formatDate
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
    modifier: Modifier = Modifier
) {
    var quantities by rememberSaveable { mutableStateOf<Map<Long, String>>(emptyMap()) }
    val errors = remember { mutableStateMapOf<Long, String>() }
    val validator = remember { ValidarDadosProducaoUseCase() }
    val today = LocalDate.now()
    val todayName = stringArrayResource(R.array.weekdays)[today.dayOfWeek.value - 1]
    val invalidNumberMessage = stringResource(R.string.error_invalid_number)

    if (saveStatus == SaveStatus.SUCCESS) {
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
        if (foods.isEmpty()) {
            item { EmptyState(stringResource(R.string.production_empty_menu, todayName)) }
        } else {
            items(foods, key = { it.id }) { food ->
                ProductionInputCard(
                    food = food,
                    value = quantities[food.id].orEmpty(),
                    error = errors[food.id],
                    onValueChange = {
                        quantities = quantities + (food.id to it)
                        errors.remove(food.id)
                    }
                )
            }
            item {
                Button(
                    onClick = {
                        val parsedQuantities = mutableMapOf<Long, Double>()
                        val currentErrors = mutableMapOf<Long, String>()
                        quantities.filterValues(String::isNotBlank).forEach { (foodId, value) ->
                            val quantity = parseDoubleOrNull(value)
                            if (quantity == null) {
                                currentErrors[foodId] = invalidNumberMessage
                            } else {
                                val error = runCatching {
                                    validator.validarQuantidadeProduzida(quantity)
                                }.exceptionOrNull()?.message
                                if (error == null) {
                                    parsedQuantities[foodId] = quantity
                                } else {
                                    currentErrors[foodId] = error
                                }
                            }
                        }
                        errors.clear()
                        errors.putAll(currentErrors)
                        if (currentErrors.isEmpty()) onSave(parsedQuantities)
                    },
                    enabled = quantities.values.any(String::isNotBlank) &&
                        saveStatus != SaveStatus.SAVING,
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
                        Text(stringResource(R.string.action_save_production))
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
