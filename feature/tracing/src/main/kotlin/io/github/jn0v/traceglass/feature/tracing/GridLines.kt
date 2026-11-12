package io.github.jn0v.traceglass.feature.tracing

import io.github.jn0v.traceglass.core.session.GridMode

/** A line segment in image pixels. */
internal data class GridLine(val x1: Float, val y1: Float, val x2: Float, val y2: Float)

/**
 * Grid lines over an image of [width] x [height] pixels, in image pixels:
 * two vertical then two horizontal lines, spanning the whole image.
 * Empty when [mode] is OFF.
 */
internal fun computeGridLines(width: Float, height: Float, mode: GridMode): List<GridLine> {
    val fractions = when (mode) {
        GridMode.OFF -> return emptyList()
        GridMode.THIRDS -> floatArrayOf(1f / 3f, 2f / 3f)
        GridMode.GOLDEN_RATIO -> floatArrayOf(0.382f, 0.618f)
    }
    return fractions.map { GridLine(it * width, 0f, it * width, height) } +
        fractions.map { GridLine(0f, it * height, width, it * height) }
}
