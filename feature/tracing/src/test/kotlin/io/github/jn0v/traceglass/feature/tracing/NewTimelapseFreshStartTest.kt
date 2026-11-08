package io.github.jn0v.traceglass.feature.tracing

import androidx.lifecycle.viewModelScope
import io.github.jn0v.traceglass.core.camera.FlashlightController
import io.github.jn0v.traceglass.core.cv.MarkerResult
import io.github.jn0v.traceglass.core.overlay.OverlayTransformCalculator
import io.github.jn0v.traceglass.core.timelapse.CompilationResult
import io.github.jn0v.traceglass.core.timelapse.SnapshotStorage
import io.github.jn0v.traceglass.core.timelapse.TimelapseCompiler
import io.github.jn0v.traceglass.feature.tracing.settings.FakeSettingsRepository
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class NewTimelapseFreshStartTest {

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

    private class DiskStorage : SnapshotStorage {
        val files = mutableListOf<File>()
        override fun saveSnapshot(jpegData: ByteArray, index: Int): File = File("/fake/snap_$index.jpg")
        override fun getSnapshotFiles(): List<File> = files.toList()
        override fun getSnapshotCount(): Int = files.size
        override fun clear() { files.clear() }
    }

    private class OutcomeCompiler(private val succeed: Boolean) : TimelapseCompiler {
        override suspend fun compile(
            snapshotFiles: List<File>,
            outputFile: File,
            fps: Int,
            onProgress: (Float) -> Unit
        ): CompilationResult {
            if (!succeed) return CompilationResult.Error("Encoder failed")
            outputFile.parentFile?.mkdirs()
            outputFile.writeBytes(byteArrayOf(0))
            return CompilationResult.Success(outputFile)
        }
    }

    private val analyzer = FrameAnalyzer(markerDetector = object : io.github.jn0v.traceglass.core.cv.MarkerDetector {
        override fun detect(
            frameBuffer: java.nio.ByteBuffer, width: Int, height: Int,
            rowStride: Int, rotation: Int
        ) = MarkerResult(emptyList(), 0L)
    })

    private fun createViewModel(storage: DiskStorage, compiler: TimelapseCompiler, cacheDir: File) =
        TracingViewModel(
            flashlightController = StubFlashlight(),
            sessionRepository = FakeSessionRepository(),
            settingsRepository = FakeSettingsRepository(),
            transformCalculator = OverlayTransformCalculator(smoothingFactor = 1f),
            frameAnalyzer = analyzer,
            timelapseOps = TimelapseOperations(
                snapshotStorage = storage,
                compiler = compiler,
                exporter = mockk(relaxed = true),
                sharer = mockk(relaxed = true),
                cacheDir = cacheDir
            ),
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher
        )

    private fun recordAndStop(vm: TracingViewModel, storage: DiskStorage) {
        vm.startTimelapse()
        testDispatcher.scheduler.runCurrent()
        storage.files.addAll(listOf(File("/fake/old_0.jpg"), File("/fake/old_1.jpg"), File("/fake/old_2.jpg")))
        vm.stopTimelapse()
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `Dismissed dialog`() {
        // Dismissed dialog: Given a compiled time-lapse whose dialog was dismissed, When I record a new
        // 1-frame time-lapse, Then its video holds 1 frame.
        val storage = DiskStorage()
        val cacheDir = kotlin.io.path.createTempDirectory("test_cache").toFile()
        val vm = createViewModel(storage, OutcomeCompiler(true), cacheDir)
        recordAndStop(vm, storage)
        assertTrue(vm.uiState.value.showPostCompilationDialog)
        vm.onDismissPostCompilationDialog()

        vm.startTimelapse()
        testDispatcher.scheduler.runCurrent()

        assertTrue(storage.getSnapshotFiles().isEmpty(), "Old frames must be gone before the new recording")
        cacheDir.deleteRecursively()
        vm.viewModelScope.cancel()
    }

    @Test
    fun `Compile error`() {
        // Compile error: Given a compilation that failed, When I start a new time-lapse,
        // Then the old frames are not in the new video.
        val storage = DiskStorage()
        val cacheDir = kotlin.io.path.createTempDirectory("test_cache").toFile()
        val vm = createViewModel(storage, OutcomeCompiler(false), cacheDir)
        recordAndStop(vm, storage)
        assertTrue(vm.uiState.value.compilationError != null)

        vm.startTimelapse()
        testDispatcher.scheduler.runCurrent()

        assertTrue(storage.getSnapshotFiles().isEmpty(), "Old frames must be gone before the new recording")
        cacheDir.deleteRecursively()
        vm.viewModelScope.cancel()
    }
}
