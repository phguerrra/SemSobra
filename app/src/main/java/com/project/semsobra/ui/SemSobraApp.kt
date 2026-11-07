package com.project.semsobra.ui

import android.content.Context
import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
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
import com.project.semsobra.data.backup.BackupFormatException
import com.project.semsobra.data.backup.BackupJsonCodec
import com.project.semsobra.data.backup.BackupPreview
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemSobraApp() {
    val application = LocalContext.current.applicationContext as Application
    val factory = remember(application) { SemSobraViewModelFactory(application) }
    val viewModel: SemSobraViewModel = viewModel(factory = factory)
    SemSobraApp(viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemSobraApp(viewModel: SemSobraViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    var backupBusy by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<PendingRestore?>(null) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = AppRoute.fromRoute(backStackEntry?.destination?.route)
    val today = LocalDate.now()
    val foodsToday = remember(uiState.foods, today) {
        uiState.foods.filter { it.disponivelNoDia(today.dayOfWeek.value) }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message.text)
            }
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                backupBusy = true
                try {
                    val json = withContext(Dispatchers.IO) { viewModel.criarBackup() }
                    withContext(Dispatchers.IO) { writeBackup(context, uri, json) }
                    snackbarHostState.showSnackbar("Backup exportado com sucesso")
                } catch (error: Exception) {
                    snackbarHostState.showSnackbar(
                        error.message ?: "Não foi possível exportar o backup"
                    )
                } finally {
                    backupBusy = false
                }
            }
        }
    }
    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                backupBusy = true
                try {
                    val json = withContext(Dispatchers.IO) { readBackup(context, uri) }
                    val decoded = withContext(Dispatchers.IO) { viewModel.inspecionarBackup(json) }
                    pendingRestore = PendingRestore(json, decoded.preview)
                } catch (error: Exception) {
                    val message = if (error is BackupFormatException) {
                        error.message ?: "O arquivo de backup não é válido"
                    } else {
                        "Não foi possível abrir esse arquivo de backup"
                    }
                    snackbarHostState.showSnackbar(message)
                } finally {
                    backupBusy = false
                }
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
                        if (route != currentRoute) {
                            navController.navigate(route.route) {
                                popUpTo(AppRoute.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
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
                    viewModel = viewModel,
                    backupEnabled = !backupBusy && !uiState.isSaving,
                    backupBusy = backupBusy,
                    onExportBackup = {
                        exportBackupLauncher.launch("SemSobra-backup-${LocalDate.now()}.json")
                    },
                    onImportBackup = {
                        importBackupLauncher.launch(arrayOf("application/json", "text/json"))
                    }
                )
            }
        }
    }

    pendingRestore?.let { pending ->
        RestoreBackupDialog(
            preview = pending.preview,
            enabled = !backupBusy,
            onDismiss = { pendingRestore = null },
            onConfirm = {
                pendingRestore = null
                scope.launch {
                    backupBusy = true
                    try {
                        withContext(Dispatchers.IO) { viewModel.restaurarBackup(pending.json) }
                        AppRoute.entries.forEach { navController.clearBackStack(it.route) }
                        navController.navigate(AppRoute.Home.route) {
                            popUpTo(AppRoute.Home.route) { inclusive = true }
                            launchSingleTop = true
                        }
                        snackbarHostState.showSnackbar("Backup restaurado com sucesso")
                    } catch (error: Exception) {
                        val message = if (error is BackupFormatException) {
                            error.message ?: "O arquivo de backup não é válido"
                        } else {
                            "Não foi possível concluir a restauração do backup"
                        }
                        snackbarHostState.showSnackbar(message)
                    } finally {
                        backupBusy = false
                    }
                }
            }
        )
    }
}

