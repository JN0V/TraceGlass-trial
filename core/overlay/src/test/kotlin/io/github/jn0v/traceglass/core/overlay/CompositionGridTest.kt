package io.github.jn0v.traceglass.core.overlay

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CompositionGridTest {

    // Thirds: Given Thirds on a 900x1200 image, Then the lines are at x=300, 600 and y=400, 800.
    @Test
    fun thirds() {
        val xs: List<Float> = CompositionGrid.Thirds.verticalLines(900f)
        val ys: List<Float> = CompositionGrid.Thirds.horizontalLines(1200f)
        assertEquals(listOf(300f, 600f), xs)
        assertEquals(listOf(400f, 800f), ys)
    }

    // Golden: Given Golden, Then the lines are at 0.382 and 0.618 of each side.
    @Test
    fun golden() {
        val xs: List<Float> = CompositionGrid.Golden.verticalLines(1000f)
        val ys: List<Float> = CompositionGrid.Golden.horizontalLines(1000f)
        assertEquals(2, xs.size)
        assertEquals(2, ys.size)
        assertEquals(381.966f, xs[0], 1f)
        assertEquals(618.034f, xs[1], 1f)
        assertEquals(381.966f, ys[0], 1f)
        assertEquals(618.034f, ys[1], 1f)
    }

    // Follows the paper: Given perspective tracking, Then grid points are mapped by the same matrix as the image.
    @Test
    fun followsThePaper() {
        val matrix: FloatArray = MatrixUtils.compose(MatrixUtils.translate(10f, 20f), MatrixUtils.scale(2f))
        val mapped: List<Pair<Float, Float>> = CompositionGrid.Thirds.mappedPoints(90f, 120f, matrix)
        val expected: Set<Pair<Float, Float>> = setOf(
            Pair(70f, 20f), Pair(70f, 260f), Pair(130f, 20f), Pair(130f, 260f),
            Pair(10f, 100f), Pair(190f, 100f), Pair(10f, 180f), Pair(190f, 180f)
        )
        assertEquals(expected, mapped.toSet())
    }
}
