package com.videoenhancer.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

data class PlayerState(
    val videoUri: Uri? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isFullScreen: Boolean = false
)

class PlayerViewModel : ViewModel() {

    var state by mutableStateOf(PlayerState())
        private set

    fun setVideo(uri: Uri) {
        state = state.copy(videoUri = uri)
    }

    fun clearVideo() {
        state = PlayerState()
    }

    fun setPlaying(playing: Boolean) {
        state = state.copy(isPlaying = playing)
    }

    fun setPosition(position: Long) {
        state = state.copy(currentPosition = position)
    }

    fun setDuration(duration: Long) {
        state = state.copy(duration = duration)
    }

    fun toggleFullScreen() {
        state = state.copy(isFullScreen = !state.isFullScreen)
    }
}
