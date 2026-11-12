package io.github.jn0v.traceglass.core.session

enum class GridMode { OFF, THIRDS, GOLDEN_RATIO }

data class SettingsData(
    val audioFeedbackEnabled: Boolean = false,
    val breakReminderEnabled: Boolean = false,
    val breakReminderIntervalMinutes: Int = 30,
    val perspectiveCorrectionEnabled: Boolean = true,
    val gridMode: GridMode = GridMode.OFF
)
