package com.example.myapplication

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.core.di.AppModule
import com.example.myapplication.core.navigation.AppNavGraph
import com.example.myapplication.ui.theme.MyApplicationTheme

@Composable
fun AlegriApp() {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        AppModule.provideSyncRepository(context).syncAll()
    }

    MyApplicationTheme {
        AppNavGraph()
    }
}
