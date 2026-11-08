package io.github.jn0v.traceglass.feature.timelapse

import io.github.jn0v.traceglass.core.timelapse.TimelapseSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimelapseCountOnlySavedFramesTest {

    @Test
    fun `Background`() = runTest {
        // Background: Given recording, When the app is in the background for 60 s
        // (the camera is stopped, so no frame is saved), Then the counter does not grow.
        var cameraRunning = true
        val session = TimelapseSession(
            intervalMs = 5000L,
            onCapture = { _ -> cameraRunning }
        )
        session.start(this)
        advanceTimeBy(100)
        assertEquals(1, session.snapshotCount.value)
        cameraRunning = false
        advanceTimeBy(60_000)
        assertEquals(1, session.snapshotCount.value)
        session.stop()
    }

    @Test
    fun `No frame`() = runTest {
        // No frame: Given a 5 s interval and no camera frame, When a tick passes,
        // Then nothing is counted.
        val session = TimelapseSession(
            intervalMs = 5000L,
            onCapture = { _ -> false }
        )
        session.start(this)
        advanceTimeBy(20_100)
        assertEquals(0, session.snapshotCount.value)
        session.stop()
    }
}
