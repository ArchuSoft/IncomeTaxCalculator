
package com.example.incometaxcalculator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.incometaxcalculator.navigation.Routes
import com.example.incometaxcalculator.ui.components.LargeActionButton

@Composable
fun HomeScreen(nav: NavHostController) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("IncomeTaxCalculator", style = MaterialTheme.typography.headlineSmall)
            Text("Quick tools for Indian Income Tax", style = MaterialTheme.typography.bodyMedium)

            LargeActionButton(
                title = "Calculate Income Tax",
                subtitle = "Compute tax, cess, surcharge",
                icon = Icons.Default.Calculate
            ) { nav.navigate(Routes.IncomeTax) }

            LargeActionButton(
                title = "Calculate Penalty",
                subtitle = "List of penalties under IT Act, 1961",
                icon = Icons.Default.AccountBalance
            ) { nav.navigate(Routes.Penalties) }

            LargeActionButton(
                title = "Draft Orders",
                subtitle = "Prepare draft orders & notes",
                icon = Icons.Default.Description
            ) { nav.navigate(Routes.DraftOrders) }

            LargeActionButton(
                title = "Resources",
                subtitle = "Links & references",
                icon = Icons.Default.MenuBook
            ) { nav.navigate(Routes.Resources) }
        }
    }
}
