package com.videoenhancer.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.view.MotionEvent
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import com.videoenhancer.R

/**
 * 屏幕捕获服务 — 放大镜模式
 *
 * 使用 MediaProjection API 捕获屏幕内容，在悬浮窗口中实时放大显示。
 * 用户可以通过此模式放大任何应用的内容。
 */
class ScreenCaptureService : Service() {

    companion object {
        const val CHANNEL_ID = "screen_capture"
        const val NOTIFICATION_ID = 1002

        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"

        private const val MAGNIFIER_SIZE_DP = 200
        private const val MAX_ZOOM = 5f
        private const val MIN_ZOOM = 1.5f

        fun start(
            context: Context,
            resultCode: Int,
            data: Intent?
        ) {
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreenCaptureService::class.java))
        }
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private lateinit var windowManager: WindowManager
    private lateinit var containerView: FrameLayout
    private lateinit var params: WindowManager.LayoutParams
    private var isDragging = false
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private val mainHandler = Handler(Looper.getMainLooper())
    private var zoomLevel = 2.0f

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, -1) ?: -1
        val data = intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)

        if (resultCode == -1 || data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification())
        setupMediaProjection(resultCode, data)
        createMagnifierWindow()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        releaseMediaProjection()
        removeMagnifierWindow()
        super.onDestroy()
    }

    private fun setupMediaProjection(resultCode: Int, data: Intent) {
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        val density = resources.displayMetrics.density
        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels

        // 创建 ImageReader 用于接收屏幕帧
        val captureWidth = (width / 2).coerceAtMost(720)
        val captureHeight = (height / 2).coerceAtMost(1280)

        imageReader = ImageReader.newInstance(
            captureWidth,
            captureHeight,
            PixelFormat.RGBA_8888,
            2
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            // 处理捕获到的帧 → 渲染到放大镜窗口
            reader.acquireLatestImage()?.let { image ->
                // 将 Image 转换为 OpenGL 纹理并渲染
                // (实际实现中会将此帧传递给 GL 渲染器进行缩放处理)
                image.close()
            }
        }, mainHandler)

        // 创建 VirtualDisplay
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "MagnifierCapture",
            captureWidth,
            captureHeight,
            resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            mainHandler
        )
    }

    private fun createMagnifierWindow() {
        val density = resources.displayMetrics.density
        val sizePx = (MAGNIFIER_SIZE_DP * density).toInt()

        containerView = FrameLayout(this).apply {
            setBackgroundColor(0xCC1A1A1A) // 半透明深色背景
            // 添加显示放大内容的 SurfaceView / TextureView
            // (实际实现中会用 OpenGL 渲染捕获的屏幕帧)
        }

        // 添加放大倍率指示
        val zoomIndicator = View(this).apply {
            // 简化：实际会显示文字指示
        }

        params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (16 * density).toInt()
            y = resources.displayMetrics.heightPixels / 4
        }

        // 触摸事件（拖动放大镜窗口）
        containerView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = false
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(event.rawX - initialTouchX) > 10 ||
                        Math.abs(event.rawY - initialTouchY) > 10
                    ) {
                        isDragging = true
                    }
                    if (isDragging) {
                        params.x = initialX + deltaX
                        params.y = initialY + deltaY
                        windowManager.updateViewLayout(containerView, params)
                    }
                }

                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // 点击放大镜窗口：切换放大倍率
                        zoomLevel = if (zoomLevel >= MAX_ZOOM) MIN_ZOOM else zoomLevel + 0.5f
                        updateZoomLevel()
                    }
                }
            }
            true
        }

        windowManager.addView(containerView, params)
    }

    private fun removeMagnifierWindow() {
        if (::containerView.isInitialized && containerView.isAttachedToWindow) {
            try {
                windowManager.removeView(containerView)
            } catch (e: IllegalArgumentException) {
                // Already removed
            }
        }
    }

    private fun updateZoomLevel() {
        // 更新放大倍率并通知渲染器
    }

    private fun releaseMediaProjection() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("放大镜已开启")
            .setContentText("点击切换放大倍率")
            .setSmallIcon(R.drawable.ic_bubble)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
