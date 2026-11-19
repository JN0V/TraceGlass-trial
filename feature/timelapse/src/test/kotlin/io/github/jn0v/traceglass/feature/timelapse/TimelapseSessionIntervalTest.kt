package io.github.jn0v.traceglass.feature.timelapse

import io.github.jn0v.traceglass.core.timelapse.TimelapseSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimelapseSessionIntervalTest {

    // Virtual times at which the session captured while recording for `recordMs`.
    // A session that does not pause between snapshots would loop forever without
    // advancing time, so the capture callback parks itself after a bounded number
    // of captures to let the test fail by assertion instead of hanging.
    private fun TestScope.captureTimes(intervalMs: Long, recordMs: Long): List<Long> {
        val times = mutableListOf<Long>()
        val session = TimelapseSession(
            intervalMs = intervalMs,
            onCapture = {
                times.add(currentTime)
                if (times.size >= 10) awaitCancellation()
            }
        )
        try {
            session.start(this)
            advanceTimeBy(recordMs)
        } finally {
            session.stop()
        }
        return times
    }

    // Given a time-lapse session with an interval of 0 ms,
    // When it records for 3 seconds,
    // Then it takes the snapshots 1 second apart.
    @Test
    fun `zero interval takes snapshots one second apart`() = runTest {
        val times = captureTimes(intervalMs = 0L, recordMs = 3100L)
        assertEquals(listOf(0L, 1000L, 2000L, 3000L), times)
    }

    // Given a session with an interval of -500 ms,
    // When it records for 3 seconds,
    // Then it takes the snapshots 1 second apart.
    @Test
    fun `negative interval takes snapshots one second apart`() = runTest {
        val times = captureTimes(intervalMs = -500L, recordMs = 3100L)
        assertEquals(listOf(0L, 1000L, 2000L, 3000L), times)
    }
}
