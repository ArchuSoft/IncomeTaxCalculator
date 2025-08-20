package com.example.incometaxcalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.incometaxcalculator.navigation.AppNavGraph
import com.example.incometaxcalculator.ui.theme.IncomeTaxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IncomeTaxTheme {
                AppNavGraph()
            }
        }
    }
}
