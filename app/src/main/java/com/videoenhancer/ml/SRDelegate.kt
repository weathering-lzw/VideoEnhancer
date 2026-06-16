package com.videoenhancer.ml

import android.content.Context
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate

/**
 * TensorFlow Lite 委托配置
 *
 * 管理 GPU/NNAPI 加速委托，优化 AI 超分推理性能
 */
object SRDelegate {

    /**
     * 创建 GPU 委托（在支持的设备上显著加速推理）
     */
    fun createGpuDelegate(): GpuDelegate? {
        return try {
            if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                GpuDelegate(
                    GpuDelegate.Options().apply {
                        // 允许 FP16 精度（更快，精度损失可忽略）
                        setPrecisionLossAllowed(true)
                        // 推断时优选 GPU
                        setInferencePreference(
                            GpuDelegate.Options.INFERENCE_PREFERENCE_SUSTAINED_SPEED
                        )
                    }
                )
            } else {
                null // GPU delegate not supported
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 检查当前设备是否支持 GPU 加速
     */
    fun isGpuSupported(): Boolean {
        return try {
            CompatibilityList().isDelegateSupportedOnThisDevice
        } catch (e: Exception) {
            false
        }
    }
}
