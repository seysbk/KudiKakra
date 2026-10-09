package com.kudikakra.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kudikakra.data.local.entity.DailyBudgetEntity
import com.kudikakra.data.local.entity.TransactionEntity
import com.kudikakra.data.local.entity.UserPreferencesEntity
import com.kudikakra.domain.budget.BudgetEngine
import com.kudikakra.domain.budget.BudgetStatus
import com.kudikakra.domain.parser.ParseResult
import com.kudikakra.domain.parser.ParserRegistry
import com.kudikakra.domain.processor.TransactionFingerprintGenerator
import com.kudikakra.notification.InspectedNotification
import com.kudikakra.notification.NotificationEvent
import com.kudikakra.notification.NotificationInspectorStore
import com.kudikakra.notification.detection.DetectionResult
import com.kudikakra.notification.detection.FinancialNotificationDetector
import com.kudikakra.notification.detection.FinancialSource
import com.kudikakra.ui.components.AddTransactionDialog
import com.kudikakra.ui.viewmodel.SpendingPlanViewModel
import com.kudikakra.ui.viewmodel.TransactionViewModel
import com.kudikakra.ui.viewmodel.UserPreferencesViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private enum class AppScreen(
    val title: String,
    val shortLabel: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val isDeveloperTool: Boolean = false
) {
    DASHBOARD("Today", "Home", Icons.Outlined.Home, Icons.Filled.Home),
    PLAN("Spending plan", "Plan", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    TRANSACTIONS("Transactions", "History", Icons.AutoMirrored.Outlined.ReceiptLong, Icons.AutoMirrored.Filled.ReceiptLong),
    SETTINGS("Settings", "Settings", Icons.Outlined.Settings, Icons.Filled.Settings),
    DEVELOPER("Developer tools", "Tools", Icons.Outlined.BugReport, Icons.Filled.BugReport, isDeveloperTool = true),
    NOTIFICATION_INSPECTOR("Notification inspector", "Inspect", Icons.Outlined.BugReport, Icons.Filled.BugReport, isDeveloperTool = true),
    PARSER_PLAYGROUND("Parser playground", "Playground", Icons.Outlined.BugReport, Icons.Filled.BugReport, isDeveloperTool = true)
}

@Composable
fun KudiKakraApp(
    transactionViewModel: TransactionViewModel? = null,
    spendingPlanViewModel: SpendingPlanViewModel? = null,
    userPreferencesViewModel: UserPreferencesViewModel? = null
) {
    val preferences by if (userPreferencesViewModel != null) {
        userPreferencesViewModel.preferences.collectAsState()
    } else {
        remember { mutableStateOf(UserPreferencesEntity()) }
    }

    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }

    val visibleNavScreens = remember(preferences.developerModeEnabled) {
        if (preferences.developerModeEnabled) {
            listOf(
                AppScreen.DASHBOARD,
                AppScreen.PLAN,
                AppScreen.TRANSACTIONS,
                AppScreen.SETTINGS,
                AppScreen.DEVELOPER
            )
        } else {
            listOf(
                AppScreen.DASHBOARD,
                AppScreen.PLAN,
                AppScreen.TRANSACTIONS,
                AppScreen.SETTINGS
            )
        }
    }

    if (!preferences.developerModeEnabled && currentScreen.isDeveloperTool) {
        currentScreen = AppScreen.DASHBOARD
    }

    val context = LocalContext.current
    val notificationAccessEnabled = remember {
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }
    var showPermissionPromptDialog by remember { mutableStateOf(!notificationAccessEnabled) }

    if (showPermissionPromptDialog && !notificationAccessEnabled) {
        AlertDialog(
            onDismissRequest = { showPermissionPromptDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Allow Notification Access") },
            text = {
                Text("KudiKakra automatically detects MoMo payment alerts on your device to keep your daily spending plan updated. All data is processed 100% locally on your phone without internet access.\n\nPlease grant Notification Listener Access so the app can function automatically.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionPromptDialog = false
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                ) {
                    Text("Grant Access")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionPromptDialog = false }) {
                    Text("Later")
                }
            }
        )
    }

    Scaffold(
        topBar = { AppHeader(title = currentScreen.title) },
        bottomBar = {
            NavigationBar {
                visibleNavScreens.forEach { screen ->
                    val isSelected = currentScreen == screen ||
                        (screen == AppScreen.DEVELOPER && (currentScreen == AppScreen.NOTIFICATION_INSPECTOR || currentScreen == AppScreen.PARSER_PLAYGROUND))
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) screen.selectedIcon else screen.icon,
                                contentDescription = screen.shortLabel
                            )
                        },
                        label = { Text(screen.shortLabel) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentScreen) {
                AppScreen.DASHBOARD -> DashboardScreen(
                    viewModel = transactionViewModel,
                    spendingPlanViewModel = spendingPlanViewModel,
                    onOpenPlan = { currentScreen = AppScreen.PLAN },
                    onOpenTransactions = { currentScreen = AppScreen.TRANSACTIONS }
                )

                AppScreen.TRANSACTIONS -> TransactionsScreen(transactionViewModel)
                AppScreen.PLAN -> SpendingPlanScreen(spendingPlanViewModel)
                AppScreen.SETTINGS -> SettingsScreen(userPreferencesViewModel)
                AppScreen.DEVELOPER -> DeveloperScreen(
                    onOpenNotificationInspector = {
                        currentScreen = AppScreen.NOTIFICATION_INSPECTOR
                    },
                    onOpenParserPlayground = {
                        currentScreen = AppScreen.PARSER_PLAYGROUND
                    }
                )
                AppScreen.NOTIFICATION_INSPECTOR -> NotificationInspectorScreen()
                AppScreen.PARSER_PLAYGROUND -> ParserPlaygroundScreen()
            }
        }
    }
}

