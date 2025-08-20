package com.example.incometaxcalculator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
@OptIn(ExperimentalMaterial3Api::class) // Or the specific experimental annotation
fun DraftOrdersScreen() {
    var text by remember { mutableStateOf("") }
    Scaffold(topBar = { TopAppBar(title = { Text("Draft Orders") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Prepare draft orders, notes and templates.")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Draft content") },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { /* TODO: Save/export */ }, modifier = Modifier.fillMaxWidth()) {
                Text("Save Draft")
            }
        }
    }
}
