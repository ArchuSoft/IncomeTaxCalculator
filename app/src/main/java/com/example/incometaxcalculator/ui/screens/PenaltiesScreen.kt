package com.example.incometaxcalculator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.incometaxcalculator.data.PenaltiesData
import com.example.incometaxcalculator.navigation.Routes

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PenaltiesScreen(nav: NavHostController) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Calculate Penalty") })
    }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(PenaltiesData.penalties) { p ->
                ElevatedCard(onClick = {
                    nav.navigate("penalty_calc/${p.section}")
                }) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Section ${p.section}", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(p.title, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(p.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
