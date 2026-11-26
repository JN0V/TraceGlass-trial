package io.github.jn0v.traceglass.core.session

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DataStoreTimelapseSettingsTest {

    @TempDir
    lateinit var tempDir: File

    // Given a fresh install, Then the interval is 5 s and the video plays at 10 fps
    @Test
    fun `interval and speed have their defaults on an empty store`() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tempDir, "defaults.preferences_pb")
        }
        val data = DataStoreSettingsRepository(dataStore).settingsData.first()
        assertEquals(5, data.timelapseIntervalSeconds)
        assertEquals(10, data.timelapseFps)
    }

    // Given 15 s and 30 fps set, When I restart the app, Then both are still set
    @Test
    fun `interval and speed are kept by a new repository instance`() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tempDir, "kept_timelapse.preferences_pb")
        }
        val first = DataStoreSettingsRepository(dataStore)
        first.setTimelapseIntervalSeconds(15)
        first.setTimelapseFps(30)
        val data = DataStoreSettingsRepository(dataStore).settingsData.first()
        assertEquals(15, data.timelapseIntervalSeconds)
        assertEquals(30, data.timelapseFps)
    }
}
