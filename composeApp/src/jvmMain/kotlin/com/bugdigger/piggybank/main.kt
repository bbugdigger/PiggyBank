package com.bugdigger.piggybank

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.bugdigger.piggybank.di.appModule
import org.koin.core.context.startKoin

fun main() {
    // Initialize Koin
    startKoin {
        modules(appModule)
    }
    
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "PiggyBank",
            state = rememberWindowState(
                width = 1200.dp,
                height = 800.dp
            )
        ) {
            App()
        }
    }
}
