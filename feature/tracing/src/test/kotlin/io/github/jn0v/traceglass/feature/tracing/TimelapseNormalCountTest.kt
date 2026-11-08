package io.github.jn0v.traceglass.feature.tracing

import androidx.lifecycle.viewModelScope
import io.github.jn0v.traceglass.core.camera.FlashlightController
import io.github.jn0v.traceglass.core.cv.MarkerResult
import io.github.jn0v.traceglass.core.timelapse.CompilationResult
import io.github.jn0v.traceglass.core.timelapse.SnapshotStorage
import io.github.jn0v.traceglass.core.timelapse.TimelapseCompiler
import io.github.jn0v.traceglass.feature.tracing.settings.FakeSettingsRepository
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class TimelapseNormalCountTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class StubFlashlight : FlashlightController {
        private val torch = MutableStateFlow(false)
        override val isTorchOn: StateFlow<Boolean> = torch.asStateFlow()
        override val hasFlashlight: Boolean = true
        override fun toggleTorch() { torch.value = !torch.value }
    }

    private class RecordingStorage : SnapshotStorage {
        val saved = mutableListOf<File>()
        override fun saveSnapshot(jpegData: ByteArray, index: Int): File {
            val f = File("/fake/snap_$index.jpg")
            saved.add(f)
            return f
        }
        override fun getSnapshotFiles(): List<File> = saved.toList()
        override fun getSnapshotCount(): Int = saved.size
        override fun clear() { saved.clear() }
    }

    private class NoopCompiler : TimelapseCompiler {
        override suspend fun compile(
            snapshotFiles: List<File>,
            outputFile: File,
            fps: Int,
            onProgress: (Float) -> Unit
        ): CompilationResult = CompilationResult.Error("unused")
    }

    private val analyzer = FrameAnalyzer(markerDetector = object : io.github.jn0v.traceglass.core.cv.MarkerDetector {
        override fun detect(
            frameBuffer: java.nio.ByteBuffer, width: Int, height: Int,
            rowStride: Int, rotation: Int
        ) = MarkerResult(emptyList(), 0L)
    })

    @Test
    fun `Normal`() {
        // Normal: Given frames arriving, When I record 3 ticks, Then the counter shows 3
        // and the disk holds 3 files.
        val storage = RecordingStorage()
        val vm = TracingViewModel(
            flashlightController = StubFlashlight(),
            sessionRepository = FakeSessionRepository(),
            settingsRepository = FakeSettingsRepository(),
            frameAnalyzer = analyzer,
            timelapseOps = TimelapseOperations(
                snapshotStorage = storage,
                compiler = NoopCompiler(),
                exporter = mockk(relaxed = true),
                sharer = mockk(relaxed = true),
                cacheDir = File("/fake/cache")
            ),
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher
        )
        val scheduler = testDispatcher.scheduler

        vm.startTimelapse()
        scheduler.runCurrent()
        analyzer.snapshotCallback?.invoke(byteArrayOf(1))
        scheduler.runCurrent()

        scheduler.advanceTimeBy(5000)
        scheduler.runCurrent()
        analyzer.snapshotCallback?.invoke(byteArrayOf(1))
        scheduler.runCurrent()

        scheduler.advanceTimeBy(5000)
        scheduler.runCurrent()
        // third tick has started, its frame has not arrived yet: not counted
        assertEquals(2, vm.uiState.value.snapshotCount)
        analyzer.snapshotCallback?.invoke(byteArrayOf(1))
        scheduler.runCurrent()

        assertEquals(3, vm.uiState.value.snapshotCount)
        assertEquals(3, storage.getSnapshotFiles().size)
        vm.viewModelScope.cancel()
    }
}
