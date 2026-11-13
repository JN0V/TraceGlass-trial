package io.github.jn0v.traceglass.feature.onboarding

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WalkthroughRepeatedStartTest {

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

    // Given the walkthrough has searched for 5 seconds without markers,
    // When detection is started again, as a rotation does,
    // Then the elapsed time is still 5 seconds and not 0.
    @Test
    fun `second startDetection keeps elapsed seconds`() = runVmTest {
        vm.startDetection()
        advanceTimeBy(5100)
        assertEquals(5, vm.uiState.value.elapsedSeconds)
        vm.startDetection()
        assertEquals(5, vm.uiState.value.elapsedSeconds)
    }

    // Given the walkthrough has searched for 6 seconds,
    // When detection is started again and 4 more seconds pass,
    // Then the help hint shows.
    @Test
    fun `second startDetection still shows guidance after 10 seconds in total`() = runVmTest {
        vm.startDetection()
        advanceTimeBy(6100)
        vm.startDetection()
        advanceTimeBy(4000)
        assertTrue(vm.uiState.value.showGuidance)
    }
}
