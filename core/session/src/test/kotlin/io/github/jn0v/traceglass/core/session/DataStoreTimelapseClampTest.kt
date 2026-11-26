package io.github.jn0v.traceglass.core.session

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DataStoreTimelapseClampTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `stored 0 reads back as 1 and stored 999 reads back as 60`() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tempDir, "clamp_low.preferences_pb")
        }
        val interval = intPreferencesKey("settings_timelapse_interval_seconds")
        val fps = intPreferencesKey("settings_timelapse_fps")
        val repo = DataStoreSettingsRepository(dataStore)

        dataStore.edit { it[interval] = 0; it[fps] = 0 }
        var data = repo.settingsData.first()
        assertEquals(1, data.timelapseIntervalSeconds)
        assertEquals(1, data.timelapseFps)

        dataStore.edit { it[interval] = 999; it[fps] = 999 }
        data = repo.settingsData.first()
        assertEquals(60, data.timelapseIntervalSeconds)
        assertEquals(60, data.timelapseFps)
    }
}
