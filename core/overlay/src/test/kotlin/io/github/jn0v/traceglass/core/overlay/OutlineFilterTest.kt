package io.github.jn0v.traceglass.core.overlay

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class OutlineFilterTest {

    private val black = 0xFF000000.toInt()
    private val white = 0xFFFFFFFF.toInt()
    private val opaque = 0xFF000000.toInt()
    private val clear = 0x00000000

    private fun grey(v: Int): Int = (0xFF shl 24) or (v shl 16) or (v shl 8) or v

    // Given the half-black, half-white picture, When it is filtered,
    // Then only the pixels along the edge are opaque.
    @Test
    fun `edge kept`() {
        val pixels = IntArray(16) { i -> if (i % 4 < 2) black else white }
        val out = OutlineFilter.filter(pixels, 4, 4, 128)
        assertEquals(16, out.size)
        for (y in 0 until 4) {
            for (x in 0 until 4) {
                val expected = if (x == 1 || x == 2) opaque else clear
                assertEquals(expected, out[y * 4 + x], "pixel x=$x y=$y")
            }
        }
    }

    // Given a plain grey 8x8 picture, When it is filtered, Then every pixel is transparent.
    @Test
    fun `flat picture`() {
        val pixels = IntArray(64) { grey(128) }
        val out = OutlineFilter.filter(pixels, 8, 8, 1)
        assertArrayEquals(IntArray(64) { clear }, out)
    }

    // Given a step from 0 to 100 and a threshold of 128, When it is filtered,
    // Then no edge is shown.
    @Test
    fun `threshold`() {
        val pixels = IntArray(16) { i -> if (i % 4 < 2) grey(0) else grey(100) }
        val out = OutlineFilter.filter(pixels, 4, 4, 128)
        assertArrayEquals(IntArray(16) { clear }, out)
    }

    // Given a step from 0 to 100 and a threshold of 100, When it is filtered,
    // Then the edge is shown: a step equal to the threshold counts as an edge.
    @Test
    fun `step equal to threshold`() {
        val pixels = IntArray(16) { i -> if (i % 4 < 2) grey(0) else grey(100) }
        val out = OutlineFilter.filter(pixels, 4, 4, 100)
        for (y in 0 until 4) {
            for (x in 0 until 4) {
                val expected = if (x == 1 || x == 2) opaque else clear
                assertEquals(expected, out[y * 4 + x], "pixel x=$x y=$y")
            }
        }
    }

    // Given a pixel list whose length is not width x height, When it is filtered,
    // Then it is refused with an error.
    @Test
    fun `wrong size`() {
        assertThrows(IllegalArgumentException::class.java) {
            OutlineFilter.filter(IntArray(15), 4, 4, 128)
        }
    }
}
