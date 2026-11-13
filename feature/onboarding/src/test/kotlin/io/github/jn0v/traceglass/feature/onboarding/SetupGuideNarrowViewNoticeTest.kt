package io.github.jn0v.traceglass.feature.onboarding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SetupGuideNarrowViewNoticeTest {

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Given a camera whose widest zoom is 1.0, When the setup guide opens,
    // Then the narrow-view notice is shown.
    @Test
    fun `no ultra-wide shows the narrow-view notice`() {
        val vm = SetupGuideViewModel(MutableStateFlow<Float?>(1.0f))

        assertTrue(vm.uiState.value.showNarrowViewNotice)
    }

    // Given a camera whose widest zoom is 0.6, When the setup guide opens,
    // Then no notice is shown.
    @Test
    fun `ultra-wide shows no notice`() {
        val vm = SetupGuideViewModel(MutableStateFlow<Float?>(0.6f))

        assertFalse(vm.uiState.value.showNarrowViewNotice)
    }

    // Given no zoom information, When the setup guide opens,
    // Then no notice is shown.
    @Test
    fun `unknown camera shows no notice`() {
        val vm = SetupGuideViewModel(MutableStateFlow<Float?>(null))

        assertFalse(vm.uiState.value.showNarrowViewNotice)
    }
}
