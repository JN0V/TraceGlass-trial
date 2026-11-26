package io.github.jn0v.traceglass.core.session

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DataStoreSettingsRepositoryTest {

    @TempDir
    lateinit var tempDir: File

    // Given a fresh install, When settings are read from an empty DataStore, Then the grid mode is Off
    @Test
    fun `grid mode is Off on an empty store`() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tempDir, "empty.preferences_pb")
        }
        val repo = DataStoreSettingsRepository(dataStore)
        assertEquals(GridMode.OFF, repo.settingsData.first().gridMode)
    }

    // Given I chose Thirds, When a new repository is built on the same DataStore, Then it reads Thirds
    @Test
    fun `grid mode Thirds is kept by a new repository instance`() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tempDir, "kept.preferences_pb")
        }
        DataStoreSettingsRepository(dataStore).setGridMode(GridMode.THIRDS)
        val second = DataStoreSettingsRepository(dataStore)
        assertEquals(GridMode.THIRDS, second.settingsData.first().gridMode)
    }

    // Given a break interval of 0 minutes saved, When the settings are read, Then the interval is 5 minutes
    @Test
    fun `break interval of 0 minutes is read as 5`() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tempDir, "interval_short.preferences_pb")
        }
        val repo = DataStoreSettingsRepository(dataStore)
        repo.setBreakReminderIntervalMinutes(0)
        assertEquals(5, repo.settingsData.first().breakReminderIntervalMinutes)
    }

    // Given a break interval of 500 minutes saved, When the settings are read, Then the interval is 60 minutes
    @Test
    fun `break interval of 500 minutes is read as 60`() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tempDir, "interval_long.preferences_pb")
        }
        val repo = DataStoreSettingsRepository(dataStore)
        repo.setBreakReminderIntervalMinutes(500)
        assertEquals(60, repo.settingsData.first().breakReminderIntervalMinutes)
    }
}
