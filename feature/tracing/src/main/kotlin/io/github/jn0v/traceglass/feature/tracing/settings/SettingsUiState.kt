package io.github.jn0v.traceglass.feature.tracing.settings

import io.github.jn0v.traceglass.core.session.GridMode

data class SettingsUiState(
    val audioFeedbackEnabled: Boolean = false,
    val breakReminderEnabled: Boolean = false,
    val breakReminderIntervalMinutes: Int = 30,
    val perspectiveCorrectionEnabled: Boolean = true,
    val gridMode: GridMode = GridMode.OFF
)
