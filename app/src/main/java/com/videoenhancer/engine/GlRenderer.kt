package com.videoenhancer.engine

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.view.Surface
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.opengles.GL10

/**
 * OpenGL ES 渲染器
 *
 * 负责管理 GL 上下文、帧缓冲对象（FBO）、纹理对象，
 * 并驱动 GlFilter 进行逐帧渲染。
 *
 * 此渲染器可用于：
 * 1. GLSurfaceView.Renderer — 在 Activity 中显示
 * 2. 离屏渲染 — 处理视频帧后输出到 MediaCodec
 */
class GlRenderer : GLSurfaceView.Renderer {

    private var enhanceFilter: GlFilter? = null
    private var fboId = 0
    private var outputTextureId = 0
    private var screenWidth = 0
    private var screenHeight = 0

    /** 外部纹理 — 来自 MediaCodec/SurfaceTexture 的输出 */
    private var externalTextureId = 0
    private var externalSurfaceTexture: SurfaceTexture? = null

    /** 当前 uniform 参数 */
    private var uniforms = FloatArray(6) // brightness, contrast, saturation, sharpness, zoom, enabled

    interface OnFrameAvailableListener {
        fun onFrameAvailable()
    }

    private var frameListener: OnFrameAvailableListener? = null

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // 设置清除色
        GLES30.glClearColor(0f, 0f, 0f, 1f)

        // 初始化增强滤镜
        enhanceFilter = GlFilter(
            GlFilter.DEFAULT_VERTEX_SHADER,
            GlFilter.ENHANCE_FRAGMENT_SHADER
        )

        // 生成外部纹理（用于接收 SurfaceTexture 输入）
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        externalTextureId = textures[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_EXTERNAL_OES, externalTextureId)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        // 创建 SurfaceTexture
        externalSurfaceTexture = SurfaceTexture(externalTextureId).apply {
            setOnFrameAvailableListener {
                frameListener?.onFrameAvailable()
            }
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        GLES30.glViewport(0, 0, width, height)

        // 创建 FBO 用于离屏渲染
        setupFBO(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // 更新外部纹理（从 SurfaceTexture 获取最新帧）
        externalSurfaceTexture?.updateTexImage()

        // 清除
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        // 使用增强滤镜渲染
        enhanceFilter?.draw(externalTextureId, uniforms)
    }

    /**
     * 设置渲染参数
     */
    fun setUniforms(brightness: Float, contrast: Float, saturation: Float,
                    sharpness: Float, zoom: Float, enabled: Boolean) {
        uniforms = floatArrayOf(
            brightness, contrast, saturation, sharpness, zoom,
            if (enabled) 1f else 0f
        )
    }

    /**
     * 获取 SurfaceTexture（用于连接 MediaCodec/ExoPlayer 输出）
     */
    fun getSurfaceTexture(): SurfaceTexture? = externalSurfaceTexture

    /**
     * 获取 Surface（用于 ExoPlayer 的 setOutputSurface）
     */
    fun getSurface(): Surface? {
        return externalSurfaceTexture?.let { Surface(it) }
    }

    /**
     * 设置帧可用监听器
     */
    fun setOnFrameAvailableListener(listener: OnFrameAvailableListener) {
        frameListener = listener
    }

    /**
     * 释放资源
     */
    fun release() {
        enhanceFilter?.release()
        enhanceFilter = null

        if (externalTextureId != 0) {
            GLES30.glDeleteTextures(1, intArrayOf(externalTextureId), 0)
            externalTextureId = 0
        }

        if (fboId != 0) {
            GLES30.glDeleteFramebuffers(1, intArrayOf(fboId), 0)
            fboId = 0
        }

        if (outputTextureId != 0) {
            GLES30.glDeleteTextures(1, intArrayOf(outputTextureId), 0)
            outputTextureId = 0
        }

        externalSurfaceTexture?.release()
        externalSurfaceTexture = null
    }

    private fun setupFBO(width: Int, height: Int) {
        // 生成输出纹理
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        outputTextureId = textures[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, outputTextureId)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA,
            width, height, 0,
            GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null
        )
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)

        // 生成 FBO
        val fbos = IntArray(1)
        GLES30.glGenFramebuffers(1, fbos, 0)
        fboId = fbos[0]

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboId)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D, outputTextureId, 0
        )

        // 检查 FBO 完整性
        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            throw RuntimeException("FBO incomplete: $status")
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }
}
