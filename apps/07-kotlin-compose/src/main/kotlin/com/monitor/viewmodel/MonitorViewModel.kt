package com.monitor.viewmodel

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import oshi.SystemInfo

data class MonitorState(
    val cpuHistory: List<Double> = List(60) { 0.0 }, // 60 seconds history
    val memoryUsed: Long = 0,
    val memoryTotal: Long = 1
)

/**
 * Interface for system metrics to enable testing
 */
interface SystemMetricsProvider {
    fun getSystemCpuLoadTicks(): LongArray
    fun getSystemCpuLoadBetweenTicks(prevTicks: LongArray): Double
    fun getMemoryTotal(): Long
    fun getMemoryAvailable(): Long
}

/**
 * Real implementation using OSHI library
 */
class OshiSystemMetricsProvider : SystemMetricsProvider {
    private val systemInfo = SystemInfo()
    private val processor = systemInfo.hardware.processor
    private val memory = systemInfo.hardware.memory

    override fun getSystemCpuLoadTicks(): LongArray = processor.systemCpuLoadTicks
    override fun getSystemCpuLoadBetweenTicks(prevTicks: LongArray): Double = 
        processor.getSystemCpuLoadBetweenTicks(prevTicks)
    override fun getMemoryTotal(): Long = memory.total
    override fun getMemoryAvailable(): Long = memory.available
}

class MonitorViewModel(
    metricsProvider: SystemMetricsProvider = OshiSystemMetricsProvider(),
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val _metricsProvider = metricsProvider

    private val _state = MutableStateFlow(MonitorState(memoryTotal = _metricsProvider.getMemoryTotal()))
    val state: StateFlow<MonitorState> = _state.asStateFlow()

    private val scope = CoroutineScope(dispatcher)

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        scope.launch {
            var prevTicks = _metricsProvider.getSystemCpuLoadTicks()
            while (isActive) {
                delay(1000)
                val currentTicks = _metricsProvider.getSystemCpuLoadTicks()
                val cpuLoad = _metricsProvider.getSystemCpuLoadBetweenTicks(prevTicks) * 100
                prevTicks = currentTicks

                val memAvailable = _metricsProvider.getMemoryAvailable()
                val memTotal = _metricsProvider.getMemoryTotal()
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
