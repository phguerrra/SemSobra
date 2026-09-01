package com.example.semsobra.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppRoute(
    val title: String,
    val shortTitle: String,
    val icon: ImageVector
) {
    Home("Inicio", "Inicio", Icons.Filled.Home),
    Foods("Preparo do Buffet", "Preparo", Icons.Filled.Fastfood),
    Production("Producao do Dia", "Producao", Icons.Filled.Restaurant),
    Closing("Fechamento", "Fechar", Icons.Filled.CheckCircle),
    Analysis("Analise", "Analise", Icons.Filled.Assessment)
}
