package io.github.jn0v.traceglass.feature.timelapse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class FileSnapshotStoragePastNineThousandTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var storage: FileSnapshotStorage

    @BeforeEach
    fun setup() {
        storage = FileSnapshotStorage(tempDir)
    }

    // Past 9999: Given snapshots 9998, 9999 and 10000 on disk, When the frames are listed,
    // Then they come back in the order 9998, 9999, 10000.
    @Test
    fun `past 9999 frames are listed in numeric order`() {
        storage.saveSnapshot(byteArrayOf(1), 9999)
        storage.saveSnapshot(byteArrayOf(2), 10000)
        storage.saveSnapshot(byteArrayOf(3), 9998)
        val names = storage.getSnapshotFiles().map { it.name }
        assertEquals(
            listOf("snapshot_9998.jpg", "snapshot_9999.jpg", "snapshot_10000.jpg"),
            names
        )
    }
}
