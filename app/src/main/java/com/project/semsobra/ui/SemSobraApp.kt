package com.project.semsobra.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.semsobra.ui.navigation.AppRoute
import com.project.semsobra.ui.model.disponivelNoDia
import com.project.semsobra.ui.screens.AnalysisScreen
import com.project.semsobra.ui.screens.ClosingScreen
import com.project.semsobra.ui.screens.FoodScreen
import com.project.semsobra.ui.screens.HomeScreen
import com.project.semsobra.ui.screens.ProductionDayScreen
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemSobraApp(viewModel: SemSobraViewModel = viewModel()) {
    var currentRoute by rememberSaveable { androidx.compose.runtime.mutableStateOf(AppRoute.Home) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val today = LocalDate.now()
    val foodsToday = uiState.foods.filter { it.disponivelNoDia(today.dayOfWeek.value) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentRoute) {
                AppRoute.Home -> HomeScreen(
                    analytics = uiState.analytics,
                    foods = foodsToday,
                    summaries = uiState.productionSummaries,
                    onRegisterProduction = { currentRoute = AppRoute.Production },
                    modifier = Modifier.padding(padding)
                )
                AppRoute.Foods -> FoodScreen(
                    foods = uiState.foods,
                    onSave = viewModel::saveFood,
                    onDelete = viewModel::deleteFood,
                    modifier = Modifier.padding(padding)
                )
                AppRoute.Production -> ProductionDayScreen(
                    foods = foodsToday,
                    onSave = viewModel::saveProductionToday,
                    modifier = Modifier.padding(padding)
                )
                AppRoute.Closing -> ClosingScreen(
                    summaries = uiState.productionSummaries,
                    onClose = viewModel::closeProduction,
                    modifier = Modifier.padding(padding)
                )
                AppRoute.Analysis -> AnalysisScreen(
                    analytics = uiState.analytics,
                    previsaoDemanda = uiState.previsaoDemanda,
                    summaries = uiState.productionSummaries,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun SemSobraBottomBar(currentRoute: AppRoute, onRouteSelected: (AppRoute) -> Unit) {
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
