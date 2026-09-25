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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.project.semsobra.ui.components.EmptyState
import com.project.semsobra.ui.components.HeaderCard
import com.project.semsobra.domain.usecase.ValidarDadosProducaoUseCase
import com.project.semsobra.ui.SaveStatus
import com.project.semsobra.ui.model.FoodUiModel
import com.project.semsobra.ui.util.formatDate
import com.project.semsobra.ui.util.dayName
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
    val quantities = remember { mutableStateMapOf<Long, String>() }
    val errors = remember { mutableStateMapOf<Long, String>() }
    val validator = remember { ValidarDadosProducaoUseCase() }
    val today = LocalDate.now()

    if (saveStatus == SaveStatus.SUCCESS) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Produção salva") },
            text = { Text("A produção de hoje foi registrada. Qual é a próxima etapa?") },
            dismissButton = {
                TextButton(onClick = onGoHome) { Text("Ir para início") }
            },
            confirmButton = {
                Button(onClick = onGoToClosing) { Text("Ir para fechamento") }
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
                title = "Produção de hoje",
                value = formatDate(today.toString()),
                subtitle = "Cardápio de ${dayName(today.dayOfWeek.value)}. Informe a quantidade preparada de cada item."
            )
        }
        if (foods.isEmpty()) {
            item { EmptyState("Nenhum preparo foi cadastrado para ${dayName(today.dayOfWeek.value)}.") }
        } else {
            items(foods, key = { it.id }) { food ->
                ProductionInputCard(
                    food = food,
                    value = quantities[food.id].orEmpty(),
                    error = errors[food.id],
                    onValueChange = {
                        quantities[food.id] = it
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
                                currentErrors[foodId] = "Informe um número válido"
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
                        Text("Salvando...")
                    } else {
                        Text("Salvar produção")
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
                onValueChange = onValueChange,
                label = { Text("Quantidade produzida (${food.unidadeMedida})") },
                placeholder = { Text("Ex.: 8,5") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { message -> { Text(message) } },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
