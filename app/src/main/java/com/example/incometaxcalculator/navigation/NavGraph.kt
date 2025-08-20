package com.example.incometaxcalculator.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.incometaxcalculator.ui.screens.*

object Routes {
    const val Home = "home"
    const val IncomeTax = "income_tax"
    const val Penalties = "penalties"
    const val PenaltyCalc = "penalty_calc/{section}"
    const val DraftOrders = "draft_orders"
    const val Resources = "resources"
}

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController, startDestination = Routes.Home) {
        composable(Routes.Home) { HomeScreen(navController) }
        composable(Routes.IncomeTax) { IncomeTaxScreen() }
        composable(Routes.Penalties) { PenaltiesScreen(navController) }
        composable(Routes.DraftOrders) { DraftOrdersScreen() }
        composable(Routes.Resources) { ResourcesScreen() }
        composable(Routes.PenaltyCalc) { backStackEntry ->
            val section = backStackEntry.arguments?.getString("section") ?: ""
            PenaltyCalculatorScreen(sectionCode = section)
        }
    }
}
