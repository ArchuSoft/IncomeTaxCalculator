package com.example.incometaxcalculator.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.incometaxcalculator.data.AssessmentYear
import com.example.incometaxcalculator.data.Compute
import com.example.incometaxcalculator.data.FinalComputation
import com.example.incometaxcalculator.data.Regime
import com.example.incometaxcalculator.data.TaxInput
import com.example.incometaxcalculator.data.TaxPayment
import com.example.incometaxcalculator.data.TaxStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun IncomeTaxScreen() {
    var ay by remember { mutableStateOf(AssessmentYear.AY_2025_26) }
    var status by remember { mutableStateOf(TaxStatus.INDIVIDUAL) }
    var regime by remember { mutableStateOf(Regime.OLD) }

    var incomeText by remember { mutableStateOf("") }
    var tdsText by remember { mutableStateOf("0") }
    var d80C by remember { mutableStateOf("0") }
    var dOther by remember { mutableStateOf("0") }

    // Dates: due date and filing date
    var dueDate by remember { mutableStateOf<LocalDate?>(null) }
    var filingDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDueDatePicker by remember { mutableStateOf(false) }
    var showFilingDatePicker by remember { mutableStateOf(false) }

    // Advance tax and self-assessment payments
    val advancePayments = remember { mutableStateListOf<PaymentUiItem>() }
    val selfPayments = remember { mutableStateListOf<PaymentUiItem>() }

    var exempt234C by remember { mutableStateOf(false) }

    var result by remember { mutableStateOf<FinalComputation?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Income Tax Calculator") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AYStatusRow(
                    ay = ay,
                    onAy = { ay = it },
                    status = status,
                    onStatus = { status = it }
                )
            }
            item {
                RegimeSelector(regime = regime, onRegime = { regime = it })
            }
            item {
                OutlinedTextField(
                    value = incomeText,
                    onValueChange = { incomeText = it },
                    label = { Text("Total Income (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (status in listOf(
                    TaxStatus.INDIVIDUAL, TaxStatus.SENIOR_CITIZEN, TaxStatus.SUPER_SENIOR, TaxStatus.HUF
                ) && regime == Regime.OLD
            ) {
                item {
                    OutlinedTextField(
                        value = d80C,
                        onValueChange = { d80C = it },
                        label = { Text("80C (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = dOther,
                        onValueChange = { dOther = it },
                        label = { Text("Other deductions (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = tdsText,
                    onValueChange = { tdsText = it },
                    label = { Text("TDS/TCS (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Dates
            item {
                DateRow(
                    title = "Due date for return",
                    date = dueDate,
                    onPick = { showDueDatePicker = true }
                )
            }
            item {
                DateRow(
                    title = "Actual filing date",
                    date = filingDate,
                    onPick = { showFilingDatePicker = true }
                )
            }

            // Advance tax block
            item {
                SectionHeader("Advance Tax Payments")
            }
            itemsIndexed(advancePayments) { index, payment ->
                PaymentRow(
                    label = "Advance Tax #${index + 1}",
                    payment = payment,
                    onChange = { advancePayments[index] = it },
                    onRemove = { advancePayments.removeAt(index) }
                )
            }
            item {
                OutlinedButton(
                    onClick = { advancePayments.add(PaymentUiItem()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Advance Tax Payment")
                }
            }

            // Self-assessment block
            item {
                SectionHeader("Self-Assessment Tax Payments")
            }
            itemsIndexed(selfPayments) { index, payment ->
                PaymentRow(
                    label = "Self-Assessment #${index + 1}",
                    payment = payment,
                    onChange = { selfPayments[index] = it },
                    onRemove = { selfPayments.removeAt(index) }
                )
            }
            item {
                OutlinedButton(
                    onClick = { selfPayments.add(PaymentUiItem()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Self-Assessment Payment")
                }
            }

            // 234C exemption
            item {
                FilterChip(
                    selected = exempt234C,
                    onClick = { exempt234C = !exempt234C },
                    label = { Text(if (exempt234C) "234C Exemption: ON" else "234C Exemption: OFF") }
                )
            }

            // Action
            item {
                Button(
                    onClick = {
                        error = null
                        result = null
                        val income = incomeText.toDoubleOrNull()
                        val tds = tdsText.toDoubleOrNull() ?: 0.0
                        if (income == null) {
                            error = "Please enter a valid Total Income."
                            return@Button
                        }
                        // Transform UI lists
                        val advDomain = advancePayments.mapNotNull { it.toDomainOrNull() }
                        val selfDomain = selfPayments.mapNotNull { it.toDomainOrNull() }

                        try {
                            val input = TaxInput(
                                assessmentYear = ay,
                                status = status,
                                regime = regime,
                                totalIncome = income,
                                deductions80C = d80C.toDoubleOrNull() ?: 0.0,
                                deductionsOther = dOther.toDoubleOrNull() ?: 0.0,
                                advanceTaxPayments = advDomain,
                                tdsTcs = tds,
                                selfAssessmentTaxPaid = selfDomain,
                                returnFilingDate = filingDate,
                                dueDateForReturn = dueDate,
                                interest234CExemptIncomes = exempt234C
                            )
                            val res = Compute.computeAll(input)
                            result = res
                        } catch (e: Exception) {
                            error = e.message ?: "Computation failed."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Calculate")
                }
            }

            // Results
            item {
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
            item {
                result?.let { r ->
                    ResultCard(r)
                }
            }
        }
    }

    // Date pickers
    if (showDueDatePicker) {
        DatePickerDialogWrap(
            initial = dueDate,
            onDismiss = { showDueDatePicker = false },
            onConfirm = { picked ->
                dueDate = picked
                showDueDatePicker = false
            }
        )
    }
    if (showFilingDatePicker) {
        DatePickerDialogWrap(
            initial = filingDate,
            onDismiss = { showFilingDatePicker = false },
            onConfirm = { picked ->
                filingDate = picked
                showFilingDatePicker = false
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun ResultCard(r: FinalComputation) {
    ElevatedCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Taxable Income: ₹%,.2f".format(r.tax.taxableIncome))
            Text("Base Tax: ₹%,.2f".format(r.tax.baseTax))
            Text("Surcharge: ₹%,.2f".format(r.tax.surcharge))
            Text("Cess: ₹%,.2f".format(r.tax.cess))
            HorizontalDivider()
            Text("Total (before interest): ₹%,.2f".format(r.tax.totalBeforeInterest))
            Spacer(Modifier.height(8.dp))
            Text("Interest 234A: ₹%,.2f".format(r.interest.i234A))
            Text("Interest 234B: ₹%,.2f".format(r.interest.i234B))
            Text("Interest 234C: ₹%,.2f".format(r.interest.i234C))
            HorizontalDivider()
            Text(
                "Total Payable: ₹%,.2f".format(r.totalPayable),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun AYStatusRow(
    ay: AssessmentYear,
    onAy: (AssessmentYear) -> Unit,
    status: TaxStatus,
    onStatus: (TaxStatus) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ExposedDropdownMenuBoxField(
            label = "Assessment Year",
            options = AssessmentYear.allOrdered(),
            selected = ay,
            onSelect = onAy
        )
        ExposedDropdownMenuBoxField(
            label = "Status",
            options = TaxStatus.entries,
            selected = status,
            onSelect = onStatus
        )
    }
}

@Composable
private fun RegimeSelector(
    regime: Regime,
    onRegime: (Regime) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilterChip(
            selected = regime == Regime.OLD,
            onClick = { onRegime(Regime.OLD) },
            label = { Text("Old Regime") },
            leadingIcon = if (regime == Regime.OLD) ({ Icon(Icons.Default.Check, null) }) else null
        )
        FilterChip(
            selected = regime == Regime.NEW,
            onClick = { onRegime(Regime.NEW) },
            label = { Text("New Regime") },
            leadingIcon = if (regime == Regime.NEW) ({ Icon(Icons.Default.Check, null) }) else null
        )
    }
}

@Composable
private fun DateRow(
    title: String,
    date: LocalDate?,
    onPick: () -> Unit
) {
    OutlinedButton(onClick = onPick) {
        Icon(Icons.Default.DateRange, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("$title: ${date?.toString() ?: "Select"}")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DatePickerDialogWrap(
    initial: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate?) -> Unit
) {
    val initMillis = initial?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = initMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val picked = state.selectedDateMillis?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                }
                onConfirm(picked)
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ExposedDropdownMenuBoxField(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            readOnly = true,
            value = when (selected) {
                is AssessmentYear -> selected.label
                is TaxStatus -> selected.label
                else -> selected.toString()
            },
            onValueChange = {},
            label = { Text(label) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = {
                    Text(
                        when (opt) {
                            is AssessmentYear -> opt.label
                            is TaxStatus -> opt.label
                            else -> opt.toString()
                        }
                    )
                }, onClick = {
                    expanded = false
                    onSelect(opt)
                })
            }
        }
    }
}

/* --------------------------
   Payment UI helpers
   -------------------------- */

@RequiresApi(Build.VERSION_CODES.O)
private data class PaymentUiItem(
    val amountText: String = "",
    val date: LocalDate? = null
) {
    fun toDomainOrNull(): TaxPayment? {
        val amt = amountText.toDoubleOrNull() ?: return null
        val d = date ?: return null
        return TaxPayment(date = d, amount = amt)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun PaymentRow(
    label: String,
    payment: PaymentUiItem,
    onChange: (PaymentUiItem) -> Unit,
    onRemove: () -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = payment.amountText,
                onValueChange = { onChange(payment.copy(amountText = it)) },
                label = { Text("Amount (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = { showPicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(payment.date?.toString() ?: "Pick Date")
            }
        }
        Row {
            TextButton(onClick = onRemove) { Text("Remove") }
        }
    }

    if (showPicker) {
        val initMillis = payment.date?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = initMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val picked = state.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    onChange(payment.copy(date = picked))
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = state)
        }
    }
}
