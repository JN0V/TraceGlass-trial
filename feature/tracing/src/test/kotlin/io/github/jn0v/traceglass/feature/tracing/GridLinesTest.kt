package io.github.jn0v.traceglass.feature.tracing

import io.github.jn0v.traceglass.core.session.GridMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GridLinesTest {
    private val tol = 0.5f

    private fun xs(lines: List<GridLine>) = lines.filter { it.x1 == it.x2 }.map { it.x1 }
    private fun ys(lines: List<GridLine>) = lines.filter { it.y1 == it.y2 }.map { it.y1 }

    private fun check(mode: GridMode, vx: List<Float>, hy: List<Float>) {
        val lines = computeGridLines(900f, 1200f, mode)
        assertEquals(4, lines.size)
        val gx = xs(lines)
        val gy = ys(lines)
        assertEquals(2, gx.size)
        assertEquals(2, gy.size)
        vx.forEachIndexed { i, v -> assertEquals(v, gx[i], tol) }
        hy.forEachIndexed { i, v -> assertEquals(v, gy[i], tol) }
        lines.filter { it.x1 == it.x2 }.forEach {
            assertEquals(0f, it.y1, tol); assertEquals(1200f, it.y2, tol)
        }
        lines.filter { it.y1 == it.y2 }.forEach {
            assertEquals(0f, it.x1, tol); assertEquals(900f, it.x2, tol)
        }
    }

    @Test
    fun `thirds on 900x1200`() = check(GridMode.THIRDS, listOf(300f, 600f), listOf(400f, 800f))

    @Test
    fun `golden ratio on 900x1200`() =
        check(GridMode.GOLDEN_RATIO, listOf(343.8f, 556.2f), listOf(458.4f, 741.6f))

    @Test
    fun `off has no line`() {
        assertTrue(computeGridLines(900f, 1200f, GridMode.OFF).isEmpty())
    }
}
