package io.github.jn0v.traceglass.core.session

import io.github.jn0v.traceglass.core.overlay.CompositionGrid

data class SettingsData(
    val audioFeedbackEnabled: Boolean = false,
    val breakReminderEnabled: Boolean = false,
    val breakReminderIntervalMinutes: Int = 30,
    val perspectiveCorrectionEnabled: Boolean = true,
    val grid: CompositionGrid = CompositionGrid.Off
)
