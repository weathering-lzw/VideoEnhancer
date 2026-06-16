package com.videoenhancer.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

data class EnhancementParams(
    val brightness: Float = 0f,      // -1.0 ~ 1.0
    val contrast: Float = 1f,        // 0.0 ~ 2.0
    val saturation: Float = 1f,      // 0.0 ~ 2.0
    val sharpness: Float = 0f,       // 0.0 ~ 2.0
    val zoom: Float = 1f,            // 1.0 ~ 5.0
)

enum class EnhancementPreset(val label: String) {
    ORIGINAL("原始"),
    VIVID("鲜艳"),
    SOFT("柔和"),
    CINEMA("电影");
}

class EnhancementViewModel : ViewModel() {

    var params by mutableStateOf(EnhancementParams())
        private set

    var activePreset by mutableStateOf(EnhancementPreset.ORIGINAL)
        private set

    var isEnhancementEnabled by mutableStateOf(true)
        private set

    var aiSuperResolution by mutableStateOf(false)
        private set

    var aiSRFactor by mutableFloatStateOf(2f) // 2x or 4x
        private set

    // --- Presets ---
    fun applyPreset(preset: EnhancementPreset) {
        activePreset = preset
        params = when (preset) {
            EnhancementPreset.ORIGINAL -> EnhancementParams()
            EnhancementPreset.VIVID -> EnhancementParams(
                brightness = 0.05f,
                contrast = 1.2f,
                saturation = 1.4f,
                sharpness = 0.3f
            )
            EnhancementPreset.SOFT -> EnhancementParams(
                brightness = 0.1f,
                contrast = 0.85f,
                saturation = 0.8f,
                sharpness = 0f
            )
            EnhancementPreset.CINEMA -> EnhancementParams(
                brightness = -0.05f,
                contrast = 1.3f,
                saturation = 0.7f,
                sharpness = 0.5f
            )
        }
    }

    // --- Individual parameter overrides (breaks preset selection) ---
    fun setBrightness(value: Float) {
        activePreset = EnhancementPreset.ORIGINAL
        params = params.copy(brightness = value.coerceIn(-1f, 1f))
    }

    fun setContrast(value: Float) {
        activePreset = EnhancementPreset.ORIGINAL
        params = params.copy(contrast = value.coerceIn(0f, 2f))
    }

    fun setSaturation(value: Float) {
        activePreset = EnhancementPreset.ORIGINAL
        params = params.copy(saturation = value.coerceIn(0f, 2f))
    }

    fun setSharpness(value: Float) {
        activePreset = EnhancementPreset.ORIGINAL
        params = params.copy(sharpness = value.coerceIn(0f, 2f))
    }

    fun setZoom(value: Float) {
        params = params.copy(zoom = value.coerceIn(1f, 5f))
    }

    fun toggleEnhancement() {
        isEnhancementEnabled = !isEnhancementEnabled
    }

    fun toggleAI() {
        aiSuperResolution = !aiSuperResolution
    }

    fun setAIFactor(factor: Float) {
        aiSRFactor = factor
    }

    /** Get combined shader uniform values as float array */
    fun toShaderUniforms(): FloatArray = floatArrayOf(
        params.brightness,
        params.contrast,
        params.saturation,
        params.sharpness,
        params.zoom,
        if (isEnhancementEnabled) 1f else 0f
    )
}
