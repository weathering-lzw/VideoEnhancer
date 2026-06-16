package com.videoenhancer.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class BubbleSettings(
    val edgeSnapEnabled: Boolean = true,
    val bubbleOpacity: Float = 0.5f,
    val autoHideEnabled: Boolean = false,
    val autoHideTimeoutMs: Long = 5000L
)

class BubbleViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = application.let {
        androidx.datastore.preferences.preferencesDataStore(
            name = "bubble_settings"
        )
    }

    var isServiceRunning by mutableStateOf(false)
        private set

    var settings by mutableStateOf(BubbleSettings())
        private set

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            // Use DataStore to load settings
            // For now, use defaults
            settings = BubbleSettings()
        }
    }

    fun setServiceRunning(running: Boolean) {
        isServiceRunning = running
    }

    fun updateEdgeSnap(enabled: Boolean) {
        settings = settings.copy(edgeSnapEnabled = enabled)
    }

    fun updateBubbleOpacity(opacity: Float) {
        settings = settings.copy(bubbleOpacity = opacity)
    }

    fun updateAutoHide(enabled: Boolean) {
        settings = settings.copy(autoHideEnabled = enabled)
    }
}
