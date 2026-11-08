package io.github.jn0v.traceglass.feature.tracing.settings

import io.github.jn0v.traceglass.core.overlay.CompositionGrid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GridSettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Off by default: Given a fresh install, Then no grid is shown.
    @Test
    fun offByDefault() {
        val vm = SettingsViewModel(FakeSettingsRepository())
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(CompositionGrid.Off, vm.uiState.value.grid)
    }
}
