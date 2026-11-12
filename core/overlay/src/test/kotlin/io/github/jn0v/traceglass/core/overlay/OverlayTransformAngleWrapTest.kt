package io.github.jn0v.traceglass.core.overlay

import io.github.jn0v.traceglass.core.cv.DetectedMarker
import io.github.jn0v.traceglass.core.cv.MarkerResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.math.sin

class OverlayTransformAngleWrapTest {

    private fun pairResult(angleDeg: Double): MarkerResult {
        val rad = Math.toRadians(angleDeg)
        val x1 = 500f + (400 * cos(rad)).toFloat()
        val y1 = 500f + (400 * sin(rad)).toFloat()
        val m0 = DetectedMarker(0, 500f, 500f, emptyList(), 1f)
        val m1 = DetectedMarker(1, x1, y1, emptyList(), 1f)
        return MarkerResult(listOf(m0, m1), 5L, 1080, 1920)
    }

    private fun edgeMarker(angleDeg: Double): MarkerResult {
        val rad = Math.toRadians(angleDeg)
        val x1 = 500f + (50 * cos(rad)).toFloat()
        val y1 = 500f + (50 * sin(rad)).toFloat()
        val corners = listOf(
        Pair(500f, 500f), Pair(x1, y1), Pair(x1, y1 + 50f), Pair(500f, 550f)
        )
        return MarkerResult(
            listOf(DetectedMarker(0, 520f, 520f, corners, 1f)), 5L, 1080, 1920
        )
    }

    // Given a two-marker reference at 179.5°, When the next frame's angle is -179.5°,
    // Then the rotation is 1°.
    @Test
    fun `bottom pair turns 1 degree across 180`() {
        val calc = OverlayTransformCalculator()
        calc.compute(pairResult(179.5), 1080f, 1920f)
        val t = calc.compute(pairResult(-179.5), 1080f, 1920f)
        assertEquals(1f, t.rotation, 0.05f)
    }

    // Given a previous rotation of 0°, When that frame is smoothed,
    // Then the result is between 0° and 1°.
    @Test
    fun `smoothing stays near after wrap`() {
        val calc = OverlayTransformCalculator()
        calc.compute(pairResult(179.5), 1080f, 1920f)
        val t = calc.computeSmoothed(
            pairResult(-179.5), 1080f, 1920f, OverlayTransform(rotation = 0f)
        )
        assertTrue(t.rotation > 0f && t.rotation < 1f, "rotation was ${t.rotation}")
    }

    // Given one marker whose reference edge is at 179.5°, When it turns 1°,
    // Then the rotation is 1°.
    @Test
    fun `one marker near 180 turns 1 degree`() {
        val calc = OverlayTransformCalculator()
        calc.compute(edgeMarker(179.5), 1080f, 1920f)
        val t = calc.compute(edgeMarker(-179.5), 1080f, 1920f)
        assertEquals(1f, t.rotation, 0.05f)
    }
}
