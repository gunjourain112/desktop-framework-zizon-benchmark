package com.monitor.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import oshi.SystemInfo
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
    fun `test close cancels coroutine scope and stops monitoring`() = runTest {
        // Create a separate ViewModel for this test
        val testViewModel = MonitorViewModel()
        
        // Get the initial state
        val initialState = testViewModel.state.value
        
        // Call close to cancel the coroutine scope
        testViewModel.close()
        
        // Wait a bit longer than the monitoring interval (1 second)
        delay(1500)
        
        // Get the state after close and delay
        val stateAfterClose = testViewModel.state.value
        
        // After close(), the monitoring loop should stop. The state should remain 
        // the same because the monitoring coroutine is cancelled and not updating.
        // We verify by comparing cpuHistory - if monitoring continued, cpuHistory would change
        assertEquals(initialState.cpuHistory, stateAfterClose.cpuHistory,
            "CPU history should not change after close() as monitoring should stop")
    }
}
