package io.github.jn0v.traceglass.feature.tracing.settings

import io.github.jn0v.traceglass.core.session.GridMode
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
class SettingsGridViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repo: FakeSettingsRepository
    private lateinit var vm: SettingsViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repo = FakeSettingsRepository()
        vm = SettingsViewModel(repo)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Given the stored grid mode is Off, When the Settings state is built, Then the Grid choice reads Off
    @Test
    fun `grid choice reads Off by default`() {
        assertEquals(GridMode.OFF, vm.uiState.value.gridMode)
    }

    // Given the Settings screen, When a grid mode is chosen, Then the handler saves it
    @Test
    fun `grid changed handler saves the chosen mode`() {
        vm.onGridChanged(GridMode.GOLDEN_RATIO)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(GridMode.GOLDEN_RATIO, vm.uiState.value.gridMode)
    }
}
