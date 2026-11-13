package io.github.jn0v.traceglass.core.session

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DataStoreSessionRangeTest {

    @TempDir
    lateinit var tempDir: File

    private fun TestScope.createRepo(): DataStoreSessionRepository {
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tempDir, "range.preferences_pb")
        }
        return DataStoreSessionRepository(dataStore)
    }

    // Given a stored zoom of 0.5, When the session is read, Then its zoom is 1.
    @Test
    fun `zoom below 1 is read as 1`() = runTest {
        val repo = createRepo()
        repo.save(SessionData(viewportZoom = 0.5f))
        assertEquals(1f, repo.sessionData.first().viewportZoom)
    }

    // Given a stored opacity of 1.7, When the session is read, Then its opacity is 1.
    @Test
    fun `opacity above 1 is read as 1`() = runTest {
        val repo = createRepo()
        repo.save(SessionData(overlayOpacity = 1.7f))
        assertEquals(1f, repo.sessionData.first().overlayOpacity)
    }

    // Given a stored scale of NaN and an offset of infinity, When the session is read,
    // Then they come back as 1 and 0.
    @Test
    fun `not a number scale and infinite offset are read as 1 and 0`() = runTest {
        val repo = createRepo()
        repo.save(
            SessionData(
                overlayScale = Float.NaN,
                overlayOffsetX = Float.POSITIVE_INFINITY,
                overlayOffsetY = Float.NEGATIVE_INFINITY
            )
        )
        val data = repo.sessionData.first()
        assertEquals(1f, data.overlayScale)
        assertEquals(0f, data.overlayOffsetX)
        assertEquals(0f, data.overlayOffsetY)
    }
}
