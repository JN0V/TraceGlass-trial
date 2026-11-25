package io.github.jn0v.traceglass.core.overlay

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CorrectAspectRatioFocalLengthTest {

    private val rect = listOf(
        Pair(0f, 0f), Pair(210f, 0f), Pair(210f, 297f), Pair(0f, 297f)
    )
    private val moved = rect.map { (x, y) -> Pair(x + 100f, y + 50f) }

    private val cx = 640f
    private val cy = 360f

    // Given a 210 by 297 rectangle and the same rectangle moved by (100, 50),
    // When the aspect ratio is corrected with a focal length of 0,
    // Then the result is null.
    @Test
    fun `a focal length of 0 is refused`() {
        val result = HomographySolver.correctAspectRatio(rect, moved, 0f, cx, cy)
        assertNull(result, "Focal length 0 should give null, got $result")
    }

    // Given the same two rectangles,
    // When the aspect ratio is corrected with a focal length of -800,
    // Then the result is null.
    @Test
    fun `a negative focal length is refused`() {
        val result = HomographySolver.correctAspectRatio(rect, moved, -800f, cx, cy)
        assertNull(result, "Negative focal length should give null, got $result")
    }
}
