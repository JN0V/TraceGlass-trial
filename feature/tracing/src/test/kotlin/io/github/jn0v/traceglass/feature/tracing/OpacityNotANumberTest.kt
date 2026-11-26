package io.github.jn0v.traceglass.feature.tracing

import androidx.lifecycle.viewModelScope
import io.github.jn0v.traceglass.core.camera.FlashlightController
import io.github.jn0v.traceglass.core.overlay.OverlayTransformCalculator
import io.github.jn0v.traceglass.feature.tracing.settings.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OpacityNotANumberTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): TracingViewModel {
        val flashlight = object : FlashlightController {
            override val hasFlashlight: Boolean = false
            override val isTorchOn: StateFlow<Boolean> = MutableStateFlow(false)
            override fun toggleTorch() {}
        }
        return TracingViewModel(
            flashlightController = flashlight,
            sessionRepository = FakeSessionRepository(),
            settingsRepository = FakeSettingsRepository(),
            transformCalculator = OverlayTransformCalculator(smoothingFactor = 1f),
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher
        )
    }

    // Given the overlay opacity is 0.5, When the opacity changes to NaN,
    // Then the overlay opacity stays 0.5.
    @Test
    fun `an opacity that is not a number keeps the last one`() {
        val viewModel = createViewModel()
        viewModel.onOpacityChanged(0.5f)
        assertEquals(0.5f, viewModel.uiState.value.overlayOpacity)

        viewModel.onOpacityChanged(Float.NaN)

        assertEquals(0.5f, viewModel.uiState.value.overlayOpacity)
        viewModel.viewModelScope.cancel()
    }
}
