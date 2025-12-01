package com.monitor

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.monitor.ui.MainScreen
import com.monitor.viewmodel.MonitorViewModel

fun main() = application {
    val viewModel = MonitorViewModel()

    Window(
        onCloseRequest = {
            viewModel.close()
            exitApplication()
        },
        title = "System Monitor (Kotlin)",
        state = WindowState(size = DpSize(1280.dp, 800.dp))
    ) {
        MainScreen(viewModel)
    }
}
