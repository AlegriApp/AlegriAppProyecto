package com.example.myapplication

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.core.di.AppModule
import com.example.myapplication.core.navigation.AppNavGraph
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun AlegriApp() {
    val context = LocalContext.current
    val scheduler = AppModule.provideSyncScheduler(context)
    val networkMonitor = AppModule.provideNetworkMonitor(context)

    // 1. Al arrancar: encolar sync inmediata + periódica de respaldo.
    LaunchedEffect(Unit) {
        scheduler.enqueueOneTime()
        scheduler.enqueuePeriodic()
    }

    // 2. Al recuperar conexión (transición false→true): forzar sync inmediata.
    //    WorkManager dedup por UNIQUE_ONE_TIME_NAME, así que múltiples
    //    notificaciones de red no encolan trabajo duplicado.
    LaunchedEffect(Unit) {
        networkMonitor.isOnline
            .distinctUntilChanged()
            .filter { it }
            .collect { scheduler.enqueueOneTime() }
    }

    MyApplicationTheme {
        AppNavGraph()
    }
}
