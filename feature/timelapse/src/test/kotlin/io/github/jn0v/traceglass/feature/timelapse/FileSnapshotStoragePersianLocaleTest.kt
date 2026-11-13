package io.github.jn0v.traceglass.feature.timelapse

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Locale

class FileSnapshotStoragePersianLocaleTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var storage: FileSnapshotStorage
    private lateinit var originalLocale: Locale

    @BeforeEach
    fun setup() {
        originalLocale = Locale.getDefault()
        storage = FileSnapshotStorage(tempDir)
    }

    @AfterEach
    fun cleanup() {
        Locale.setDefault(originalLocale)
        storage.clear()
    }

    private val persian = Locale.forLanguageTag("fa")

    // Given the language is Persian,
    // When snapshot 1 is saved,
    // Then its file is snapshot_0001.jpg.
    @Test
    fun `snapshot 1 is saved with Latin digits in Persian`() {
        Locale.setDefault(persian)
        val file = storage.saveSnapshot(byteArrayOf(1), 1)
        assertEquals("snapshot_0001.jpg", file.name)
    }

    // Given the language is Persian and snapshots 9999 and 10000 saved,
    // When the files are listed,
    // Then 9999 comes before 10000.
    @Test
    fun `9999 comes before 10000 in Persian`() {
        Locale.setDefault(persian)
        storage.saveSnapshot(byteArrayOf(1), 10000)
        storage.saveSnapshot(byteArrayOf(1), 9999)
        val names = storage.getSnapshotFiles().map { it.name }
        assertEquals(listOf("snapshot_9999.jpg", "snapshot_10000.jpg"), names)
    }

    // Given snapshot 0 saved in English and snapshot 1 in Persian,
    // When the files are listed,
    // Then they come in order 0, 1.
    @Test
    fun `order holds when language changes between snapshots`() {
        Locale.setDefault(Locale.ENGLISH)
        storage.saveSnapshot(byteArrayOf(1), 0)
        Locale.setDefault(persian)
        storage.saveSnapshot(byteArrayOf(1), 1)
        val names = storage.getSnapshotFiles().map { it.name }
        assertEquals(listOf("snapshot_0000.jpg", "snapshot_0001.jpg"), names)
    }
}
