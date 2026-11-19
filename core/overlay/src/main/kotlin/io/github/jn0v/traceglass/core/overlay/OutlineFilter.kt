package io.github.jn0v.traceglass.core.overlay

/** Keeps only the outlines of an ARGB picture: edge pixels become opaque black, the rest transparent. */
object OutlineFilter {
    private const val OPAQUE = 0xFF000000.toInt()
    private const val CLEAR = 0x00000000

    fun filter(pixels: IntArray, width: Int, height: Int, threshold: Int): IntArray {
        require(width > 0 && height > 0) { "width and height must be positive" }
        require(pixels.size == width * height) { "pixels size ${pixels.size} != $width x $height" }
        require(threshold >= 0) { "threshold must not be negative: $threshold" }
        val limit = maxOf(threshold, 1)
        val lum = IntArray(pixels.size) { i ->
            val p = pixels[i]
            (299 * ((p shr 16) and 0xFF) + 587 * ((p shr 8) and 0xFF) + 114 * (p and 0xFF)) / 1000
        }
        val out = IntArray(pixels.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = y * width + x
                val v = lum[i]
                val edge = (x > 0 && Math.abs(v - lum[i - 1]) >= limit) ||
                    (x < width - 1 && Math.abs(v - lum[i + 1]) >= limit) ||
                    (y > 0 && Math.abs(v - lum[i - width]) >= limit) ||
                    (y < height - 1 && Math.abs(v - lum[i + width]) >= limit)
                out[i] = if (edge) OPAQUE else CLEAR
            }
        }
        return out
    }
}
