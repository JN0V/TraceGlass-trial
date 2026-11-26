package io.github.jn0v.traceglass.feature.timelapse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class FileSnapshotStorageNegativeIndexTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var storage: FileSnapshotStorage

    @BeforeEach
    fun setup() {
        storage = FileSnapshotStorage(tempDir)
    }

    // Given an empty snapshot storage,
    // When a snapshot is saved with the number -1,
    // Then it is refused with an IllegalArgumentException naming -1,
    // And no file is written, and the count stays 0.
    @Test
    fun `a negative number is refused`() {
        assertEquals(0, storage.getSnapshotCount())
        val e = assertThrows(IllegalArgumentException::class.java) {
            storage.saveSnapshot(byteArrayOf(1), -1)
        }
        assertTrue(e.message?.contains("-1") == true, "message was: ${e.message}")
        val written = File(tempDir, "timelapse").listFiles() ?: emptyArray()
        assertEquals(0, written.size)
        assertEquals(0, storage.getSnapshotCount())
    }

    // Given an empty snapshot storage,
    // When a snapshot is saved with the number 0,
    // Then snapshot_0000.jpg is written, listed and counted once.
    @Test
    fun `a snapshot numbered 0 is still saved`() {
        assertEquals(0, storage.getSnapshotCount())
        val file = storage.saveSnapshot(byteArrayOf(1), 0)
        assertEquals("snapshot_0000.jpg", file.name)
        assertTrue(file.exists())
        assertEquals(listOf("snapshot_0000.jpg"), storage.getSnapshotFiles().map { it.name })
        assertEquals(1, storage.getSnapshotCount())
    }
}
