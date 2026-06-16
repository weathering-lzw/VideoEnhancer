package com.videoenhancer.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * AI 超分辨率推理引擎
 *
 * 使用 TensorFlow Lite 模型对视频帧进行端侧超分辨率处理。
 * 支持 2x / 4x 放大倍数。
 *
 * 模型需放置在 assets/ 目录下，支持格式：
 * - fsrcnn_2x.tflite  — FSRCNN 2x 超分模型（推荐，速度快）
 * - esrgan_lite_4x.tflite — ESRGAN-lite 4x 超分模型（画质好，速度慢）
 */
class SuperResolutionModel(private val context: Context) {

    companion object {
        private const val MODEL_2X = "fsrcnn_2x.tflite"
        private const val MODEL_4X = "esrgan_lite_4x.tflite"

        /** 输入帧尺寸限制（过大帧会缩放后再推理） */
        private const val MAX_INPUT_SIZE = 640

        /** TFLite 模型输入 tensor 尺寸 */
        private const val INPUT_SIZE = 256
        private const val OUTPUT_SIZE_2X = 512 // 256 * 2
        private const val OUTPUT_SIZE_4X = 1024 // 256 * 4
    }

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var currentScaleFactor = 2

    /**
     * 加载超分模型
     *
     * @param scaleFactor 放大倍数（2 或 4）
     */
    fun loadModel(scaleFactor: Int): Boolean {
        return try {
            currentScaleFactor = scaleFactor
            val modelName = when (scaleFactor) {
                4 -> MODEL_4X
                else -> MODEL_2X
            }

            // 从 assets 加载模型
            val modelBuffer = loadModelFile(modelName) ?: return false

            // 创建 GPU delegate（支持时）
            gpuDelegate = SRDelegate.createGpuDelegate()

            // 配置 interpreter
            val options = Interpreter.Options().apply {
                setNumThreads(4) // 4 线程并行
                gpuDelegate?.let { addDelegate(it) }
            }

            interpreter = Interpreter(modelBuffer, options)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 对一帧进行超分辨率处理
     *
     * @param input 输入位图（建议 256x256，会被自动裁剪/缩放）
     * @return 超分后的位图（512x512 或 1024x1024）
     */
    fun process(input: Bitmap): Bitmap? {
        val interp = interpreter ?: return null

        val outputSize = if (currentScaleFactor == 4) OUTPUT_SIZE_4X else OUTPUT_SIZE_2X

        // 预处理：缩放/裁剪到模型输入尺寸
        val processedInput = Bitmap.createScaledBitmap(
            input, INPUT_SIZE, INPUT_SIZE, true
        )

        // 转换为 ByteBuffer（RGB 归一化到 [-1, 1]）
        val inputBuffer = bitmapToByteBuffer(processedInput)

        // 输出 buffer
        val outputBuffer = ByteBuffer.allocateDirect(1 * outputSize * outputSize * 3 * 4)
        outputBuffer.order(ByteOrder.nativeOrder())

        // 推理
        interp.run(inputBuffer, outputBuffer)

        // 后处理：将输出 buffer 转换为 Bitmap
        return byteBufferToBitmap(outputBuffer, outputSize, outputSize)
    }

    /**
     * 处理一帧的 ByteBuffer 输入（用于视频帧管线）
     */
    fun processBuffer(inputBuffer: ByteBuffer, width: Int, height: Int): ByteBuffer? {
        val interp = interpreter ?: return null

        val outputSize = if (currentScaleFactor == 4) OUTPUT_SIZE_4X else OUTPUT_SIZE_2X

        val outputBuffer = ByteBuffer.allocateDirect(1 * outputSize * outputSize * 3 * 4)
        outputBuffer.order(ByteOrder.nativeOrder())

        interp.run(inputBuffer, outputBuffer)

        return outputBuffer
    }

    /**
     * 释放模型资源
     */
    fun release() {
        interpreter?.close()
        interpreter = null
        gpuDelegate?.close()
        gpuDelegate = null
    }

    /**
     * 检查模型是否已加载
     */
    fun isLoaded(): Boolean = interpreter != null

    // --- 私有辅助方法 ---

    private fun loadModelFile(modelName: String): MappedByteBuffer? {
        return try {
            val assetFileDescriptor = context.assets.openFd(modelName)
            val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4)
        buffer.order(ByteOrder.nativeOrder())
        buffer.rewind()

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f
            // 归一化到 [-1, 1]
            buffer.putFloat(r * 2f - 1f)
            buffer.putFloat(g * 2f - 1f)
            buffer.putFloat(b * 2f - 1f)
        }

        buffer.rewind()
        return buffer
    }

    private fun byteBufferToBitmap(buffer: ByteBuffer, width: Int, height: Int): Bitmap {
        buffer.rewind()

        val pixels = IntArray(width * height)
        for (i in pixels.indices) {
            val r = ((buffer.getFloat() + 1f) / 2f * 255f).toInt().coerceIn(0, 255)
            val g = ((buffer.getFloat() + 1f) / 2f * 255f).toInt().coerceIn(0, 255)
            val b = ((buffer.getFloat() + 1f) / 2f * 255f).toInt().coerceIn(0, 255)
            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }
}
