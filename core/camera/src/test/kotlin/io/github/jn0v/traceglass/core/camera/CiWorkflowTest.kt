package io.github.jn0v.traceglass.core.camera

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class CiWorkflowTest {

    // Gradle runs unit tests with the module directory as working directory.
    private val workflow = File("../../.github/workflows/ci.yml").readText()

    private fun steps(): List<String> =
        workflow.split(Regex("(?m)^      - name: ")).drop(1)

    private fun step(name: String): String? =
        steps().firstOrNull { it.lineSequence().first().trim() == name }

    private fun uploadOf(artifact: String): String? =
        steps().firstOrNull {
            it.contains("actions/upload-artifact") && Regex("name: ${Regex.escape(artifact)}\\s*$", RegexOption.MULTILINE).containsMatchIn(it)
        }

    private fun retention(stepText: String): String? =
        Regex("retention-days:\\s*(\\d+)").find(stepText)?.groupValues?.get(1)

    // Given a pull request on main
    // When its CI run finishes
    // Then its Artifacts list holds traceglass-debug-apk with app-debug.apk inside
    @Test
    fun `pull request run uploads traceglass-debug-apk with app-debug apk`() {
        assertTrue(Regex("(?m)^  pull_request:").containsMatchIn(workflow))
        val upload = uploadOf("traceglass-debug-apk")
        assertNotNull(upload, "no upload step for traceglass-debug-apk")
        assertTrue(upload!!.contains("app/build/outputs/apk/debug/app-debug.apk"))
        assertFalse(upload.contains("github.event_name"), "upload must not be limited to an event")
        val names = steps().map { it.lineSequence().first().trim() }
        assertEquals(names.indexOf("Build debug") + 1, names.indexOf(upload.lineSequence().first().trim()))
    }

    // Given a push to main
    // When its CI run finishes
    // Then the same artifact is offered
    @Test
    fun `push to main run uploads the same artifact`() {
        assertTrue(Regex("(?m)^  push:").containsMatchIn(workflow))
        val upload = uploadOf("traceglass-debug-apk")
        assertNotNull(upload, "no upload step for traceglass-debug-apk")
        assertFalse(upload!!.contains("if:"), "upload must run on every event")
    }

    // Given the artifact
    // When I compare its retention to lint-results
    // Then both expire after the same few days, not the default 90
    @Test
    fun `debug apk retention equals lint-results retention`() {
        val apk = uploadOf("traceglass-debug-apk")
        assertNotNull(apk, "no upload step for traceglass-debug-apk")
        val lint = uploadOf("lint-results")!!
        assertEquals("7", retention(lint))
        assertEquals(retention(lint), retention(apk!!))
    }

    // Given the CI run
    // When I list its artifacts
    // Then no release APK is among them
    @Test
    fun `no release apk is uploaded`() {
        val apk = uploadOf("traceglass-debug-apk")
        assertNotNull(apk, "no upload step for traceglass-debug-apk")
        steps().filter { it.contains("actions/upload-artifact") }.forEach {
            assertFalse(it.contains("release", ignoreCase = true), "release upload: $it")
            assertFalse(it.contains("*.apk"), "wildcard apk upload: $it")
        }
    }
}
