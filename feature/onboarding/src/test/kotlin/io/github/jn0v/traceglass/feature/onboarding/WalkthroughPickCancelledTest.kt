package io.github.jn0v.traceglass.feature.onboarding

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class WalkthroughPickCancelledTest {

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
        vm.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    // Given the walkthrough on its "pick an image" step,
    // When the pick is cancelled,
    // Then it is back on "markers found", with no image.
    @Test
    fun `cancel goes back`() {
        vm.onMarkersDetected()
        vm.onProceedToPickImage()
        assertEquals(WalkthroughStep.PICK_IMAGE, vm.uiState.value.step)
        vm.onImagePickCancelled()
        assertEquals(WalkthroughStep.MARKERS_FOUND, vm.uiState.value.step)
        assertNull(vm.uiState.value.imageUri)
    }

    // Given a cancelled pick,
    // When I tap Continue,
    // Then the "pick an image" step comes back.
    @Test
    fun `try again`() {
        vm.onMarkersDetected()
        vm.onProceedToPickImage()
        vm.onImagePickCancelled()
        vm.onProceedToPickImage()
        assertEquals(WalkthroughStep.PICK_IMAGE, vm.uiState.value.step)
    }

    // Given the walkthrough on its tooltip step,
    // When a pick is cancelled,
    // Then the step does not change.
    @Test
    fun `ignored elsewhere`() {
        vm.onMarkersDetected()
        vm.onProceedToPickImage()
        vm.onImagePicked("file:///data/reference_image.jpg")
        assertEquals(WalkthroughStep.SHOW_TOOLTIP, vm.uiState.value.step)
        vm.onImagePickCancelled()
        assertEquals(WalkthroughStep.SHOW_TOOLTIP, vm.uiState.value.step)
    }
}
