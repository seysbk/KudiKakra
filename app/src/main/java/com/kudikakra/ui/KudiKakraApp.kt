package com.kudikakra.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class AppScreen(val title: String, val shortLabel: String) {
    DASHBOARD("Today", "Home"),
    TRANSACTIONS("Transactions", "History"),
    PLAN("Spending plan", "Plan"),
    SETTINGS("Settings", "Settings"),
    DEVELOPER("Developer tools", "Tools")
}

@Composable
fun KudiKakraApp() {
    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }

    Scaffold(
        topBar = { AppHeader(title = currentScreen.title) },
        bottomBar = {
            NavigationBar {
                AppScreen.entries.forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        icon = { Text(screen.shortLabel.take(1)) },
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
                    onOpenPlan = { currentScreen = AppScreen.PLAN },
                    onOpenTransactions = { currentScreen = AppScreen.TRANSACTIONS }
                )

                AppScreen.TRANSACTIONS -> TransactionsScreen()
                AppScreen.PLAN -> SpendingPlanScreen()
                AppScreen.SETTINGS -> SettingsScreen()
                AppScreen.DEVELOPER -> DeveloperScreen()
            }
        }
    }
}

@Composable
private fun AppHeader(title: String) {
    Surface(shadowElevation = 2.dp) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
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
    onOpenPlan: () -> Unit,
    onOpenTransactions: () -> Unit
) {
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
        item {
            SpendingSummaryCard(
                spent = "GH₵0.00",
                plan = "GH₵60.00",
                remaining = "GH₵60.00",
                progress = 0f
            )
        }
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("No spending recorded yet", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Add transactions manually first. Automatic notification detection will be added after the local flow is reliable.",
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
                    Text("How KudiKakra counts spending", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Expenses count toward today's plan.")
                    Text("Income, transfers, and cash withdrawals do not count automatically.")
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
    progress: Float
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
        }
    }
}

@Composable
private fun TransactionsScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Transactions will be stored locally on this device.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("No transactions yet", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Manual expense entry will be added next, before notification parsing.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { }, enabled = false) { Text("Add transaction (next phase)") }
                }
            }
        }
        item { TransactionRuleLegend() }
    }
}

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
private fun SpendingPlanScreen() {
    val days = listOf(
        "Monday" to "GH₵40",
        "Tuesday" to "GH₵60",
        "Wednesday" to "GH₵40",
        "Thursday" to "GH₵60",
        "Friday" to "GH₵80",
        "Saturday" to "GH₵120",
        "Sunday" to "GH₵60"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Set a separate spending plan for each day. Plans are temporary until Room storage is added.")
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(days) { (day, amount) ->
            Card {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(day, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { }, enabled = false) { Text(amount) }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen() {
    var trackingEnabled by remember { mutableStateOf(false) }
    var spendingNotifications by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingSwitch(
                title = "Automatic tracking",
                description = "Read supported financial notifications on this device.",
                checked = trackingEnabled,
                onCheckedChange = { trackingEnabled = it }
            )
        }
        item {
            SettingSwitch(
                title = "Spending notifications",
                description = "Notify you when you approach or exceed today's plan.",
                checked = spendingNotifications,
                onCheckedChange = { spendingNotifications = it }
            )
        }
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Privacy", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("KudiKakra is local-first. Financial data should stay on your device and raw notifications should not be stored permanently.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { }, enabled = false) { Text("Notification access status (next phase)") }
                    OutlinedButton(onClick = { }, enabled = false) { Text("Delete local data (next phase)") }
                }
            }
        }
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
private fun DeveloperScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Use these tools during development. Do not expose them as part of the final user experience.")
        }
        item {
            DeveloperToolCard("Notification inspector", "View package, title, text, and timestamp without saving raw notifications.")
        }
        item {
            DeveloperToolCard("Parser playground", "Test representative provider messages before using real notifications.")
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
private fun DeveloperToolCard(title: String, description: String) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = { }, enabled = false) { Text("Open tool (next phase)") }
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
