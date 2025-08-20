package com.example.incometaxcalculator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ResourcesScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Resources") }) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Useful references (verify latest):")
            Text("- Income-tax India tax rate charts and help pages[3][4][1][20].")
            Text("- Domestic company rate/surcharge references[11].")
            Text("- Surcharge and marginal relief overview[17].")
            Text("- Penalties overview and sections[6][12][18][15].")
        }
    }
}
