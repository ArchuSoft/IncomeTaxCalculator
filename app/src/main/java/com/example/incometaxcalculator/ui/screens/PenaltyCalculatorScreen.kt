package com.example.incometaxcalculator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PenaltyCalculatorScreen(sectionCode: String) {
    var input1 by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Penalty Calculator: Section $sectionCode") })
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Enter required details for Section $sectionCode.")
            OutlinedTextField(
                value = input1,
                onValueChange = { input1 = it },
                label = { Text("Sample input") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                // TODO: Implement per section logic.
                result = "Computation for Section $sectionCode is not implemented yet."
            }) {
                Text("Compute")
            }
            result?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        }
    }
}
