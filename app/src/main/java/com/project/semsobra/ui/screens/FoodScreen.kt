package com.project.semsobra.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.project.semsobra.ui.components.EmptyState
import com.project.semsobra.ui.components.FormCard
import com.project.semsobra.ui.components.SectionTitle
import com.project.semsobra.ui.model.FoodUiModel
import com.project.semsobra.ui.SaveStatus
import com.project.semsobra.ui.model.disponivelNoDia
import com.project.semsobra.ui.util.dayName
import java.time.LocalDate

@Composable
fun FoodScreen(
    foods: List<FoodUiModel>,
    saveStatus: SaveStatus,
    deleteStatus: SaveStatus,
    onSave: (Long, String, String, String, Int) -> Unit,
    onSaveResultConsumed: () -> Unit,
    onDelete: (FoodUiModel) -> Unit,
    onDeleteResultConsumed: () -> Unit,
    onToggleDay: (FoodUiModel, Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingId by rememberSaveable { androidx.compose.runtime.mutableStateOf(0L) }
    var nome by rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    var descricao by rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    var unidade by rememberSaveable { androidx.compose.runtime.mutableStateOf("kg") }
    var selectedDay by rememberSaveable {
        androidx.compose.runtime.mutableIntStateOf(LocalDate.now().dayOfWeek.value)
    }
    var foodPendingDeletion by remember { mutableStateOf<FoodUiModel?>(null) }
    var section by rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }
    val foodRows = remember(foods) { foods.chunked(2) }

    fun clearForm() {
        editingId = 0
        nome = ""
        descricao = ""
        unidade = "kg"
    }

    LaunchedEffect(saveStatus) {
        if (saveStatus == SaveStatus.SUCCESS) {
            clearForm()
            onSaveResultConsumed()
        }
    }

    LaunchedEffect(deleteStatus) {
        if (deleteStatus == SaveStatus.SUCCESS) {
            foodPendingDeletion = null
            onDeleteResultConsumed()
        }
    }

    foodPendingDeletion?.let { food ->
        val deleting = deleteStatus == SaveStatus.SAVING
        AlertDialog(
            onDismissRequest = {
                if (!deleting) {
                    foodPendingDeletion = null
                    onDeleteResultConsumed()
                }
            },
            title = { Text("Excluir ${food.nome}?") },
            text = {
                Text(
                    "Se este preparo ainda não possui histórico, ele será excluído. " +
                        "Caso já tenha sido usado em produções, será apenas inativado para " +
                        "preservar os relatórios anteriores."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(food) },
                    enabled = !deleting
                ) {
                    if (deleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                        Text("Excluindo...")
                    } else {
                        Text("Excluir", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        foodPendingDeletion = null
                        onDeleteResultConsumed()
                    },
                    enabled = !deleting
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = section == 0,
                    onClick = { section = 0 },
                    label = { Text("Todos os itens") }
                )
                FilterChip(
                    selected = section == 1,
                    onClick = { section = 1; clearForm() },
                    label = { Text("Cardápio por dia") }
                )
            }
        }
        if (section == 1) item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Monte o cardápio do dia",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Escolha o dia e marque os itens que serão preparados.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((1..7).toList()) { day ->
                        FilterChip(
                            selected = selectedDay == day,
                            onClick = {
                                if (selectedDay != day) {
                                    selectedDay = day
                                    clearForm()
                                }
                            },
                            label = { Text(dayShortName(day)) }
                        )
                    }
                }
            }
        }
        if (section == 0) item {
            FormCard {
                Text(
                    if (editingId == 0L) "Cadastrar item" else "Editar item",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Cadastre uma vez. Depois escolha em quais dias ele entra no cardápio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome do preparo") },
                    placeholder = { Text("Ex.: Maionese da casa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descricao,
                    onValueChange = { descricao = it },
                    label = { Text("Ingredientes / composição") },
                    placeholder = { Text("Ex.: língua, molho, cebola, alho e temperos") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = unidade,
                    onValueChange = { unidade = it },
                    label = { Text("Unidade de controle") },
                    supportingText = { Text("Para buffet por kg, mantenha kg.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onSave(editingId, nome, descricao, unidade, FoodUiModel.TODOS_OS_DIAS)
                        },
                        enabled = nome.isNotBlank() && saveStatus != SaveStatus.SAVING
                    ) {
                        if (saveStatus == SaveStatus.SAVING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                            Text("Salvando...")
                        } else {
                            Text(if (editingId == 0L) "Cadastrar" else "Salvar")
                        }
                    }
                    if (editingId != 0L) {
                        OutlinedButton(
                            onClick = ::clearForm,
                            enabled = saveStatus != SaveStatus.SAVING
                        ) {
                            Text("Cancelar")
                        }
                    }
                }
            }
        }
        item {
            SectionTitle(
                if (section == 0) "Itens cadastrados"
                else "Itens de ${dayName(selectedDay)}"
            )
        }
        if (foods.isEmpty()) {
            item { EmptyState("Cadastre o primeiro item em Todos os itens.") }
        } else if (section == 0) {
            items(
                items = foodRows,
                key = { row -> row.joinToString("-") { it.id.toString() } }
            ) { rowFoods ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowFoods.forEach { food ->
                    FoodCard(
                        food = food,
                            modifier = Modifier.weight(1f),
                        onEdit = {
                            editingId = food.id
                            nome = food.nome
                            descricao = food.descricao
                            unidade = food.unidadeMedida
                        },
                        onDelete = { foodPendingDeletion = food }
                    )
                    }
                    if (rowFoods.size == 1) {
                        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    }
                }
            }
        } else {
            items(foods, key = { it.id }) { food ->
                    DayFoodSelector(
                        food = food,
                        selected = food.disponivelNoDia(selectedDay),
                        onSelectedChange = { onToggleDay(food, selectedDay, it) }
                    )
            }
        }
    }
}

@Composable
private fun FoodCard(
    food: FoodUiModel,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val ingredients = food.descricao
        .split(',', ';', '\n')
        .map(String::trim)
        .filter(String::isNotBlank)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(food.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (ingredients.isEmpty()) {
                Text(
                    "Sem ingredientes cadastrados.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "Ingredientes",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ingredients.forEach { ingredient ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = ingredient,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
            Text("Unidade de controle: ${food.unidadeMedida}", style = MaterialTheme.typography.labelLarge)
            val selectedDays = (1..7).filter(food::disponivelNoDia)
            Text(
                if (selectedDays.isEmpty()) "Ainda não está em nenhum cardápio"
                else selectedDays.joinToString(" • ", transform = ::dayShortName),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) { Text("Editar") }
                TextButton(onClick = onDelete) { Text("Excluir") }
            }
        }
    }
}

@Composable
private fun DayFoodSelector(
    food: FoodUiModel,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit
) {
    Card(
        onClick = { onSelectedChange(!selected) },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(food.nome, fontWeight = FontWeight.Bold)
                Text(
                    if (selected) "Incluído neste dia" else "Toque para incluir",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilterChip(
                selected = selected,
                onClick = { onSelectedChange(!selected) },
                label = { Text(if (selected) "Selecionado" else "Selecionar") }
            )
        }
    }
}

private fun dayShortName(day: Int): String = when (day) {
    1 -> "Seg"
    2 -> "Ter"
    3 -> "Qua"
    4 -> "Qui"
    5 -> "Sex"
    6 -> "Sáb"
    7 -> "Dom"
    else -> "-"
}
