package io.github.jn0v.traceglass.core.timelapse

import java.io.File

interface SnapshotStorage {
    fun saveSnapshot(jpegData: ByteArray, index: Int): File
    fun getSnapshotFiles(): List<File>
    fun getSnapshotCount(): Int
    /** Index for the next new snapshot: highest numbered file on disk plus 1, or 0. */
    fun nextIndex(): Int
    fun clear()
}
