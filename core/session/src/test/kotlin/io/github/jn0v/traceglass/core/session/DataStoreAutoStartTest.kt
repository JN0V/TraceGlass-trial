package io.github.jn0v.traceglass.core.session

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DataStoreAutoStartTest {

    @TempDir
    lateinit var tempDir: File

    // Given auto-start turned off, When I restart the app (a new repository on the same DataStore),
    // Then it is still off.
    @Test
    fun `auto-start turned off is kept by a new repository instance`() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tempDir, "autostart.preferences_pb")
        }
        DataStoreSettingsRepository(dataStore).setAutoStartTimelapse(false)

        val second = DataStoreSettingsRepository(dataStore)

        assertFalse(second.settingsData.first().autoStartTimelapse)
    }
}
