package com.monitor.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class MonitorViewModelTest {

    private lateinit var viewModel: MonitorViewModel

    // Note: OSHI uses real hardware info, so we can't easily mock it without abstraction.
    // However, we can test that the state is updated and contains valid initial values.

    @BeforeTest
    fun setup() {
        // Just instantiate the real ViewModel.
        // In a strict unit test, we would abstract SystemInfo.
        viewModel = MonitorViewModel()
    }

    @AfterTest
    fun tearDown() {
        viewModel.close()
    }

    @Test
    fun `test state contains valid memory info`() = runTest {
        val state = viewModel.state.value

        // Check initial state validity
        assertTrue(state.memoryTotal > 0, "Memory total should be greater than 0")

        // CPU history should be initialized with 0.0s
        assertTrue(state.cpuHistory.size == 60)
        assertTrue(state.cpuHistory.all { it == 0.0 })
    }

    @Test
    fun `test state updates over time with advanceTimeBy`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        
        // Create fake metrics provider for testing
        val fakeMetricsProvider = object : SystemMetricsProvider {
            private var tickCount = 0L
            override fun getSystemCpuLoadTicks(): LongArray {
                tickCount += 100
                return longArrayOf(tickCount, tickCount, tickCount, tickCount, tickCount, tickCount, tickCount, tickCount)
            }
            override fun getSystemCpuLoadBetweenTicks(prevTicks: LongArray): Double = 0.5 // 50% CPU
            override fun getMemoryTotal(): Long = 16_000_000_000L // 16GB
            override fun getMemoryAvailable(): Long = 8_000_000_000L // 8GB available
        }
        
        val testViewModel = MonitorViewModel(
            metricsProvider = fakeMetricsProvider,
            dispatcher = testDispatcher
        )
        
        try {
            // Initial state check
            val initialState = testViewModel.state.value
            assertEquals(60, initialState.cpuHistory.size)
            assertTrue(initialState.cpuHistory.all { it == 0.0 }, "Initial CPU history should be all zeros")
            assertEquals(0L, initialState.memoryUsed)
            assertEquals(16_000_000_000L, initialState.memoryTotal)
            
            // Advance time by 1 second (1000ms) to trigger first update
            advanceTimeBy(1100)
            runCurrent()
            
            val stateAfterFirstUpdate = testViewModel.state.value
            
            // Verify CPU history has been updated (last value should be 50.0 from our fake 0.5 * 100)
            assertEquals(50.0, stateAfterFirstUpdate.cpuHistory.last(), "CPU should show 50% after first update")
            
            // Verify memory used is updated (16GB - 8GB = 8GB used)
            assertEquals(8_000_000_000L, stateAfterFirstUpdate.memoryUsed)
            
            // Advance time by another second
            advanceTimeBy(1100)
            runCurrent()
            
            val stateAfterSecondUpdate = testViewModel.state.value
            
            // Verify the second-to-last value is also 50.0 (history is shifting)
            assertEquals(50.0, stateAfterSecondUpdate.cpuHistory[58], "Second to last CPU value should be 50%")
            assertEquals(50.0, stateAfterSecondUpdate.cpuHistory.last(), "Last CPU value should also be 50%")
            
        } finally {
            testViewModel.close()
        }
    }
}
