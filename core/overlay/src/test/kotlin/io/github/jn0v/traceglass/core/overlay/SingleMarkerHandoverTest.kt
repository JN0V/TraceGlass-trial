package io.github.jn0v.traceglass.core.overlay

import io.github.jn0v.traceglass.core.cv.DetectedMarker
import io.github.jn0v.traceglass.core.cv.MarkerResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.math.sin

class SingleMarkerHandoverTest {

    private fun marker(id: Int, cx: Float, cy: Float, size: Float, angleDeg: Float): DetectedMarker {
        val h = size / 2f
        val rad = Math.toRadians(angleDeg.toDouble())
        val c = cos(rad).toFloat()
        val s = sin(rad).toFloat()
        val corners = listOf(
            Pair(cx - h * c + h * s, cy - h * s - h * c),
            Pair(cx + h * c + h * s, cy + h * s - h * c),
            Pair(cx + h * c - h * s, cy + h * s + h * c),
            Pair(cx - h * c - h * s, cy - h * s + h * c)
        )
        return DetectedMarker(id, cx, cy, corners, 1f)
    }

    private fun result(vararg markers: DetectedMarker) =
        MarkerResult(markers.toList(), 5L, 1080, 1920)

    /** Two markers 600 px apart (scaled), the pair turned by angleDeg around marker 0. */
    private fun pair(angleDeg: Float, scale: Float): MarkerResult {
        val rad = Math.toRadians(angleDeg.toDouble())
        val d = 600f * scale
        val m0 = marker(0, 200f, 500f, 50f * scale, angleDeg)
        val m1 = marker(
            1, 200f + d * cos(rad).toFloat(), 500f + d * sin(rad).toFloat(),
            50f * scale, angleDeg
        )
        return result(m0, m1)
    }

    // Given a two-marker rotation of 30°, When only marker 0 is still seen, Then the rotation is about 30°.
    @Test
    fun `rotation kept when dropping to one marker`() {
        val calc = OverlayTransformCalculator()
        calc.compute(pair(0f, 1f), 1080f, 1920f)
        val two = calc.compute(pair(30f, 1f), 1080f, 1920f)
        assertEquals(30f, two.rotation, 1f)

        val one = calc.compute(result(marker(0, 200f, 500f, 50f, 30f)), 1080f, 1920f)
        assertEquals(30f, one.rotation, 2f)
    }

    // Given a two-marker scale of 1.5, When only one marker is still seen, Then the scale is about 1.5.
    @Test
    fun `scale kept when dropping to one marker`() {
        val calc = OverlayTransformCalculator()
        calc.compute(pair(0f, 1f), 1080f, 1920f)
        val two = calc.compute(pair(0f, 1.5f), 1080f, 1920f)
        assertEquals(1.5f, two.scale, 0.05f)

        val one = calc.compute(result(marker(0, 200f, 500f, 75f, 0f)), 1080f, 1920f)
        assertEquals(1.5f, one.scale, 0.1f)
    }

    // Given the switch to one marker, When that marker turns another 10°, Then the rotation is about 40°.
    @Test
    fun `single marker follows further rotation after the switch`() {
        val calc = OverlayTransformCalculator()
        calc.compute(pair(0f, 1f), 1080f, 1920f)
        calc.compute(pair(30f, 1f), 1080f, 1920f)
        calc.compute(result(marker(0, 200f, 500f, 50f, 30f)), 1080f, 1920f)

        val turned = calc.compute(result(marker(0, 200f, 500f, 50f, 40f)), 1080f, 1920f)
        assertEquals(40f, turned.rotation, 2f)
    }
}
