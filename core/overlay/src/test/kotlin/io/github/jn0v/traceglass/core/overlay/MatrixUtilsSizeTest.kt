package io.github.jn0v.traceglass.core.overlay

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MatrixUtilsSizeTest {

    // Given a matrix of 8 numbers, When it is multiplied by the identity,
    // Then it is refused with an IllegalArgumentException saying a matrix holds 9 numbers.
    @Test
    fun `a matrix of 8 numbers is refused`() {
        val e = assertThrows(IllegalArgumentException::class.java) {
            MatrixUtils.multiply(FloatArray(8), MatrixUtils.identity())
        }
        assertTrue(e.message?.contains("9") == true, "message was: ${e.message}")
    }

    // Given a matrix of 10 numbers, When it is multiplied by the identity,
    // Then it is refused the same way.
    @Test
    fun `a matrix of 10 numbers is refused`() {
        val e = assertThrows(IllegalArgumentException::class.java) {
            MatrixUtils.multiply(FloatArray(10), MatrixUtils.identity())
        }
        assertTrue(e.message?.contains("9") == true, "message was: ${e.message}")
    }
}
