package io.github.jn0v.traceglass.feature.timelapse

import io.github.jn0v.traceglass.core.timelapse.TimelapseSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class RestoreContinueNumberingTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `Restore`() = runTest {
        // Restore: Given 0000-0004 on disk and a restored time-lapse, When I continue,
        // Then new frames are 0005, 0006, ... and nothing is overwritten.
        val dir = File(tempDir, "timelapse").also { it.mkdirs() }
        for (i in 0..4) {
            File(dir, "snapshot_%04d.jpg".format(i)).writeBytes(byteArrayOf(i.toByte(), 9))
        }
        val storage = FileSnapshotStorage(tempDir)
        val session = TimelapseSession(
            intervalMs = 1000L,
            onCapture = { index ->
                storage.saveSnapshot(byteArrayOf(100), index)
                true
            }
        )

        session.restoreFromExisting(storage.getSnapshotCount(), storage.nextIndex())
        session.resume(this)
        advanceTimeBy(100)
        session.stop()

        for (i in 0..4) {
            assertEquals(
                listOf(i.toByte(), 9.toByte()),
                File(dir, "snapshot_%04d.jpg".format(i)).readBytes().toList()
            )
        }
        val created = File(dir, "snapshot_0005.jpg")
        assertTrue(created.exists())
        assertEquals(listOf<Byte>(100), created.readBytes().toList())
    }
}
