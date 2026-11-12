package io.github.jn0v.traceglass.core.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override val settingsData: Flow<SettingsData> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e("SettingsRepo", "Failed to read settings, using defaults", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            SettingsData(
                audioFeedbackEnabled = prefs[KEY_AUDIO_FEEDBACK] ?: false,
                breakReminderEnabled = prefs[KEY_BREAK_REMINDER] ?: false,
                breakReminderIntervalMinutes = prefs[KEY_BREAK_INTERVAL] ?: 30,
                perspectiveCorrectionEnabled = prefs[KEY_PERSPECTIVE_CORRECTION] ?: true,
                gridMode = prefs[KEY_GRID_MODE]
                    ?.let { name -> GridMode.entries.firstOrNull { it.name == name } }
                    ?: GridMode.OFF,
                autoStartTimelapse = prefs[KEY_AUTO_START_TIMELAPSE] ?: true
            )
        }

    override suspend fun setAudioFeedbackEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_AUDIO_FEEDBACK] = enabled }
    }

    override suspend fun setBreakReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_BREAK_REMINDER] = enabled }
    }

    override suspend fun setBreakReminderIntervalMinutes(minutes: Int) {
        dataStore.edit { it[KEY_BREAK_INTERVAL] = minutes }
    }

    override suspend fun setPerspectiveCorrectionEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_PERSPECTIVE_CORRECTION] = enabled }
    }

    override suspend fun setGridMode(mode: GridMode) {
        dataStore.edit { it[KEY_GRID_MODE] = mode.name }
    }

    override suspend fun setAutoStartTimelapse(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_START_TIMELAPSE] = enabled }
    }

    companion object {
        private val KEY_AUTO_START_TIMELAPSE = booleanPreferencesKey("settings_auto_start_timelapse")
        private val KEY_AUDIO_FEEDBACK = booleanPreferencesKey("settings_audio_feedback")
        private val KEY_BREAK_REMINDER = booleanPreferencesKey("settings_break_reminder")
        private val KEY_BREAK_INTERVAL = intPreferencesKey("settings_break_interval")
        private val KEY_GRID_MODE = stringPreferencesKey("settings_grid_mode")
        private val KEY_PERSPECTIVE_CORRECTION = booleanPreferencesKey("settings_perspective_correction")
    }
}
