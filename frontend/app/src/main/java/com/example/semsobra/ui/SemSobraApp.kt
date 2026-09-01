package com.example.semsobra.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.semsobra.ui.navigation.AppRoute
import com.example.semsobra.ui.screens.AnalysisScreen
import com.example.semsobra.ui.screens.ClosingScreen
import com.example.semsobra.ui.screens.FoodScreen
import com.example.semsobra.ui.screens.HomeScreen
import com.example.semsobra.ui.screens.ProductionDayScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemSobraApp(viewModel: SemSobraViewModel = viewModel()) {
    var currentRoute by rememberSaveable { androidx.compose.runtime.mutableStateOf(AppRoute.Home) }
    val foods by viewModel.foods.collectAsStateWithLifecycle()
    val summaries by viewModel.productionSummaries.collectAsStateWithLifecycle()
    val analytics by viewModel.analytics.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SemSobra", style = MaterialTheme.typography.titleLarge)
                        Text(currentRoute.title, style = MaterialTheme.typography.labelMedium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            SemSobraBottomBar(
                currentRoute = currentRoute,
                onRouteSelected = { currentRoute = it }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when (currentRoute) {
            AppRoute.Home -> HomeScreen(
                analytics = analytics,
                foods = foods,
                summaries = summaries,
                onRegisterProduction = { currentRoute = AppRoute.Production },
                modifier = Modifier.padding(padding)
            )
            AppRoute.Foods -> FoodScreen(
                foods = foods,
                onSave = viewModel::saveFood,
                onDelete = viewModel::deleteFood,
                modifier = Modifier.padding(padding)
            )
            AppRoute.Production -> ProductionDayScreen(
                foods = foods,
                onSave = viewModel::saveProductionToday,
                modifier = Modifier.padding(padding)
            )
            AppRoute.Closing -> ClosingScreen(
                summaries = summaries,
                onClose = viewModel::closeProduction,
                modifier = Modifier.padding(padding)
            )
            AppRoute.Analysis -> AnalysisScreen(
                analytics = analytics,
                summaries = summaries,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun SemSobraBottomBar(
    currentRoute: AppRoute,
    onRouteSelected: (AppRoute) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp,
        tonalElevation = 4.dp
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            AppRoute.entries.forEach { route ->
                NavigationBarItem(
                    selected = route == currentRoute,
                    onClick = { onRouteSelected(route) },
                    icon = {
                        Icon(
                            imageVector = route.icon,
                            contentDescription = route.title,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(route.shortTitle, style = MaterialTheme.typography.labelSmall)
                    }
                )
            }
        }
    }
}
