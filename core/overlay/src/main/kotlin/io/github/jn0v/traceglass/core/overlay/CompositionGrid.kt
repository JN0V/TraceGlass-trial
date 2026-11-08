package io.github.jn0v.traceglass.core.overlay

/**
 * Composition grid drawn over the reference image. Pure Kotlin.
 * Line positions are fractions of a side; [mappedPoints] runs line endpoints
 * through the same 3x3 matrix (row-major) as the image.
 */
enum class CompositionGrid(private val fractions: List<Float>) {
    Off(emptyList()),
    Thirds(listOf(1f / 3f, 2f / 3f)),
    Golden(listOf(0.381966f, 0.618034f));

    /** x positions of the vertical lines on an image [width] wide. */
    fun verticalLines(width: Float): List<Float> = fractions.map { it * width }

    /** y positions of the horizontal lines on an image [height] high. */
    fun horizontalLines(height: Float): List<Float> = fractions.map { it * height }

    /** Both endpoints of every line, mapped through [matrix]. */
    fun mappedPoints(width: Float, height: Float, matrix: FloatArray): List<Pair<Float, Float>> {
        val points = ArrayList<Pair<Float, Float>>()
        for (x in verticalLines(width)) {
            points += map(matrix, x, 0f)
            points += map(matrix, x, height)
        }
        for (y in horizontalLines(height)) {
            points += map(matrix, 0f, y)
            points += map(matrix, width, y)
        }
        return points
    }

    private fun map(m: FloatArray, x: Float, y: Float): Pair<Float, Float> {
        val w = m[6] * x + m[7] * y + m[8]
        return Pair(
            (m[0] * x + m[1] * y + m[2]) / w,
            (m[3] * x + m[4] * y + m[5]) / w
        )
    }
}
