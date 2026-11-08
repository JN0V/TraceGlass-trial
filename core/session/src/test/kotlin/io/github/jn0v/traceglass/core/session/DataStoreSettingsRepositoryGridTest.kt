package io.github.jn0v.traceglass.core.session

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.jn0v.traceglass.core.overlay.CompositionGrid
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DataStoreSettingsRepositoryGridTest {

    @TempDir
    lateinit var tempDir: File

    // Choice kept: Given I pick Thirds, When I restart the app, Then Thirds is still selected.
    @Test
    fun choiceKept() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tempDir, "grid_prefs.preferences_pb")
        }
        DataStoreSettingsRepository(dataStore).setGrid(CompositionGrid.Thirds)

        val restarted = DataStoreSettingsRepository(dataStore)
        assertEquals(CompositionGrid.Thirds, restarted.settingsData.first().grid)
    }
}
