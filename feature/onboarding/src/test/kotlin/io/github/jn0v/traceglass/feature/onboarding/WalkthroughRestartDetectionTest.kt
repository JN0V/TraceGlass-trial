package io.github.jn0v.traceglass.feature.onboarding

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WalkthroughRestartDetectionTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var vm: WalkthroughViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        vm = WalkthroughViewModel(
            onboardingRepository = FakeOnboardingRepository(),
            sessionRepository = FakeSessionRepository()
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun runVmTest(block: suspend TestScope.() -> Unit) = runTest(testDispatcher) {
        try {
            block()
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    // Given the markers were found, When detection is started again,
    // Then the walkthrough stays on "markers found".
    @Test
    fun `startDetection after markers found stays on MARKERS_FOUND`() = runVmTest {
        vm.startDetection()
        vm.onMarkersDetected()
        vm.startDetection()
        assertEquals(WalkthroughStep.MARKERS_FOUND, vm.uiState.value.step)
    }

    // Given an image was picked, When detection is started again,
    // Then it stays on the tooltip step and keeps the image.
    @Test
    fun `startDetection after image picked stays on SHOW_TOOLTIP and keeps image`() = runVmTest {
        vm.onImagePicked("file:///data/reference_image.jpg")
        advanceUntilIdle()
        vm.startDetection()
        assertEquals(WalkthroughStep.SHOW_TOOLTIP, vm.uiState.value.step)
        assertEquals("file:///data/reference_image.jpg", vm.uiState.value.imageUri)
    }
}
