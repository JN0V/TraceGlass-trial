package io.github.jn0v.traceglass.feature.tracing

import androidx.lifecycle.viewModelScope
import io.github.jn0v.traceglass.core.camera.FlashlightController
import io.github.jn0v.traceglass.core.cv.MarkerResult
import io.github.jn0v.traceglass.core.overlay.OverlayTransformCalculator
import io.github.jn0v.traceglass.core.session.SettingsData
import io.github.jn0v.traceglass.core.session.SettingsRepository
import io.github.jn0v.traceglass.core.timelapse.CompilationResult
import io.github.jn0v.traceglass.core.timelapse.SnapshotStorage
import io.github.jn0v.traceglass.core.timelapse.TimelapseCompiler
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
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
class TimelapseChoiceTest {

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
        settings: SettingsData,
        storage: ChoiceSnapshotStorage,
        compiler: ChoiceCompiler,
        cacheDir: File
    ): TracingViewModel {
        val flashlight = object : FlashlightController {
            override val hasFlashlight: Boolean = false
            override val isTorchOn: StateFlow<Boolean> = MutableStateFlow(false)
            override fun toggleTorch() {}
        }
        val settingsRepo = mockk<SettingsRepository>(relaxed = true)
        every { settingsRepo.settingsData } returns flowOf(settings)
        return TracingViewModel(
            flashlightController = flashlight,
            sessionRepository = FakeSessionRepository(),
            settingsRepository = settingsRepo,
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

    // Given the interval set to 15 s, When I record, Then snapshots are taken at 0 s, 15 s and 30 s
    @Test
    fun `interval set to 15 seconds takes snapshots at 0, 15 and 30 seconds`() = runTest(testDispatcher) {
        val cacheDir = kotlin.io.path.createTempDirectory("test_cache").toFile()
        val vm = createViewModel(
            SettingsData(timelapseIntervalSeconds = 15), ChoiceSnapshotStorage(), ChoiceCompiler(), cacheDir
        )
        testDispatcher.scheduler.runCurrent()

        vm.startTimelapse()
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, vm.uiState.value.snapshotCount)

        testDispatcher.scheduler.advanceTimeBy(14_999L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, vm.uiState.value.snapshotCount)

        testDispatcher.scheduler.advanceTimeBy(1L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(2, vm.uiState.value.snapshotCount)

        testDispatcher.scheduler.advanceTimeBy(14_999L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(2, vm.uiState.value.snapshotCount)

        testDispatcher.scheduler.advanceTimeBy(1L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(3, vm.uiState.value.snapshotCount)

        vm.stopTimelapse()
        cacheDir.deleteRecursively()
        vm.viewModelScope.cancel()
    }

    // Given 30 fps set, When I stop recording, Then the video is made at 30 fps
    @Test
    fun `speed set to 30 fps compiles the video at 30 fps`() = runTest(testDispatcher) {
        val storage = ChoiceSnapshotStorage()
        val compiler = ChoiceCompiler()
        val cacheDir = kotlin.io.path.createTempDirectory("test_cache").toFile()
        val vm = createViewModel(SettingsData(timelapseFps = 30), storage, compiler, cacheDir)
        testDispatcher.scheduler.runCurrent()

        vm.startTimelapse()
        testDispatcher.scheduler.runCurrent()
        analyzer.snapshotCallback?.invoke(byteArrayOf(1, 2, 3))
        vm.stopTimelapse()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, compiler.compileCalls)
        assertEquals(30, compiler.lastFps)
        cacheDir.deleteRecursively()
        vm.viewModelScope.cancel()
    }
}

private class ChoiceSnapshotStorage : SnapshotStorage {
    private val files = sortedMapOf<Int, File>()

    override fun saveSnapshot(jpegData: ByteArray, index: Int): File {
        val file = File("/fake/choice_%04d.jpg".format(index))
        files[index] = file
        return file
    }

    override fun getSnapshotFiles(): List<File> = files.values.toList()
    override fun getSnapshotCount(): Int = files.size
    override fun clear() {
        files.clear()
    }
}

private class ChoiceCompiler : TimelapseCompiler {
    var compileCalls = 0
    var lastFps = 0

    override suspend fun compile(
        snapshotFiles: List<File>,
        outputFile: File,
        fps: Int,
        onProgress: (Float) -> Unit
    ): CompilationResult {
        compileCalls++
        lastFps = fps
        return CompilationResult.Success(outputFile)
    }
}
