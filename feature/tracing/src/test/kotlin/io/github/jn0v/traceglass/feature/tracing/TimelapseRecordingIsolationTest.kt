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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class TimelapseRecordingIsolationTest {

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

    private fun createViewModel(
        storage: RecordingSnapshotStorage,
        compiler: RecordingCompiler,
        cacheDir: File
    ): TracingViewModel {
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
    }

    // Delivers one camera frame to the callback the session armed for its current capture.
    private fun takeSnapshot() {
        analyzer.snapshotCallback?.invoke(byteArrayOf(1, 2, 3))
    }

    private fun nextCapture() {
        testDispatcher.scheduler.advanceTimeBy(5000L)
        testDispatcher.scheduler.runCurrent()
    }

    // Given a time-lapse stopped with 3 snapshots and its "video ready" dialog closed by tapping outside it,
    // When I start a new time-lapse, take 1 snapshot and stop it,
    // Then the new video holds that 1 frame only.
    @Test
    fun `dialog dismissed then new timelapse video holds only new frame`() = runTest(testDispatcher) {
        val storage = RecordingSnapshotStorage()
        val compiler = RecordingCompiler()
        val cacheDir = kotlin.io.path.createTempDirectory("test_cache").toFile()
        val vm = createViewModel(storage, compiler, cacheDir)

        vm.startTimelapse()
        testDispatcher.scheduler.runCurrent()
        takeSnapshot()
        nextCapture()
        takeSnapshot()
        nextCapture()
        takeSnapshot()
        vm.stopTimelapse()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(3, compiler.lastFrameCount)
        assertEquals(true, vm.uiState.value.showPostCompilationDialog)

        vm.onDismissPostCompilationDialog()

        vm.startTimelapse()
        testDispatcher.scheduler.runCurrent()
        takeSnapshot()
        vm.stopTimelapse()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, compiler.compileCalls)
        assertEquals(1, compiler.lastFrameCount)
        cacheDir.deleteRecursively()
        vm.viewModelScope.cancel()
    }

    // Given a time-lapse whose video failed to compile,
    // When I start a new time-lapse and stop it,
    // Then its video holds only the new frames.
    @Test
    fun `failed compile then new timelapse video holds only new frames`() = runTest(testDispatcher) {
        val storage = RecordingSnapshotStorage()
        val compiler = RecordingCompiler().apply { shouldSucceed = false }
        val cacheDir = kotlin.io.path.createTempDirectory("test_cache").toFile()
        val vm = createViewModel(storage, compiler, cacheDir)

        vm.startTimelapse()
        testDispatcher.scheduler.runCurrent()
        takeSnapshot()
        nextCapture()
        takeSnapshot()
        vm.stopTimelapse()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, compiler.lastFrameCount)

        compiler.shouldSucceed = true
        vm.startTimelapse()
        testDispatcher.scheduler.runCurrent()
        takeSnapshot()
        vm.stopTimelapse()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, compiler.compileCalls)
        assertEquals(1, compiler.lastFrameCount)
        cacheDir.deleteRecursively()
        vm.viewModelScope.cancel()
    }
}

private class RecordingSnapshotStorage : SnapshotStorage {
    private val files = sortedMapOf<Int, File>()

    override fun saveSnapshot(jpegData: ByteArray, index: Int): File {
        val file = File("/fake/snap_%04d.jpg".format(index))
        files[index] = file
        return file
    }

    override fun getSnapshotFiles(): List<File> = files.values.toList()
    override fun getSnapshotCount(): Int = files.size
    override fun clear() {
        files.clear()
    }
}

private class RecordingCompiler : TimelapseCompiler {
    var shouldSucceed = true
    var compileCalls = 0
    var lastFrameCount = 0

    override suspend fun compile(
        snapshotFiles: List<File>,
        outputFile: File,
        fps: Int,
        onProgress: (Float) -> Unit
    ): CompilationResult {
        compileCalls++
        lastFrameCount = snapshotFiles.size
        return if (shouldSucceed) CompilationResult.Success(outputFile)
        else CompilationResult.Error("compile failed")
    }
}
