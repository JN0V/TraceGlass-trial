package io.github.jn0v.traceglass.core.overlay

import io.github.jn0v.traceglass.core.cv.DetectedMarker
import io.github.jn0v.traceglass.core.cv.MarkerResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OverlayTransformPairSwapTest {

    private fun marker(id: Int, cx: Float, cy: Float): DetectedMarker =
        DetectedMarker(id, cx, cy, emptyList(), 1f)

    private fun result(vararg markers: DetectedMarker) = MarkerResult(markers.toList(), 5L)

    private val m0 = marker(0, 100f, 500f)
    private val m1 = marker(1, 500f, 500f)
    private val m2 = marker(2, 500f, 900f)
    private val m3 = marker(3, 100f, 900f)

    // Given a reference on markers 0 and 1,
    // When only markers 1 and 2 are seen at the same places,
    // Then the rotation is about 0 and the scale about 1.
    @Test
    fun `pair swap keeps rotation and scale`() {
        val calc = OverlayTransformCalculator()
        calc.compute(result(m0, m1, m2), 1080f, 1920f)

        val swapped = calc.compute(result(m1, m2), 1080f, 1920f)

        assertEquals(0f, swapped.rotation, 1f)
        assertEquals(1f, swapped.scale, 0.05f)
    }

    // Given the same frames,
    // When the pair changes,
    // Then the overlay's offset does not change.
    @Test
    fun `pair swap keeps the place`() {
        val calc = OverlayTransformCalculator()
        val before = calc.compute(result(m0, m1, m2), 1080f, 1920f)

        val swapped = calc.compute(result(m1, m2), 1080f, 1920f)

        assertEquals(before.offsetX, swapped.offsetX, 1f)
        assertEquals(before.offsetY, swapped.offsetY, 1f)
    }

    // Given a reference on markers 0, 1 and 2,
    // When markers 1, 2 and 3 are seen with nothing moved,
    // Then the rotation is about 0.
    @Test
    fun `three markers keep rotation when the set shifts`() {
        val calc = OverlayTransformCalculator()
        calc.compute(result(m0, m1, m2), 1080f, 1920f)

        val shifted = calc.compute(result(m1, m2, m3), 1080f, 1920f)

        assertEquals(0f, shifted.rotation, 1f)
    }
}
