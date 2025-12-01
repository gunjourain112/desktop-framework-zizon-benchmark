package com.monitor.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import oshi.SystemInfo
import oshi.hardware.CentralProcessor

data class MonitorState(
    val cpuHistory: List<Double> = List(60) { 0.0 }, // 60 seconds history
    val memoryUsed: Long = 0,
    val memoryTotal: Long = 1
)

class MonitorViewModel {
    private val systemInfo = SystemInfo()
    private val processor: CentralProcessor = systemInfo.hardware.processor
    private val memory = systemInfo.hardware.memory

    private val _state = MutableStateFlow(MonitorState(memoryTotal = memory.total))
    val state: StateFlow<MonitorState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        scope.launch {
            var prevTicks = processor.systemCpuLoadTicks
            while (true) {
                delay(1000)
                val currentTicks = processor.systemCpuLoadTicks
                val cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100
                prevTicks = currentTicks

                val memAvailable = memory.available
                val memTotal = memory.total
                val memUsed = memTotal - memAvailable

                _state.update { currentState ->
                    currentState.copy(
                        cpuHistory = (currentState.cpuHistory.drop(1) + cpuLoad),
                        memoryUsed = memUsed,
                        memoryTotal = memTotal
                    )
                }
            }
        }
    }

    fun close() {
        scope.cancel()
    }
}
