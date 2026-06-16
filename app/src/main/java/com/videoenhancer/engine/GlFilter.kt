package com.videoenhancer.engine

import android.opengl.GLES30
import java.nio.FloatBuffer

/**
 * OpenGL ES 3.0 滤镜基类
 *
 * 管理着色器程序生命周期，提供统一的渲染接口
 */
open class GlFilter(
    vertexShaderCode: String,
    fragmentShaderCode: String
) {
    protected var programId = 0
    private var vboId = 0
    private var vaoId = 0

    /** 全屏四边形顶点（两个三角形组成矩形） */
    private val vertexData = floatArrayOf(
        // 位置 (x, y)   纹理坐标 (s, t)
        -1f, -1f, 0f, 0f,  // 左下
         1f, -1f, 1f, 0f,  // 右下
        -1f,  1f, 0f, 1f,  // 左上
         1f,  1f, 1f, 1f   // 右上
    )

    private val vertexBuffer: FloatBuffer = createFloatBuffer(vertexData)

    init {
        initGL()
    }

    private fun initGL() {
        // 创建着色器程序
        programId = createProgram(vertexShaderCode, fragmentShaderCode)

        // 生成 VAO 和 VBO
        val vaoArray = IntArray(1)
        val vboArray = IntArray(1)
        GLES30.glGenVertexArrays(1, vaoArray, 0)
        GLES30.glGenBuffers(1, vboArray, 0)
        vaoId = vaoArray[0]
        vboId = vboArray[0]

        // 绑定 VAO
        GLES30.glBindVertexArray(vaoId)

        // 上传顶点数据
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        vertexBuffer.position(0)
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            vertexBuffer.capacity() * 4,
            vertexBuffer,
            GLES30.GL_STATIC_DRAW
        )

        // 位置属性 (location=0)
        GLES30.glVertexAttribPointer(
            0, 2, GLES30.GL_FLOAT, false, 16, 0
        )
        GLES30.glEnableVertexAttribArray(0)

        // 纹理坐标属性 (location=1)
        GLES30.glVertexAttribPointer(
            1, 2, GLES30.GL_FLOAT, false, 16, 8
        )
        GLES30.glEnableVertexAttribArray(1)

        // 解绑
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glBindVertexArray(0)
    }

    /**
     * 渲染一帧到当前绑定的帧缓冲
     *
     * @param textureId 输入纹理 ID
     * @param uniforms 着色器 uniform 值数组
     */
    fun draw(textureId: Int, uniforms: FloatArray = floatArrayOf()) {
        GLES30.glUseProgram(programId)

        // 绑定纹理
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)

        // 设置 uniform
        val textureUniform = GLES30.glGetUniformLocation(programId, "uTexture")
        GLES30.glUniform1i(textureUniform, 0)

        // 传递增强参数
        if (uniforms.isNotEmpty()) {
            val brightnessLoc = GLES30.glGetUniformLocation(programId, "uBrightness")
            val contrastLoc = GLES30.glGetUniformLocation(programId, "uContrast")
            val saturationLoc = GLES30.glGetUniformLocation(programId, "uSaturation")
            val sharpnessLoc = GLES30.glGetUniformLocation(programId, "uSharpness")
            val zoomLoc = GLES30.glGetUniformLocation(programId, "uZoom")
            val enabledLoc = GLES30.glGetUniformLocation(programId, "uEnabled")

            if (uniforms.size > 0) GLES30.glUniform1f(brightnessLoc, uniforms[0])
            if (uniforms.size > 1) GLES30.glUniform1f(contrastLoc, uniforms[1])
            if (uniforms.size > 2) GLES30.glUniform1f(saturationLoc, uniforms[2])
            if (uniforms.size > 3) GLES30.glUniform1f(sharpnessLoc, uniforms[3])
            if (uniforms.size > 4) GLES30.glUniform1f(zoomLoc, uniforms[4])
            if (uniforms.size > 5) GLES30.glUniform1f(enabledLoc, uniforms[5])
        }

        // 设置分辨率 uniform（用于锐化卷积）
        val resolutionLoc = GLES30.glGetUniformLocation(programId, "uResolution")
        // 需要有实际分辨率值传入，此处简化处理

        // 绘制全屏四边形
        GLES30.glBindVertexArray(vaoId)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindVertexArray(0)

        GLES30.glUseProgram(0)
    }

    fun release() {
        if (programId != 0) {
            GLES30.glDeleteProgram(programId)
            programId = 0
        }
        if (vboId != 0) {
            GLES30.glDeleteBuffers(1, intArrayOf(vboId), 0)
            vboId = 0
        }
        if (vaoId != 0) {
            GLES30.glDeleteVertexArrays(1, intArrayOf(vaoId), 0)
            vaoId = 0
        }
    }

    companion object {
        /** 默认顶点着色器 */
        const val DEFAULT_VERTEX_SHADER = """
            #version 300 es
            layout(location = 0) in vec4 aPosition;
            layout(location = 1) in vec2 aTexCoord;
            out vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = aTexCoord;
            }
        """

        /** 完整增强片段着色器（合并所有效果为单 pass） */
        const val ENHANCE_FRAGMENT_SHADER = """
            #version 300 es
            precision highp float;
            in vec2 vTexCoord;
            out vec4 fragColor;

            uniform sampler2D uTexture;
            uniform float uBrightness;
            uniform float uContrast;
            uniform float uSaturation;
            uniform float uSharpness;
            uniform float uZoom;
            uniform float uEnabled;
            uniform vec2 uResolution;

            // 拉普拉斯锐化核
            float sharpen(vec2 uv, vec2 texelSize) {
                vec4 center = texture(uTexture, uv);
                
                vec4 left   = texture(uTexture, uv - vec2(texelSize.x, 0.0));
                vec4 right  = texture(uTexture, uv + vec2(texelSize.x, 0.0));
                vec4 top    = texture(uTexture, uv - vec2(0.0, texelSize.y));
                vec4 bottom = texture(uTexture, uv + vec2(0.0, texelSize.y));

                vec4 laplacian = center * 5.0 - left - right - top - bottom;
                return clamp((center + laplacian * uSharpness * 0.3).r, 0.0, 1.0);
            }

            // RGB 转 HSL 亮度分量
            float getLuminance(vec3 c) {
                return dot(c, vec3(0.299, 0.587, 0.114));
            }

            void main() {
                // 放大：通过调整纹理坐标实现数字变焦
                vec2 zoomedCoord = (vTexCoord - 0.5) / uZoom + 0.5;
                
                // 边缘 clamp（防止缩放时显示边缘外内容）
                if (zoomedCoord.x < 0.0 || zoomedCoord.x > 1.0 ||
                    zoomedCoord.y < 0.0 || zoomedCoord.y > 1.0) {
                    fragColor = vec4(0.0, 0.0, 0.0, 1.0);
                    return;
                }

                vec4 color = texture(uTexture, zoomedCoord);

                if (uEnabled < 0.5) {
                    fragColor = color;
                    return;
                }

                // 1. 亮度调节
                vec3 result = color.rgb + uBrightness;

                // 2. 对比度调节
                result = (result - 0.5) * uContrast + 0.5;

                // 3. 饱和度调节
                float luminance = getLuminance(result);
                result = mix(vec3(luminance), result, uSaturation);

                // 4. 锐度
                if (uSharpness > 0.001) {
                    vec2 texelSize = 1.0 / uResolution;
                    float r = sharpen(zoomedCoord, texelSize);
                    // 仅对亮度通道锐化，保留颜色
                    float origLum = getLuminance(result);
                    result = result * (1.0 + (r - origLum) * uSharpness * 0.5);
                }

                // 钳制到有效范围
                result = clamp(result, 0.0, 1.0);

                fragColor = vec4(result, color.a);
            }
        """

        /** 纯放大（无其他增强）的片段着色器 */
        const val ZOOM_FRAGMENT_SHADER = """
            #version 300 es
            precision highp float;
            in vec2 vTexCoord;
            out vec4 fragColor;

            uniform sampler2D uTexture;
            uniform float uZoom;

            void main() {
                vec2 zoomedCoord = (vTexCoord - 0.5) / uZoom + 0.5;
                if (zoomedCoord.x < 0.0 || zoomedCoord.x > 1.0 ||
                    zoomedCoord.y < 0.0 || zoomedCoord.y > 1.0) {
                    fragColor = vec4(0.0, 0.0, 0.0, 1.0);
                    return;
                }
                fragColor = texture(uTexture, zoomedCoord);
            }
        """

        private fun createProgram(vertexCode: String, fragmentCode: String): Int {
            val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexCode)
            val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentCode)

            val program = GLES30.glCreateProgram()
            GLES30.glAttachShader(program, vertexShader)
            GLES30.glAttachShader(program, fragmentShader)
            GLES30.glLinkProgram(program)

            // 检查链接状态
            val status = IntArray(1)
            GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, status, 0)
            if (status[0] == 0) {
                val log = GLES30.glGetProgramInfoLog(program)
                GLES30.glDeleteProgram(program)
                throw RuntimeException("Shader program link failed: $log")
            }

            // 链接完成后可以删除着色器
            GLES30.glDeleteShader(vertexShader)
            GLES30.glDeleteShader(fragmentShader)

            return program
        }

        private fun loadShader(type: Int, code: String): Int {
            val shader = GLES30.glCreateShader(type)
            GLES30.glShaderSource(shader, code)
            GLES30.glCompileShader(shader)

            val status = IntArray(1)
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
            if (status[0] == 0) {
                val log = GLES30.glGetShaderInfoLog(shader)
                GLES30.glDeleteShader(shader)
                throw RuntimeException("Shader compile failed: $log")
            }

            return shader
        }

        private fun createFloatBuffer(data: FloatArray): FloatBuffer {
            val buffer = java.nio.ByteBuffer
                .allocateDirect(data.size * 4)
                .order(java.nio.ByteOrder.nativeOrder())
                .asFloatBuffer()
            buffer.put(data)
            buffer.flip()
            return buffer
        }
    }
}
