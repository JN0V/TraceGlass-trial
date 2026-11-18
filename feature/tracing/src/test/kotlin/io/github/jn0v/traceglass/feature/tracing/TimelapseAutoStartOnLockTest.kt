package io.github.jn0v.traceglass.feature.tracing

import androidx.lifecycle.viewModelScope
import io.github.jn0v.traceglass.core.camera.FlashlightController
import io.github.jn0v.traceglass.core.cv.MarkerResult
import io.github.jn0v.traceglass.core.overlay.OverlayTransformCalculator
import io.github.jn0v.traceglass.feature.tracing.settings.FakeSettingsRepository
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class TimelapseAutoStartOnLockTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val analyzer = FrameAnalyzer(markerDetector = object : io.github.jn0v.traceglass.core.cv.MarkerDetector {
        override fun detect(
            frameBuffer: java.nio.ByteBuffer, width: Int, height: Int,
            rowStride: Int, rotation: Int
        ) = MarkerResult(emptyList(), 0L)
    })

    private fun createViewModel(settings: FakeSettingsRepository): TracingViewModel {
        val flashlight = object : FlashlightController {
            override val hasFlashlight: Boolean = false
            override val isTorchOn: StateFlow<Boolean> = MutableStateFlow(false)
            override fun toggleTorch() {}
        }
        return TracingViewModel(
            flashlightController = flashlight,
            sessionRepository = FakeSessionRepository(),
            settingsRepository = settings,
            transformCalculator = OverlayTransformCalculator(smoothingFactor = 1f),
            frameAnalyzer = analyzer,
            timelapseOps = TimelapseOperations(
                snapshotStorage = mockk(relaxed = true),
                compiler = mockk(relaxed = true),
                exporter = mockk(relaxed = true),
                sharer = mockk(relaxed = true),
                cacheDir = File(System.getProperty("java.io.tmpdir"))
            ),
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher
        )
    }

    // Given auto-start on and no recording, When I lock the overlay, Then the time-lapse starts.
    @Test
    fun `lock starts the timelapse when auto-start is on`() = runTest(testDispatcher) {
        val settings = FakeSettingsRepository().apply { setAutoStartTimelapse(true) }
        val vm = createViewModel(settings)
        testDispatcher.scheduler.runCurrent()

        vm.onToggleLock()
        testDispatcher.scheduler.runCurrent()

        assertTrue(vm.uiState.value.isTimelapseRecording)
        vm.viewModelScope.cancel()
    }

    // Given a paused time-lapse, When I lock the overlay, Then it stays paused.
    @Test
    fun `lock keeps a paused timelapse paused`() = runTest(testDispatcher) {
        val settings = FakeSettingsRepository().apply { setAutoStartTimelapse(true) }
        val vm = createViewModel(settings)
        testDispatcher.scheduler.runCurrent()
        vm.startTimelapse()
        testDispatcher.scheduler.runCurrent()
        vm.pauseTimelapse()
        testDispatcher.scheduler.runCurrent()
        assertTrue(vm.uiState.value.isTimelapsePaused)

        vm.onToggleLock()
        testDispatcher.scheduler.runCurrent()

        assertTrue(vm.uiState.value.isOverlayLocked)
        assertTrue(vm.uiState.value.isTimelapsePaused)
        assertFalse(vm.uiState.value.isTimelapseRecording)
        vm.viewModelScope.cancel()
    }

    // Given auto-start off, When I lock the overlay, Then no time-lapse starts.
    @Test
    fun `lock does not start a timelapse when auto-start is off`() = runTest(testDispatcher) {
        val settings = FakeSettingsRepository().apply { setAutoStartTimelapse(false) }
        val vm = createViewModel(settings)
        testDispatcher.scheduler.runCurrent()

        vm.onToggleLock()
        testDispatcher.scheduler.runCurrent()

        assertTrue(vm.uiState.value.isOverlayLocked)
        assertFalse(vm.uiState.value.isTimelapseRecording)
        assertFalse(vm.uiState.value.isTimelapsePaused)
        vm.viewModelScope.cancel()
    }
}
