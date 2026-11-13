package io.github.jn0v.traceglass.core.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import android.util.Log
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retryWhen
import java.io.IOException
import kotlinx.coroutines.flow.map

class DataStoreSessionRepository(
    private val dataStore: DataStore<Preferences>
) : SessionRepository {

    override val sessionData: Flow<SessionData> = dataStore.data.catch { e ->
        if (e !is IOException) throw e
        Log.w(TAG, "Session store unreadable — treating as empty session", e)
        emit(emptyPreferences())
        throw e
    }.retryWhen { cause, _ ->
        // Keep observing: a later successful save or a recovered file must still be seen.
        if (cause !is IOException) false else { delay(RETRY_DELAY_MS); true }
    }.map { prefs ->
        val version = prefs[KEY_SCHEMA_VERSION] ?: 0
        if (version != SCHEMA_VERSION && version != 0) {
            // Incompatible schema — discard stale data and return defaults
            Log.w(TAG, "Session schema version mismatch: stored=$version, current=$SCHEMA_VERSION — discarding")
            return@map SessionData()
        }
        SessionData(
            imageUri = prefs[KEY_IMAGE_URI],
            overlayOffsetX = finiteOr(prefs[KEY_OFFSET_X], 0f),
            overlayOffsetY = finiteOr(prefs[KEY_OFFSET_Y], 0f),
            overlayScale = finiteOr(prefs[KEY_SCALE], 1f).let { if (it <= 0f) 1f else it },
            overlayRotation = finiteOr(prefs[KEY_ROTATION], 0f),
            overlayOpacity = finiteOr(prefs[KEY_OPACITY], 0.5f).coerceIn(0f, 1f),
            colorTint = prefs[KEY_COLOR_TINT] ?: "NONE",
            isInvertedMode = prefs[KEY_INVERTED] ?: false,
            isSessionActive = prefs[KEY_SESSION_ACTIVE] ?: false,
            timelapseSnapshotCount = prefs[KEY_TIMELAPSE_COUNT] ?: 0,
            isTimelapseRecording = prefs[KEY_TIMELAPSE_RECORDING] ?: false,
            isTimelapsePaused = prefs[KEY_TIMELAPSE_PAUSED] ?: false,
            isOverlayLocked = prefs[KEY_OVERLAY_LOCKED] ?: false,
            viewportZoom = finiteOr(prefs[KEY_VIEWPORT_ZOOM], 1f).coerceIn(1f, 5f),
            viewportPanX = finiteOr(prefs[KEY_VIEWPORT_PAN_X], 0f),
            viewportPanY = finiteOr(prefs[KEY_VIEWPORT_PAN_Y], 0f)
        )
    }

    override suspend fun save(data: SessionData) {
        dataStore.edit { prefs ->
            prefs[KEY_SCHEMA_VERSION] = SCHEMA_VERSION
            if (data.imageUri != null) {
                prefs[KEY_IMAGE_URI] = data.imageUri
            } else {
                prefs.remove(KEY_IMAGE_URI)
            }
            prefs[KEY_OFFSET_X] = data.overlayOffsetX
            prefs[KEY_OFFSET_Y] = data.overlayOffsetY
            prefs[KEY_SCALE] = data.overlayScale
            prefs[KEY_ROTATION] = data.overlayRotation
            prefs[KEY_OPACITY] = data.overlayOpacity
            prefs[KEY_COLOR_TINT] = data.colorTint
            prefs[KEY_INVERTED] = data.isInvertedMode
            prefs[KEY_SESSION_ACTIVE] = data.isSessionActive
            prefs[KEY_TIMELAPSE_COUNT] = data.timelapseSnapshotCount
            prefs[KEY_TIMELAPSE_RECORDING] = data.isTimelapseRecording
            prefs[KEY_TIMELAPSE_PAUSED] = data.isTimelapsePaused
            prefs[KEY_OVERLAY_LOCKED] = data.isOverlayLocked
            prefs[KEY_VIEWPORT_ZOOM] = data.viewportZoom
            prefs[KEY_VIEWPORT_PAN_X] = data.viewportPanX
            prefs[KEY_VIEWPORT_PAN_Y] = data.viewportPanY
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs ->
            SESSION_KEYS.forEach { prefs.remove(it) }
        }
    }

    companion object {
        private fun finiteOr(value: Float?, default: Float): Float =
            if (value == null || !value.isFinite()) default else value

        private const val TAG = "DataStoreSessionRepo"
        private const val SCHEMA_VERSION = 1
        private const val RETRY_DELAY_MS = 2_000L

        private val KEY_SCHEMA_VERSION = intPreferencesKey("session_schema_version")
        private val KEY_IMAGE_URI = stringPreferencesKey("session_image_uri")
        private val KEY_OFFSET_X = floatPreferencesKey("session_offset_x")
        private val KEY_OFFSET_Y = floatPreferencesKey("session_offset_y")
        private val KEY_SCALE = floatPreferencesKey("session_scale")
        private val KEY_ROTATION = floatPreferencesKey("session_rotation")
        private val KEY_OPACITY = floatPreferencesKey("session_opacity")
        private val KEY_COLOR_TINT = stringPreferencesKey("session_color_tint")
        private val KEY_INVERTED = booleanPreferencesKey("session_inverted")
        private val KEY_SESSION_ACTIVE = booleanPreferencesKey("session_active")
        private val KEY_TIMELAPSE_COUNT = intPreferencesKey("session_timelapse_count")
        private val KEY_TIMELAPSE_RECORDING = booleanPreferencesKey("session_timelapse_recording")
        private val KEY_TIMELAPSE_PAUSED = booleanPreferencesKey("session_timelapse_paused")
        private val KEY_OVERLAY_LOCKED = booleanPreferencesKey("session_overlay_locked")
        private val KEY_VIEWPORT_ZOOM = floatPreferencesKey("session_viewport_zoom")
        private val KEY_VIEWPORT_PAN_X = floatPreferencesKey("session_viewport_pan_x")
        private val KEY_VIEWPORT_PAN_Y = floatPreferencesKey("session_viewport_pan_y")

        private val SESSION_KEYS: List<Preferences.Key<*>> = listOf(
            KEY_IMAGE_URI, KEY_OFFSET_X, KEY_OFFSET_Y, KEY_SCALE, KEY_ROTATION,
            KEY_OPACITY, KEY_COLOR_TINT, KEY_INVERTED, KEY_SESSION_ACTIVE,
            KEY_TIMELAPSE_COUNT, KEY_TIMELAPSE_RECORDING, KEY_TIMELAPSE_PAUSED,
            KEY_OVERLAY_LOCKED, KEY_VIEWPORT_ZOOM, KEY_VIEWPORT_PAN_X, KEY_VIEWPORT_PAN_Y
        )
    }
}
