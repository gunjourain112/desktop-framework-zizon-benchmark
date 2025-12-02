package com.monitor.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import oshi.SystemInfo
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
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
}
