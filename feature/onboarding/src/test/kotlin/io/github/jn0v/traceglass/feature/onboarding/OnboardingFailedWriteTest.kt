package io.github.jn0v.traceglass.feature.onboarding

import io.github.jn0v.traceglass.core.session.SessionData
import io.github.jn0v.traceglass.core.session.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingFailedWriteTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FailingOnboardingRepository : OnboardingRepository {
        override val isOnboardingCompleted: Flow<Boolean> = flowOf(false)
        override val isTooltipShown: Flow<Boolean> = flowOf(false)
        override val selectedTier: Flow<SetupTier?> = flowOf(null)
        override suspend fun setOnboardingCompleted() { throw IOException("disk full") }
        override suspend fun setTooltipShown() { throw IOException("disk full") }
        override suspend fun setSelectedTier(tier: SetupTier) { throw IOException("disk full") }
        override suspend fun resetOnboarding() { throw IOException("disk full") }
    }

    private class FailingSessionRepository : SessionRepository {
        override val sessionData: Flow<SessionData> = flowOf(SessionData())
        override suspend fun save(data: SessionData) { throw IOException("disk full") }
        override suspend fun clear() { throw IOException("disk full") }
    }

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Given saving "onboarding completed" fails, When I tap Done, Then onboarding ends and nothing crashes.
    @Test
    fun `done despite a failed write`() = runTest(testDispatcher) {
        val vm = OnboardingViewModel(FailingOnboardingRepository())
        testDispatcher.scheduler.advanceUntilIdle()
        vm.onComplete()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.isCompleted)
        assertFalse(vm.uiState.value.wasSkipped)
    }

    // Given the same failure, When I tap Skip, Then onboarding ends as skipped and nothing crashes.
    @Test
    fun `skip despite a failed write`() = runTest(testDispatcher) {
        val vm = OnboardingViewModel(FailingOnboardingRepository())
        testDispatcher.scheduler.advanceUntilIdle()
        vm.onSkip()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.isCompleted)
        assertTrue(vm.uiState.value.wasSkipped)
    }

    // Given saving the session fails, When I pick an image in the walkthrough, Then it moves on to the tooltip step.
    @Test
    fun `walkthrough goes on`() = runTest(testDispatcher) {
        val vm = WalkthroughViewModel(FailingOnboardingRepository(), FailingSessionRepository())
        vm.onImagePicked("file:///data/reference_image.jpg")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(WalkthroughStep.SHOW_TOOLTIP, vm.uiState.value.step)
    }
}
