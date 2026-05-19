package com.example.myapplication

import androidx.compose.runtime.Composable
import com.example.myapplication.core.navigation.AppNavGraph
import com.example.myapplication.ui.theme.MyApplicationTheme

@Composable
fun AlegriApp() {
    MyApplicationTheme {
        AppNavGraph()
    }
}
