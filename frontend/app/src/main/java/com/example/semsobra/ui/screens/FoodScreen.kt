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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.semsobra.ui.components.EmptyState
import com.example.semsobra.ui.components.FormCard
import com.example.semsobra.ui.components.SectionTitle
import com.example.semsobra.ui.model.FoodUiModel

@Composable
fun FoodScreen(
    foods: List<FoodUiModel>,
    onSave: (Long, String, String, String) -> Unit,
    onDelete: (FoodUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingId by rememberSaveable { androidx.compose.runtime.mutableStateOf(0L) }
    var nome by rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    var descricao by rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    var unidade by rememberSaveable { androidx.compose.runtime.mutableStateOf("kg") }

    fun clearForm() {
        editingId = 0
        nome = ""
        descricao = ""
        unidade = "kg"
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FormCard {
                Text(
                    "Cadastro de preparos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Registre cada item servido no buffet por kg e descreva o que entra no preparo.",
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
                    label = { Text("Descricao / composicao") },
                    placeholder = { Text("Ex.: batata, cenoura, ervilha, ovos, maionese e temperos") },
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
                            onSave(editingId, nome, descricao, unidade)
                            clearForm()
                        },
                        enabled = nome.isNotBlank()
                    ) {
                        Text(if (editingId == 0L) "Cadastrar" else "Salvar")
                    }
                    if (editingId != 0L) {
                        OutlinedButton(onClick = ::clearForm) {
                            Text("Cancelar")
                        }
                    }
                }
            }
        }
        item { SectionTitle("Preparos cadastrados") }
        if (foods.isEmpty()) {
            item { EmptyState("Nenhum preparo cadastrado.") }
        } else {
            items(foods, key = { it.id }) { food ->
                FoodCard(
                    food = food,
                    onEdit = {
                        editingId = food.id
                        nome = food.nome
                        descricao = food.descricao
                        unidade = food.unidadeMedida
                    },
                    onDelete = { onDelete(food) }
                )
            }
        }
    }
}

@Composable
private fun FoodCard(
    food: FoodUiModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(food.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                food.descricao.ifBlank { "Sem descricao cadastrada." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("Controle em ${food.unidadeMedida}", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) {
                    Text("Editar")
                }
                TextButton(onClick = onDelete) {
                    Text("Excluir")
                }
            }
        }
    }
}
