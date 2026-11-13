package io.github.jn0v.traceglass.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SetupGuideViewModel(
    widestZoomRatio: StateFlow<Float?>
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SetupGuideUiState(showNarrowViewNotice = isNarrow(widestZoomRatio.value))
    )
    val uiState: StateFlow<SetupGuideUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            widestZoomRatio.collect { zoom ->
                _uiState.update { it.copy(showNarrowViewNotice = isNarrow(zoom)) }
            }
        }
    }

    fun onSectionSelected(section: SetupGuideSection) {
        _uiState.update { it.copy(selectedSection = section) }
    }

    companion object {
        private fun isNarrow(zoom: Float?): Boolean = zoom != null && zoom >= 1.0f


        const val MARKER_SHEET_URL =
            "https://github.com/jn0v/traceglass/releases/latest/download/markers.pdf"
        const val STAND_MODEL_URL =
            "https://github.com/jn0v/traceglass/releases/latest/download/phone-stand.stl"
    }
}
