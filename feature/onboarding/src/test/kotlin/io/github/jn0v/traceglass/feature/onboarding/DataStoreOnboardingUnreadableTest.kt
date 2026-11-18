package io.github.jn0v.traceglass.feature.onboarding

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

class DataStoreOnboardingUnreadableTest {

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    // Only reads fail: a write succeeds, so a fix that writes defaults
    // back after a failed read does not trip this fake.
    private class ThrowingStore(private val error: Throwable) : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw error }
        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences
        ): Preferences = transform(emptyPreferences())
    }

    // Given the onboarding store throws an IOException on read,
    // When "onboarding completed" is read, Then it is false.
    @Test
    fun `unreadable store gives onboarding not completed`() = runTest {
        val repo = DataStoreOnboardingRepository(ThrowingStore(IOException("corrupt")))
        assertFalse(repo.isOnboardingCompleted.first())
    }

    // Given the same store, When the chosen tier is read, Then there is none.
    @Test
    fun `unreadable store gives no selected tier`() = runTest {
        val repo = DataStoreOnboardingRepository(ThrowingStore(IOException("corrupt")))
        assertNull(repo.selectedTier.first())
    }
}
