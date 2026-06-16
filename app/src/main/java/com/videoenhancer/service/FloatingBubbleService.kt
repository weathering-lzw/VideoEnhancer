package com.videoenhancer.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.videoenhancer.MainActivity
import com.videoenhancer.R
import com.videoenhancer.util.EdgeSnapHelper
import com.videoenhancer.util.PermissionManager

/**
 * 悬浮球前台服务
 *
 * 在屏幕上方创建一个可拖拽的小圆形按钮，用于快速控制视频增强功能。
 * 点击展开控制面板，拖拽可移动并自动吸附到屏幕边缘。
 */
class FloatingBubbleService : Service() {

    companion object {
        const val CHANNEL_ID = "floating_bubble"
        const val NOTIFICATION_ID = 1001

        /** 悬浮球直径 (dp) */
        private const val BUBBLE_SIZE_DP = 52

        /** 拖动 vs 点击的判定阈值 (px) */
        private const val CLICK_THRESHOLD = 10

        /** Intent 动作 */
        const val ACTION_START = "com.videoenhancer.action.START_BUBBLE"
        const val ACTION_STOP = "com.videoenhancer.action.STOP_BUBBLE"

        fun start(context: Context) {
            val intent = Intent(context, FloatingBubbleService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingBubbleService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: ImageView
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var edgeSnapHelper: EdgeSnapHelper

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (!PermissionManager.hasOverlayPermission(this)) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                if (!::bubbleView.isInitialized) {
                    createBubble()
                }
                startForeground(NOTIFICATION_ID, createNotification())
            }

            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        removeBubble()
        super.onDestroy()
    }

    private fun createBubble() {
        val density = resources.displayMetrics.density
        val bubbleSizePx = (BUBBLE_SIZE_DP * density).toInt()

        // 创建悬浮球视图
        bubbleView = ImageView(this).apply {
            setImageResource(R.drawable.ic_bubble)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(
                (8 * density).toInt(),
                (8 * density).toInt(),
                (8 * density).toInt(),
                (8 * density).toInt()
            )
            // 通过背景绘制实现毛玻璃效果
            setBackgroundResource(android.R.color.transparent)
            // 设置圆形裁剪
            clipToOutline = false
            alpha = 0.5f // 初始透明度 50%
            contentDescription = "画质增强悬浮球"

            setOnClickListener {
                // 点击事件由 Touch 事件中的 ACTION_UP 处理
            }
        }

        // 窗口布局参数
        params = WindowManager.LayoutParams(
            bubbleSizePx,
            bubbleSizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = resources.displayMetrics.widthPixels - bubbleSizePx - (16 * density).toInt() // 默认右侧
            y = resources.displayMetrics.heightPixels / 3
        }

        // 触摸事件处理
        bubbleView.setOnTouchListener { _, event ->
            onTouchEvent(event)
            true
        }

        edgeSnapHelper = EdgeSnapHelper(windowManager, params, bubbleView)

        windowManager.addView(bubbleView, params)
    }

    private fun removeBubble() {
        if (::bubbleView.isInitialized && bubbleView.isAttachedToWindow) {
            edgeSnapHelper.cancel()
            try {
                windowManager.removeView(bubbleView)
            } catch (e: IllegalArgumentException) {
                // View already removed
            }
        }
    }

    private fun onTouchEvent(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = false
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY

                // 触摸时增加不透明度
                bubbleView.alpha = 0.8f
                edgeSnapHelper.cancel()
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = (event.rawX - initialTouchX).toInt()
                val deltaY = (event.rawY - initialTouchY).toInt()

                // 如果移动超过阈值，视为拖动
                if (Math.abs(event.rawX - initialTouchX) > CLICK_THRESHOLD ||
                    Math.abs(event.rawY - initialTouchY) > CLICK_THRESHOLD
                ) {
                    isDragging = true
                }

                if (isDragging) {
                    params.x = initialX + deltaX
                    params.y = initialY + deltaY
                    windowManager.updateViewLayout(bubbleView, params)
                }
            }

            MotionEvent.ACTION_UP -> {
                bubbleView.alpha = 0.5f

                if (isDragging) {
                    // 拖拽结束 → 边缘吸附
                    edgeSnapHelper.snapToEdge()
                } else {
                    // 点击 → 打开控制面板
                    openControlPanel()
                }
            }
        }
    }

    private fun openControlPanel() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("show_control_panel", true)
        }
        startActivity(intent)
    }

    private fun createNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FloatingBubbleService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.bubble_notification_title))
            .setContentText(getString(R.string.bubble_notification_text))
            .setSmallIcon(R.drawable.ic_bubble)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "关闭", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