@Composable
private fun AppHeader(title: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "KudiKakra",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(text = title, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun DashboardScreen(
    viewModel: TransactionViewModel?,
    spendingPlanViewModel: SpendingPlanViewModel?,
    onOpenPlan: () -> Unit,
    onOpenTransactions: () -> Unit
) {
    val budgetSummary by if (spendingPlanViewModel != null) {
        spendingPlanViewModel.todaySummary.collectAsState()
    } else {
        remember { mutableStateOf(BudgetEngine.calculate(null, 0L)) }
    }
    val transactionCount by if (viewModel != null) {
        viewModel.transactions.collectAsState()
    } else {
        remember { mutableStateOf(emptyList<TransactionEntity>()) }
    }
    val pendingReviewList by if (viewModel != null) {
        viewModel.pendingReviewTransactions.collectAsState()
    } else {
        remember { mutableStateOf(emptyList<TransactionEntity>()) }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                LocalDate.now().format(
                    DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
                ),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        if (pendingReviewList.isNotEmpty()) {
            item {
                Card {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pending Review", fontWeight = FontWeight.Bold)
                            Text(
                                "${pendingReviewList.size} transaction(s) require confirmation before updating spending.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Button(onClick = onOpenTransactions) { Text("Review") }
                    }
                }
            }
        }
        item {
            SpendingSummaryCard(
                spent = formatMoney(budgetSummary.spentMinorUnits),
                plan = budgetSummary.planMinorUnits?.let(::formatMoney) ?: "No plan",
                remaining = budgetSummary.remainingMinorUnits?.let(::formatMoney) ?: "—",
                progress = (budgetSummary.percentageUsed / 100f).coerceIn(0f, 1f),
                status = budgetStatusLabel(budgetSummary.status)
            )
        }
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            if (transactionCount.isEmpty()) "No spending recorded yet"
                            else "${transactionCount.size} transaction(s) recorded",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "KudiKakra tracks your daily spending locally. Expenses automatically update your daily spending plan.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onOpenTransactions) { Text("View transactions") }
                        OutlinedButton(onClick = onOpenPlan) { Text("Set plan") }
                    }
                }
            }
        }
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Add Home Screen Widget", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Add the KudiKakra widget to your phone's home screen to easily check today's remaining spending at a glance with a compact squircle design.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("How KudiKakra counts spending", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Expense — counts toward today's plan automatically.", style = MaterialTheme.typography.bodyMedium)
                    Text("• Income, Transfers & Cash Withdrawals — excluded from daily spending.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun SpendingSummaryCard(
    spent: String,
    plan: String,
    remaining: String,
    progress: Float,
    status: String
) {
    Card {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Today's spending", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(spent, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text("of $plan daily plan", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("$remaining remaining", fontWeight = FontWeight.SemiBold)
            Text(status, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun budgetStatusLabel(status: BudgetStatus): String = when (status) {
    BudgetStatus.NO_PLAN -> "Set a plan to track today's limit."
    BudgetStatus.ON_TRACK -> "You are on track with today's plan."
    BudgetStatus.APPROACHING_LIMIT -> "You are approaching today's plan."
    BudgetStatus.EXCEEDED -> "Today's plan has been exceeded."
}

private data class TransactionDayGroup(
    val date: LocalDate,
    val headerTitle: String,
    val dailySpentMinorUnits: Long,
    val transactions: List<TransactionEntity>
)

private fun groupTransactionsByDay(transactions: List<TransactionEntity>): List<TransactionDayGroup> {
    val zoneId = ZoneId.systemDefault()
    val today = LocalDate.now(zoneId)
    val yesterday = today.minusDays(1)

    return transactions
        .groupBy {
            Instant.ofEpochMilli(it.timestampEpochMillis)
                .atZone(zoneId)
                .toLocalDate()
        }
        .entries
        .sortedByDescending { it.key }
        .map { (date, dayTxns) ->
            val title = when (date) {
                today -> "Today · ${date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))}"
                yesterday -> "Yesterday · ${date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))}"
                else -> date.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.getDefault()))
            }
            val dailySpent = dayTxns
                .filter { it.type == com.kudikakra.domain.model.TransactionType.EXPENSE && !it.excludedFromSpending }
                .sumOf { it.amountMinorUnits }

            TransactionDayGroup(
                date = date,
                headerTitle = title,
                dailySpentMinorUnits = dailySpent,
                transactions = dayTxns.sortedByDescending { it.timestampEpochMillis }
            )
        }
}

@Composable
private fun TransactionsScreen(viewModel: TransactionViewModel?) {
    val rawTransactions by viewModel?.transactions?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }
    val pendingTransactions by viewModel?.pendingReviewTransactions?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }
    var selectedTypeFilter by remember { mutableStateOf<com.kudikakra.domain.model.TransactionType?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    val filteredTransactions = remember(rawTransactions, selectedTypeFilter) {
        if (selectedTypeFilter == null) rawTransactions
        else rawTransactions.filter { it.type == selectedTypeFilter }
    }

    val dayGroups = remember(filteredTransactions) {
        groupTransactionsByDay(filteredTransactions)
    }

    val totalExpensesSum = remember(rawTransactions) {
        rawTransactions.filter { it.type == com.kudikakra.domain.model.TransactionType.EXPENSE && !it.excludedFromSpending }.sumOf { it.amountMinorUnits }
    }
    val totalIncomeSum = remember(rawTransactions) {
        rawTransactions.filter { it.type == com.kudikakra.domain.model.TransactionType.INCOME }.sumOf { it.amountMinorUnits }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add transaction")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).imePadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                top = 20.dp,
                end = 20.dp,
                bottom = 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary Card
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Transaction History Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Spent", style = MaterialTheme.typography.bodySmall)
                                Text(formatMoney(totalExpensesSum), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                            Column {
                                Text("Total Income", style = MaterialTheme.typography.bodySmall)
                                Text(formatMoney(totalIncomeSum), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Column {
                                Text("Recorded", style = MaterialTheme.typography.bodySmall)
                                Text("${rawTransactions.size} item(s)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filters = listOf(
                        null to "All",
                        com.kudikakra.domain.model.TransactionType.EXPENSE to "Expenses",
                        com.kudikakra.domain.model.TransactionType.INCOME to "Income",
                        com.kudikakra.domain.model.TransactionType.TRANSFER to "Transfers",
                        com.kudikakra.domain.model.TransactionType.WITHDRAWAL to "Withdrawals"
                    )
                    filters.forEach { (type, label) ->
                        val isSelected = selectedTypeFilter == type
                        if (isSelected) {
                            Button(
                                onClick = { selectedTypeFilter = type },
                                modifier = Modifier.height(36.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(label, style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { selectedTypeFilter = type },
                                modifier = Modifier.height(36.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(label, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // Pending Review Section
            if (pendingTransactions.isNotEmpty()) {
                item {
                    Text(
                        "Needs Confirmation (${pendingTransactions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(pendingTransactions) { pending ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${pending.source} • ${pending.type.name}",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    formatMoney(pending.amountMinorUnits),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                "Merchant: ${pending.merchant ?: "(unknown)"} | Confidence: ${pending.confidence}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel?.confirmTransaction(pending) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Confirm")
                                }
                                OutlinedButton(
                                    onClick = { editingTransaction = pending },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Edit")
                                }
                                TextButton(
                                    onClick = { transactionToDelete = pending }
                                ) {
                                    Text("Reject")
                                }
                            }
                        }
                    }
                }
            }

            if (filteredTransactions.isEmpty()) {
                item {
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("No transactions found", fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Click the + button to add a manual transaction or change filter.")
                        }
                    }
                }
            } else {
                dayGroups.forEach { group ->
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = group.headerTitle,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "${formatMoney(group.dailySpentMinorUnits)} spent",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    items(group.transactions) { t ->
                        TransactionItemCard(
                            transaction = t,
                            onEdit = { editingTransaction = t },
                            onDelete = { transactionToDelete = t }
                        )
                    }
                }
            }
            item { TransactionRuleLegend() }
        }
    }

    if (showAddDialog || editingTransaction != null) {
        AddTransactionDialog(
            onDismiss = {
                showAddDialog = false
                editingTransaction = null
            },
            onSave = { entity ->
                if (editingTransaction != null) {
                    viewModel?.update(entity)
                } else {
                    viewModel?.insert(entity)
                }
                showAddDialog = false
                editingTransaction = null
            },
            transactionToEdit = editingTransaction
        )
    }

    transactionToDelete?.let { transaction ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Delete transaction?") },
            text = { Text("This will permanently remove the local transaction record.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel?.delete(transaction)
                        transactionToDelete = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TransactionItemCard(
    transaction: TransactionEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val timeStr = Instant.ofEpochMilli(transaction.timestampEpochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))

    val isExpense = transaction.type == com.kudikakra.domain.model.TransactionType.EXPENSE
    val isIncome = transaction.type == com.kudikakra.domain.model.TransactionType.INCOME

    val amountPrefix = when {
        isExpense -> "-"
        isIncome -> "+"
        else -> ""
    }
    val amountColor = when {
        isExpense -> MaterialTheme.colorScheme.error
        isIncome -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchant ?: transaction.type.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${transaction.source} • ${transaction.type.name} • $timeStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                transaction.reference?.let { ref ->
                    Text(
                        text = "Ref: $ref",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix${formatMoney(transaction.amountMinorUnits)}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = amountColor
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

private fun formatMoney(amountMinorUnits: Long): String =
    "GH₵${String.format(Locale.US, "%,.2f", amountMinorUnits / 100.0)}"

private fun parseMinorUnits(value: String): Long? = runCatching {
    BigDecimal(value.trim())
        .movePointRight(2)
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact()
}.getOrNull()

@Composable
private fun TransactionRuleLegend() {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Transaction types", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Expense — counts toward spending")
            Text("Income — money received; excluded")
            Text("Transfer — money moved; excluded by default")
            Text("Withdrawal — converted to cash; excluded by default")
            Text("Unknown — requires review; never counts automatically")
        }
    }
}

@Composable
private fun SpendingPlanScreen(viewModel: SpendingPlanViewModel?) {
    val budgets by if (viewModel != null) {
        viewModel.budgets.collectAsState()
    } else {
        remember { mutableStateOf(emptyList<DailyBudgetEntity>()) }
    }
    val days = listOf(
        java.util.Calendar.MONDAY to "Monday",
        java.util.Calendar.TUESDAY to "Tuesday",
        java.util.Calendar.WEDNESDAY to "Wednesday",
        java.util.Calendar.THURSDAY to "Thursday",
        java.util.Calendar.FRIDAY to "Friday",
        java.util.Calendar.SATURDAY to "Saturday",
        java.util.Calendar.SUNDAY to "Sunday"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            top = 20.dp,
            end = 20.dp,
            bottom = 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Set a separate spending plan for each day. Changes are saved locally on this device.")
        }
        items(days) { (dayOfWeek, dayName) ->
            val budget = budgets.firstOrNull { it.dayOfWeek == dayOfWeek }
            BudgetEditorRow(
                dayOfWeek = dayOfWeek,
                dayName = dayName,
                initialAmountMinorUnits = budget?.amountMinorUnits ?: 0L,
                onSave = { amountMinorUnits ->
                    viewModel?.save(
                        DailyBudgetEntity(
                            dayOfWeek = dayOfWeek,
                            amountMinorUnits = amountMinorUnits,
                            updatedAtEpochMillis = System.currentTimeMillis()
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun BudgetEditorRow(
    dayOfWeek: Int,
    dayName: String,
    initialAmountMinorUnits: Long,
    onSave: (Long) -> Unit
) {
    var amount by remember(dayOfWeek, initialAmountMinorUnits) {
        mutableStateOf(
            if (initialAmountMinorUnits == 0L) ""
            else BigDecimal.valueOf(initialAmountMinorUnits, 2).toPlainString()
        )
    }
    var error by remember(dayOfWeek) { mutableStateOf<String?>(null) }

    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(dayName, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = amount,
                onValueChange = {
                    amount = it
                    error = null
                },
                label = { Text("Daily plan (GH₵)") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    val parsed = parseMinorUnits(amount)
                    if (parsed == null || parsed <= 0) {
                        error = "Enter a plan greater than zero."
                    } else {
                        onSave(parsed)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save $dayName plan") }
        }
    }
}

@Composable
private fun SettingsScreen(viewModel: UserPreferencesViewModel?) {
    val context = LocalContext.current
    val notificationAccessEnabled = NotificationManagerCompat
        .getEnabledListenerPackages(context)
        .contains(context.packageName)
    var postNotificationsAllowed by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                NotificationManagerCompat.from(context).areNotificationsEnabled()
        )
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        postNotificationsAllowed = granted || NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!postNotificationsAllowed) {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            )
        }
    }
    val preferences by if (viewModel != null) {
        viewModel.preferences.collectAsState()
    } else {
        remember { mutableStateOf(UserPreferencesEntity()) }
    }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingSwitch(
                title = "Automatic tracking",
                description = "Read supported financial notifications on this device.",
                checked = preferences.automaticTrackingEnabled,
                onCheckedChange = { viewModel?.setAutomaticTrackingEnabled(it) }
            )
        }

        item {
            SettingSwitch(
                title = "Spending notifications",
                description = "Notify you when you approach or exceed today's plan.",
                checked = preferences.spendingNotificationsEnabled,
                onCheckedChange = { viewModel?.setSpendingNotificationsEnabled(it) }
            )
        }

        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Spending alert permission", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (postNotificationsAllowed) "Status: Allowed" else "Status: Not allowed. Android is blocking budget alerts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (postNotificationsAllowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    if (!postNotificationsAllowed) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        ) { Text("Allow spending alerts") }
                    }
                }
            }
        }

        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Alert Threshold", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Notify when daily spending reaches ${preferences.notificationThresholdPercent}% of today's plan.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Slider(
                            value = preferences.notificationThresholdPercent.toFloat(),
                            onValueChange = { viewModel?.setNotificationThresholdPercent(it.roundToInt()) },
                            valueRange = 10f..100f,
                            steps = 17,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${preferences.notificationThresholdPercent}%",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        }

        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Monitored Financial Sources", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Select which financial providers KudiKakra should monitor.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val enabledSources = preferences.getEnabledFinancialSources()
                    FinancialSource.entries.forEach { source ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(source.displayName, modifier = Modifier.weight(1f))
                            Switch(
                                checked = source in enabledSources,
                                onCheckedChange = { isChecked ->
                                    viewModel?.toggleSource(source, isChecked)
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Notification Listener Permission", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (notificationAccessEnabled) {
                            "Status: Granted"
                        } else {
                            "Status: Not Granted — Notification tracking requires permission."
                        },
                        fontWeight = FontWeight.Bold,
                        color = if (notificationAccessEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    ) { Text("Manage notification access") }
                }
            }
        }

        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Privacy & Data Guarantees", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• Local-First: All financial data is processed and stored strictly on this device.", style = MaterialTheme.typography.bodyMedium)
                    Text("• No Cloud / No Server: KudiKakra operates without backend servers, cloud databases, or accounts.", style = MaterialTheme.typography.bodyMedium)
                    Text("• No AI APIs: Notification data is never sent to remote AI services.", style = MaterialTheme.typography.bodyMedium)
                    Text("• Zero Raw Storage: Raw notification text is parsed transiently in memory and never stored to disk.", style = MaterialTheme.typography.bodyMedium)
                    Text("• No Internet Permission: The app requests zero network access permissions.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Data Management", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Permanently wipe all transaction history, spending plans, and local settings.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete all local data")
                    }
                }
            }
        }

        item {
            SettingSwitch(
                title = "Developer mode",
                description = "Enable developer tools for testing parsers and inspecting notifications.",
                checked = preferences.developerModeEnabled,
                onCheckedChange = { viewModel?.setDeveloperModeEnabled(it) }
            )
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete all local data?") },
            text = { Text("This will permanently remove all transactions, spending plans, and user preferences stored on this device. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel?.deleteAllLocalData()
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text("Delete Everything", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodyMedium)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun DeveloperScreen(
    onOpenNotificationInspector: () -> Unit,
    onOpenParserPlayground: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Use these tools during development. Keep disabled in normal use.")
        }
        item {
            DeveloperToolCard(
                title = "Notification inspector",
                description = "View package, title, text, and timestamp without saving raw notifications.",
                enabled = true,
                onClick = onOpenNotificationInspector
            )
        }
        item {
            DeveloperToolCard(
                title = "Parser playground",
                description = "Test representative provider messages before using real notifications.",
                enabled = true,
                onClick = onOpenParserPlayground
            )
        }
        item {
            DeveloperToolCard("Test budget", "Try below-plan, at-plan, and over-plan spending states.")
        }
        item {
            DeveloperToolCard("Test transaction", "Create controlled expense, income, transfer, withdrawal, and unknown examples.")
        }
    }
}

@Composable
private fun DeveloperToolCard(
    title: String,
    description: String,
    enabled: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onClick, enabled = enabled) {
                Text(if (enabled) "Open tool" else "Open tool (next phase)")
            }
        }
    }
}

@Composable
private fun NotificationInspectorScreen() {
    val isServiceConnected by NotificationInspectorStore.isServiceConnected.collectAsState()
    val inspectedItems by NotificationInspectorStore.inspectedNotifications.collectAsState()
    val detector = remember { FinancialNotificationDetector() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Listener Service Status", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (isServiceConnected) "Service Connected" else "Service Disconnected",
                        color = if (isServiceConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        item {
            Text(
                "Events are kept in memory only and are cleared when the app process ends.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { NotificationInspectorStore.clear() }) {
                    Text("Clear events")
                }
                Button(onClick = {
                    val sampleEvent = NotificationEvent(
                        packageName = "com.mtn.momo",
                        title = "MTN MoMo",
                        text = "You have paid GH₵25.00 to Accra Groceries. Transaction ID: ${System.currentTimeMillis().toString().takeLast(6)}",
                        timestampEpochMillis = System.currentTimeMillis()
                    )
                    val detection = detector.detect(sampleEvent)
                    NotificationInspectorStore.add(sampleEvent, detection)
                }) {
                    Text("Simulate MoMo Notification")
                }
            }
        }
        if (inspectedItems.isEmpty()) {
            item {
                Card {
                    Text(
                        "No notifications received yet. Enable notification access, or click 'Simulate MoMo Notification' to test.",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(inspectedItems) { item -> InspectedNotificationCard(item) }
        }
    }
}

@Composable
private fun ParserPlaygroundScreen() {
    var packageName by remember { mutableStateOf("com.mtn.momo") }
    var title by remember { mutableStateOf("MobileMoney") }
    var text by remember {
        mutableStateOf("Payment made for GH₵ 25.00 to Accra Groceries. Transaction ID: 10293847561. Fee charged: GH₵ 0.00.")
    }
    var parseResult by remember { mutableStateOf<ParseResult?>(null) }
    val registry = remember { ParserRegistry() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Test financial notification messages against active provider parsers.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Preset Samples", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                packageName = "com.mtn.momo"
                                title = "MobileMoney"
                                text = "Payment made for GH₵ 25.00 to Accra Groceries. Transaction ID: 10293847561. Fee charged: GH₵ 0.00."
                            }
                        ) { Text("Expense") }
                        OutlinedButton(
                            onClick = {
                                packageName = "com.mtn.momo"
                                title = "MobileMoney"
                                text = "An amount of GH₵ 200.00 has been received from JOHN DOE. Transaction ID: 12345."
                            }
                        ) { Text("Income") }
                        OutlinedButton(
                            onClick = {
                                packageName = "com.mtn.momo"
                                title = "MobileMoney"
                                text = "You have transferred GH₵ 100.00 to KOFI MENSAH. Transaction ID: 88776655."
                            }
                        ) { Text("Transfer") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                packageName = "com.mtn.momo"
                                title = "MobileMoney"
                                text = "Cash Out of GH₵ 300.00 from Agent AGENT NAME. Transaction ID: 55443322."
                            }
                        ) { Text("Withdrawal") }
                        OutlinedButton(
                            onClick = {
                                packageName = "com.mtn.momo"
                                title = "MobileMoney"
                                text = "Transaction failed due to insufficient balance."
                            }
                        ) { Text("Unknown") }
                    }
                }
            }
        }

        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = packageName,
                        onValueChange = { packageName = it },
                        label = { Text("Package name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Notification title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Notification text") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val event = NotificationEvent(
                                packageName = packageName,
                                title = title,
                                text = text,
                                timestampEpochMillis = System.currentTimeMillis()
                            )
                            parseResult = registry.parse(event)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Parse Notification")
                    }
                }
            }
        }

        parseResult?.let { result ->
            item {
                ParsedResultCard(result)
            }
        }
    }
}

