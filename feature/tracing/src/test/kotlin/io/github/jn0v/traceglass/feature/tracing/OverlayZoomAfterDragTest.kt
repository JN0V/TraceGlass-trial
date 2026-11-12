package io.github.jn0v.traceglass.feature.tracing

import androidx.compose.ui.geometry.Offset
import io.github.jn0v.traceglass.core.camera.FlashlightController
import io.github.jn0v.traceglass.feature.tracing.settings.FakeSettingsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OverlayZoomAfterDragTest {

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
        val flashlight = mockk<FlashlightController>(relaxed = true)
        every { flashlight.hasFlashlight } returns true
        every { flashlight.isTorchOn } returns MutableStateFlow(false)
        return TracingViewModel(
            flashlightController = flashlight,
            sessionRepository = FakeSessionRepository(),
            settingsRepository = FakeSettingsRepository(),
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher
        )
    }

    // Given an offset of (300, 0), When I zoom 2x at (840, 960) (view 1080x1920),
    // Then the offset is still (300, 0): the point under the fingers is the view center
    // shifted by the offset, so it is the zoom pivot and nothing moves.
    @Test
    fun `zoom after a drag keeps the offset`() {
        val viewModel = createViewModel()
        viewModel.onOverlayDrag(Offset(300f, 0f))
        viewModel.onOverlayGesture(Offset(840f, 960f), Offset.Zero, 2f, 0f)
        assertEquals(300f, viewModel.uiState.value.overlayOffset.x, 0.1f)
        assertEquals(0f, viewModel.uiState.value.overlayOffset.y, 0.1f)
        assertEquals(2f, viewModel.uiState.value.overlayScale, 0.001f)
    }
}
