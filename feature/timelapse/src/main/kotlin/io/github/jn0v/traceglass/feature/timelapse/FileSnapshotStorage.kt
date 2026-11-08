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
        val file = File(timelapseDir, "snapshot_%04d.jpg".format(index))
        if (!file.createNewFile()) {
            throw java.io.IOException("Snapshot already exists: ${file.name}")
        }
        file.writeBytes(jpegData)
        cachedCount.updateAndGet { c -> if (c >= 0) c + 1 else c }
        return file
    }

    override fun getSnapshotFiles(): List<File> {
        val files = timelapseDir
            .listFiles { f -> f.extension == "jpg" }
            ?.sorted()
            ?: emptyList()
        cachedCount.set(files.size)
        return files
    }

    override fun getSnapshotCount(): Int {
        val c = cachedCount.get()
        if (c >= 0) return c
        return getSnapshotFiles().size
    }

    override fun nextIndex(): Int {
        val max = timelapseDir.listFiles()
            ?.mapNotNull { NAME_REGEX.matchEntire(it.name)?.groupValues?.get(1)?.toIntOrNull() }
            ?.maxOrNull()
        return if (max == null) 0 else max + 1
    }

    override fun clear() {
        timelapseDir.listFiles()?.forEach { it.delete() }
        cachedCount.set(0)
    }

    private companion object {
        val NAME_REGEX = Regex("""snapshot_(\d{4,})\.jpg""")
    }
}