@Composable
private fun ParsedResultCard(result: ParseResult) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Parse Result", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Status: ${if (result.success) "SUCCESS" else "FAILED"}",
                color = if (result.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
            Text("Reason: ${result.reason}", style = MaterialTheme.typography.bodySmall)

            result.transaction?.let { txn ->
                val fingerprint = TransactionFingerprintGenerator.generate(txn)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Source: ${txn.source}", fontWeight = FontWeight.SemiBold)
                Text("Amount: ${formatMoney(txn.amountMinorUnits)} (${txn.amountMinorUnits} minor units)")
                Text("Currency: ${txn.currency}")
                Text("Direction: ${txn.direction}")
                Text("Type: ${txn.type}")
                Text("Merchant/Party: ${txn.merchant ?: "(none)"}")
                Text("Reference: ${txn.reference ?: "(none)"}")
                Text("Confidence: ${txn.confidence}")
                Text("Excluded from spending: ${txn.excludedFromSpending}")
                Text("Fingerprint: ${fingerprint.take(16)}...", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun InspectedNotificationCard(item: InspectedNotification) {
    val event = item.event
    val detection = item.detectionResult
    val timestamp = Instant.ofEpochMilli(event.timestampEpochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault()))

    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(event.packageName, fontWeight = FontWeight.SemiBold)
            Text("Title: ${event.title.ifBlank { "(none)" }}")
            Text("Text: ${event.text.ifBlank { "(none)" }}")
            Text("Received: $timestamp", style = MaterialTheme.typography.bodySmall)

            detection?.let { result ->
                Spacer(modifier = Modifier.height(4.dp))
                when (result) {
                    is DetectionResult.Financial -> {
                        Text(
                            "Detection: FINANCIAL (${result.confidence}) - ${result.matchedSource?.displayName ?: "Unknown Provider"}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Reason: ${result.reason}", style = MaterialTheme.typography.bodySmall)
                    }
                    is DetectionResult.NotFinancial -> {
                        Text(
                            "Detection: NOT FINANCIAL",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text("Reason: ${result.reason}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun KudiKakraAppPreview() {
    com.kudikakra.ui.theme.KudiKakraTheme {
        KudiKakraApp()
    }
}