@Composable
private fun AppNavigation(
    navController: NavHostController,
    uiState: SemSobraUiState,
    foodsToday: List<com.project.semsobra.ui.model.FoodUiModel>,
    viewModel: SemSobraViewModel,
    backupEnabled: Boolean,
    backupBusy: Boolean,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = AppRoute.Home.route,
        enterTransition = { smoothEnterTransition() },
        exitTransition = { smoothExitTransition() },
        popEnterTransition = { smoothEnterTransition(fromEnd = false) },
        popExitTransition = { smoothExitTransition(toStart = false) }
    ) {
        composable(AppRoute.Home.route) {
            HomeScreen(
                analytics = uiState.analytics,
                forecastDate = uiState.previsaoDemanda.dataPrevisao,
                foods = foodsToday,
                summaries = uiState.productionSummaries,
                onOpenProduction = { navController.navigate(AppRoute.Production.route) },
                onOpenClosing = { navController.navigate(AppRoute.Closing.route) },
                onOpenAnalysis = { navController.navigate(AppRoute.Analysis.route) },
                onExportBackup = onExportBackup,
                onImportBackup = onImportBackup,
                backupEnabled = backupEnabled,
                backupBusy = backupBusy
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
                onDeleteResultConsumed = viewModel::consumeFoodDeleteResult,
                onToggleDay = viewModel::toggleFoodDay
            )
        }
        composable(AppRoute.Production.route) {
            val todaySummary = uiState.productionSummaries.firstOrNull {
                it.day.data == LocalDate.now().toString()
            }
            if (todaySummary != null && uiState.productionSaveStatus != SaveStatus.SUCCESS) {
                LaunchedEffect(todaySummary.day.id, todaySummary.fechado) {
                    navController.navigate(
                        if (todaySummary.fechado) AppRoute.Analysis.route else AppRoute.Closing.route
                    ) {
                        popUpTo(AppRoute.Production.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            } else {
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
                summaries = uiState.productionSummaries,
                saveStatus = uiState.closingSaveStatus,
                onSaveHistory = viewModel::closeProduction,
                onSaveHistoryResultConsumed = viewModel::consumeClosingSaveResult
            )
        }
    }
}

private data class PendingRestore(val json: String, val preview: BackupPreview)

@Composable
private fun RestoreBackupDialog(
    preview: BackupPreview,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm").withZone(ZoneId.systemDefault())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restaurar backup?") },
        text = {
            Column {
                Text("Backup de ${dateFormatter.format(preview.exportedAt)}.")
                Spacer(Modifier.size(8.dp))
                Text("${preview.preparationCount} preparos, ${preview.productionCount} produções e ${preview.productionItemCount} itens.")
                Spacer(Modifier.size(8.dp))
                Text("A restauração substituirá todos os dados atuais do SemSobra. Essa ação não pode ser desfeita.")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = enabled) { Text("Restaurar dados") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = enabled) { Text("Cancelar") }
        }
    )
}

private fun readBackup(context: Context, uri: Uri): String {
    val input = context.contentResolver.openInputStream(uri)
        ?: throw IOException("Não foi possível abrir o arquivo selecionado")
    return input.use { it.readUtf8Limited(BackupJsonCodec.MAX_BACKUP_BYTES) }
}

private fun writeBackup(context: Context, uri: Uri, json: String) {
    val output = context.contentResolver.openOutputStream(uri, "wt")
        ?: throw IOException("Não foi possível salvar o arquivo de backup")
    output.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
}

private fun InputStream.readUtf8Limited(maxBytes: Long): String {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var totalBytes = 0L
    while (true) {
        val bytesRead = read(buffer)
        if (bytesRead < 0) break
        totalBytes += bytesRead
        if (totalBytes > maxBytes) throw BackupFormatException("O arquivo de backup é grande demais")
        output.write(buffer, 0, bytesRead)
    }
    return output.toString(Charsets.UTF_8.name())
}

private fun smoothEnterTransition(fromEnd: Boolean = true): EnterTransition =
    fadeIn(animationSpec = tween(durationMillis = 140)) +
        slideInHorizontally(
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
            initialOffsetX = { width -> if (fromEnd) width / 16 else -width / 16 }
        )

private fun smoothExitTransition(toStart: Boolean = true): ExitTransition =
    fadeOut(animationSpec = tween(durationMillis = 110)) +
        slideOutHorizontally(
            animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
            targetOffsetX = { width -> if (toStart) -width / 20 else width / 20 }
        )

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
    val bottomBarRoutes = remember { AppRoute.entries.filter { it != AppRoute.Analysis } }
    val activeRoute = if (currentRoute == AppRoute.Analysis) AppRoute.Production else currentRoute

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp,
        tonalElevation = 0.dp
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            bottomBarRoutes.forEach { route ->
                NavigationBarItem(
                    selected = route == activeRoute,
                    onClick = {
                        if (route != activeRoute) {
                            onRouteSelected(route)
                        }
                    },
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
