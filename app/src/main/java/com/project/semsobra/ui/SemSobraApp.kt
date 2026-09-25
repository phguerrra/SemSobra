package com.project.semsobra.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.project.semsobra.ui.navigation.AppRoute
import com.project.semsobra.ui.model.disponivelNoDia
import com.project.semsobra.ui.model.UiEvent
import com.project.semsobra.ui.screens.AnalysisScreen
import com.project.semsobra.ui.screens.ClosingScreen
import com.project.semsobra.ui.screens.FoodScreen
import com.project.semsobra.ui.screens.HomeScreen
import com.project.semsobra.ui.screens.ProductionDayScreen
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemSobraApp(viewModel: SemSobraViewModel = viewModel()) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = AppRoute.fromRoute(backStackEntry?.destination?.route)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val today = LocalDate.now()
    val foodsToday = uiState.foods.filter { it.disponivelNoDia(today.dayOfWeek.value) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message.text)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Eco,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("SemSobra", style = MaterialTheme.typography.titleLarge)
                            Text(
                                currentRoute.title,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            if (!uiState.isLoading && uiState.loadError == null) {
                SemSobraBottomBar(
                    currentRoute = currentRoute,
                    onRouteSelected = { route ->
                        navController.navigate(route.route) {
                            popUpTo(AppRoute.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> InitialLoadingState()
                uiState.loadError != null -> InitialErrorState(
                    message = uiState.loadError.orEmpty(),
                    onRetry = viewModel::retryInitialLoad
                )
                else -> AppNavigation(
                    navController = navController,
                    uiState = uiState,
                    foodsToday = foodsToday,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun AppNavigation(
    navController: NavHostController,
    uiState: SemSobraUiState,
    foodsToday: List<com.project.semsobra.ui.model.FoodUiModel>,
    viewModel: SemSobraViewModel
) {
    NavHost(navController = navController, startDestination = AppRoute.Home.route) {
        composable(AppRoute.Home.route) {
            HomeScreen(
                analytics = uiState.analytics,
                foods = foodsToday,
                summaries = uiState.productionSummaries,
                onOpenProduction = { navController.navigate(AppRoute.Production.route) },
                onOpenClosing = { navController.navigate(AppRoute.Closing.route) },
                onOpenAnalysis = { navController.navigate(AppRoute.Analysis.route) }
            )
        }
        composable(AppRoute.Foods.route) {
            FoodScreen(
                foods = uiState.foods,
                saveStatus = uiState.foodSaveStatus,
                deleteStatus = uiState.foodDeleteStatus,
                onSave = viewModel::saveFood,
                onSaveResultConsumed = viewModel::consumeFoodSaveResult,
                onDelete = viewModel::deleteFood,
                onDeleteResultConsumed = viewModel::consumeFoodDeleteResult
            )
        }
        composable(AppRoute.Production.route) {
            ProductionDayScreen(
                foods = foodsToday,
                saveStatus = uiState.productionSaveStatus,
                onSave = viewModel::saveProductionToday,
                onGoHome = {
                    viewModel.consumeProductionSaveResult()
                    navController.navigate(AppRoute.Home.route) {
                        popUpTo(AppRoute.Home.route) { inclusive = true }
                    }
                },
                onGoToClosing = {
                    viewModel.consumeProductionSaveResult()
                    navController.navigate(AppRoute.Closing.route) {
                        popUpTo(AppRoute.Home.route)
                    }
                }
            )
        }
        composable(AppRoute.Closing.route) {
            ClosingScreen(
                summaries = uiState.productionSummaries,
                saveStatus = uiState.closingSaveStatus,
                onClose = viewModel::closeProduction
            )
        }
        composable(AppRoute.Analysis.route) {
            AnalysisScreen(
                analytics = uiState.analytics,
                previsaoDemanda = uiState.previsaoDemanda,
                summaries = uiState.productionSummaries
            )
        }
    }
}

@Composable
private fun InitialLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.size(20.dp))
        Text("Carregando seus dados...", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(6.dp))
        Text(
            "Preparando cardápio, histórico e previsões.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InitialErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Não foi possível abrir o SemSobra", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.size(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(20.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text("Tentar novamente")
        }
    }
}

@Composable
private fun SemSobraBottomBar(currentRoute: AppRoute, onRouteSelected: (AppRoute) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp,
        tonalElevation = 0.dp
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
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
