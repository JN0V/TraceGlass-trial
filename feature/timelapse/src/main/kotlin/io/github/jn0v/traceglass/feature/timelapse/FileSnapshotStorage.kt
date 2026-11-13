package io.github.jn0v.traceglass.feature.timelapse

import io.github.jn0v.traceglass.core.timelapse.SnapshotStorage
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class FileSnapshotStorage(
    private val baseDir: File
) : SnapshotStorage {

    private val timelapseDir: File by lazy {
        File(baseDir, "timelapse").also { it.mkdirs() }
    }

    private val cachedCount = AtomicInteger(-1)

    override fun saveSnapshot(jpegData: ByteArray, index: Int): File {
        val file = File(timelapseDir, "snapshot_%04d.jpg".format(java.util.Locale.ROOT, index))
        val existed = file.exists()
        file.writeBytes(jpegData)
        if (!existed) cachedCount.updateAndGet { c -> if (c >= 0) c + 1 else c }
        return file
    }

    override fun getSnapshotFiles(): List<File> {
        val files = timelapseDir
            .listFiles { f -> f.extension == "jpg" }
            ?.sortedWith(
                compareBy<File> { snapshotNumber(it) == null }
                    .thenBy(nullsLast()) { snapshotNumber(it) }
                    .thenBy { it.name }
            )
            ?: emptyList()
        cachedCount.set(files.size)
        return files
    }

    private fun snapshotNumber(file: File): java.math.BigInteger? =
        SNAPSHOT_NAME.matchEntire(file.name)?.groupValues?.get(1)?.toBigInteger()

    private companion object {
        val SNAPSHOT_NAME = Regex("snapshot_(\\p{Nd}+)\\.jpg")
    }

    override fun getSnapshotCount(): Int {
        val c = cachedCount.get()
        if (c >= 0) return c
        return getSnapshotFiles().size
    }

    override fun clear() {
        timelapseDir.listFiles()?.forEach { it.delete() }
        cachedCount.set(0)
    }
}
