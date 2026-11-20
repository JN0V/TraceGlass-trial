package io.github.jn0v.traceglass.core.camera

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class WorklineAnswerWorkflowTest {

    private fun workflow(): String {
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            val f = File(dir, ".github/workflows/workline-answer.yml")
            if (f.isFile) return f.readText()
            dir = dir.parentFile
        }
        throw AssertionError("workline-answer.yml not found above ${File("").absolutePath}")
    }

    private fun between(text: String, from: String, to: String): String {
        val start = text.indexOf(from)
        assertTrue(start >= 0, "workflow has no '$from'")
        val end = text.indexOf(to, start + from.length)
        assertTrue(end > start, "workflow has no '$to' after '$from'")
        return text.substring(start, end)
    }

    // The three later lookups of the pull request number: the answer step,
    // the apply job, the "answer's state" step; each with the output that
    // would carry the head step's number to it.
    private fun sites(text: String): Map<String, Pair<String, String>> = mapOf(
        "answer step" to (between(text, "id: answer", "uses: actions/upload-artifact") to "steps.head.outputs.number"),
        "apply job" to (between(text, "\n  apply:\n", "name: The answer's state") to "needs.judge.outputs.number"),
        "answer's state step" to (between(text, "name: The answer's state", "name: The next answer") to "needs.judge.outputs.number"),
    )

    private fun unfilteredLookups(region: String): List<String> =
        region.lines().filter { it.contains("gh pr list") && !it.contains("isCrossRepository") }

    // Given a Draft pull request in this repository and a fork pull request on
    // the same branch name, When the answer workflow runs, Then the answer is
    // made and applied on the Draft's pull request only: no later lookup lists
    // the pull requests of the branch without leaving out the cross-repository ones.
    @Test
    fun `same-named fork branch is never answered nor applied`() {
        for ((name, site) in sites(workflow())) {
            assertEquals(
                emptyList<String>(), unfilteredLookups(site.first),
                "the $name takes a pull request number from a list that keeps fork pull requests",
            )
        }
    }

    // Given only the Draft's pull request, When the answer workflow runs, Then the
    // answer is made and applied on it as before: the head step gives the number
    // and each later place takes it from there, or filters as the head step does.
    @Test
    fun `no fork answers and applies on the Draft's pull request`() {
        val text = workflow()
        val head = between(text, "name: The Draft's head", "name: The answer (proposed only)")
        val judgeOutputs = between(text, "    outputs:\n", "    steps:\n")
        val headOutputsNumber = head.contains("number=") && judgeOutputs.contains("number: \${{ steps.head.outputs.number }}")
        for ((name, site) in sites(text)) {
            val (region, ref) = site
            val fromHead = region.contains(ref) && headOutputsNumber
            val filtered = region.lines().any { it.contains("gh pr list") && it.contains("isCrossRepository") }
            assertTrue(fromHead || filtered, "the $name does not take the number of the head step's pull request")
            assertEquals(emptyList<String>(), unfilteredLookups(region), "the $name still has an unfiltered lookup")
        }
        val answer = sites(text).getValue("answer step").first
        assertTrue(answer.contains("workline test answer --merge-request \"\$pr\""), "the answer step no longer answers a pull request number")
    }

    // Given an open pull request from a fork only, When the workflow runs, Then it
    // answers nothing and exits cleanly: the head step finds none and exits 0, and
    // no later place finds the fork's pull request, or each ends cleanly on none.
    @Test
    fun `only a fork pull request answers nothing and exits cleanly`() {
        val text = workflow()
        val head = between(text, "name: The Draft's head", "name: The answer (proposed only)")
        assertTrue(head.contains("nothing to answer") && head.contains("exit 0"), "the head step does not exit cleanly on none")
        for ((name, site) in sites(text)) {
            val (region, ref) = site
            val lookups = region.lines().filter { it.contains("gh pr list") }
            if (lookups.isEmpty()) {
                assertTrue(region.contains(ref), "the $name has no lookup and no number from the head step")
            } else {
                assertTrue(lookups.all { it.contains("isCrossRepository") }, "the $name finds a fork pull request")
                assertTrue(region.contains("exit 0"), "the $name does not exit cleanly on no pull request")
            }
        }
        assertTrue(sites(text).getValue("answer step").first.isNotEmpty())
        assertTrue(text.contains("if: steps.head.outputs.head != ''"), "the answer step runs without a head")
    }
}
