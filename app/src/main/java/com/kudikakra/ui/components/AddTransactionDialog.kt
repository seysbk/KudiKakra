package com.kudikakra.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kudikakra.data.local.entity.TransactionEntity
import com.kudikakra.domain.model.ConfidenceLevel
import com.kudikakra.domain.model.TransactionDirection
import com.kudikakra.domain.model.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit,
    transactionToEdit: TransactionEntity? = null
) {
    val initialTimestamp = transactionToEdit?.timestampEpochMillis ?: System.currentTimeMillis()
    val dateFormatter = remember { SimpleDateFormat(DATE_TIME_PATTERN, Locale.getDefault()) }
    var amount by remember(transactionToEdit?.id) {
        mutableStateOf(
            transactionToEdit?.let {
                BigDecimal.valueOf(it.amountMinorUnits, 2).toPlainString()
            } ?: ""
        )
    }
    var merchant by remember(transactionToEdit?.id) {
        mutableStateOf(transactionToEdit?.merchant.orEmpty())
    }
    var type by remember(transactionToEdit?.id) {
        mutableStateOf(transactionToEdit?.type ?: TransactionType.EXPENSE)
    }
    var source by remember(transactionToEdit?.id) {
        mutableStateOf(transactionToEdit?.source ?: "Manual")
    }
    var dateTime by remember(transactionToEdit?.id) {
        mutableStateOf(dateFormatter.format(Date(initialTimestamp)))
    }
    var expandedType by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (transactionToEdit == null) "Add transaction" else "Edit transaction") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        validationError = null
                    },
                    label = { Text("Amount (GH₵)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant / description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = { Text("Source (e.g. Cash, MoMo)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dateTime,
                    onValueChange = {
                        dateTime = it
                        validationError = null
                    },
                    label = { Text("Date and time ($DATE_TIME_PATTERN)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = expandedType,
                    onExpandedChange = { expandedType = !expandedType }
                ) {
                    OutlinedTextField(
                        value = type.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Transaction type") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType)
                        },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = expandedType,
                        onDismissRequest = { expandedType = false }
                    ) {
                        TransactionType.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name) },
                                onClick = {
                                    type = option
                                    expandedType = false
                                }
                            )
                        }
                    }
                }
                validationError?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountMinorUnits = parseAmount(amount)
                    val timestamp = runCatching { dateFormatter.parse(dateTime)?.time }.getOrNull()
                    when {
                        amountMinorUnits == null || amountMinorUnits <= 0 -> {
                            validationError = "Enter an amount greater than zero."
                        }
                        timestamp == null -> {
                            validationError = "Use the date format yyyy-MM-dd HH:mm."
                        }
                        else -> {
                            onSave(
                                TransactionEntity(
                                    id = transactionToEdit?.id ?: 0,
                                    source = source.trim().ifBlank { "Manual" },
                                    amountMinorUnits = amountMinorUnits,
                                    currency = transactionToEdit?.currency ?: "GHS",
                                    type = type,
                                    direction = directionFor(type),
                                    merchant = merchant.trim().takeIf { it.isNotBlank() },
                                    reference = transactionToEdit?.reference,
                                    timestampEpochMillis = timestamp,
                                    confidence = ConfidenceLevel.HIGH,
                                    category = transactionToEdit?.category,
                                    excludedFromSpending = type != TransactionType.EXPENSE,
                                    fingerprint = transactionToEdit?.fingerprint,
                                    createdAtEpochMillis = transactionToEdit?.createdAtEpochMillis
                                        ?: System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun parseAmount(value: String): Long? = runCatching {
    BigDecimal(value.trim())
        .movePointRight(2)
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact()
}.getOrNull()

private fun directionFor(type: TransactionType): TransactionDirection = when (type) {
    TransactionType.INCOME -> TransactionDirection.IN
    TransactionType.EXPENSE,
    TransactionType.TRANSFER,
    TransactionType.WITHDRAWAL -> TransactionDirection.OUT
    TransactionType.UNKNOWN -> TransactionDirection.UNKNOWN
}
