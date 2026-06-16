package com.videoenhancer.engine

import android.content.Context
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLSurfaceView
import android.view.Surface

/**
 * 视频增强引擎 — 核心协调器
 *
 * 负责管理 GL 线程、连接 ExoPlayer 输出与 OpenGL 渲染管线，
 * 协调帧接收 → 增强处理 → 显示输出的全流程。
 *
 * 使用流程：
 * 1. 创建 VideoEnhancementEngine 实例
 * 2. 调用 start() 启动 GL 线程
 * 3. 获取 Surface 传递给 ExoPlayer
 * 4. 每帧通过 setUniforms 更新参数
 * 5. 调用 stop() 释放资源
 */
class VideoEnhancementEngine(private val context: Context) {

    private var glRenderer: GlRenderer? = null
    private var glSurfaceView: GLSurfaceView? = null
    private var isRunning = false

    /**
     * 启动增强引擎（需在 GL 线程上执行）
     *
     * @param outputSurface 用于显示输出内容的 Surface（可以是 SurfaceView 的 Surface）
     */
    fun start(outputSurface: Surface? = null) {
        if (isRunning) return
        isRunning = true

        // 创建 GLSurfaceView（用于提供 GL 上下文和渲染循环）
        glSurfaceView = object : GLSurfaceView(context) {
            override fun onDetachedFromWindow() {
                super.onDetachedFromWindow()
            }
        }.apply {
            setEGLContextClientVersion(3) // OpenGL ES 3.0
            setRenderer(GlRenderer().also { glRenderer = it })
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    /**
     * 获取可用于 ExoPlayer 的输入 Surface
     */
    fun getInputSurface(): Surface? {
        return glRenderer?.getSurface()
    }

    /**
     * 获取 SurfaceTexture（用于 MediaCodec 解码器）
     */
    fun getSurfaceTexture(): android.graphics.SurfaceTexture? {
        return glRenderer?.getSurfaceTexture()
    }

    /**
     * 更新增强参数
     */
    fun updateParams(
        brightness: Float = 0f,
        contrast: Float = 1f,
        saturation: Float = 1f,
        sharpness: Float = 0f,
        zoom: Float = 1f,
        enabled: Boolean = true
    ) {
        glRenderer?.setUniforms(brightness, contrast, saturation, sharpness, zoom, enabled)
    }

    /**
     * 设置帧可用回调
     */
    fun setOnFrameAvailableListener(listener: GlRenderer.OnFrameAvailableListener) {
        glRenderer?.setOnFrameAvailableListener(listener)
    }

    /**
     * 请求渲染一帧
     */
    fun requestRender() {
        glSurfaceView?.requestRender()
    }

    /**
     * 停止引擎并释放所有 GL 资源
     */
    fun stop() {
        isRunning = false
        glRenderer?.release()
        glRenderer = null
        glSurfaceView?.let {
            try {
                it.queueEvent {
                    // GL 资源清理
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        glSurfaceView = null
    }
}
