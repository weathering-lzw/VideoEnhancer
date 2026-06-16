package com.videoenhancer.util

import android.animation.ValueAnimator
import android.view.View
import android.view.WindowManager

/**
 * 悬浮球边缘吸附动画器
 * 当用户拖拽后松开，自动将悬浮球吸附到最近的屏幕边缘
 */
class EdgeSnapHelper(
    private val windowManager: WindowManager,
    private val params: WindowManager.LayoutParams,
    private val bubbleView: View
) {
    private var animator: ValueAnimator? = null
    private val displayMetrics = bubbleView.resources.displayMetrics

    /** 吸附后露出的宽度 (dp → px) */
    var snapMarginPx: Int = (16 * displayMetrics.density).toInt()

    /** 悬浮球直径 (dp → px) */
    private val bubbleSizePx: Int
        get() = params.width

    /**
     * 执行边缘吸附动画
     */
    fun snapToEdge() {
        animator?.cancel()

        val screenWidth = displayMetrics.widthPixels
        val currentX = params.x

        // 计算到左右边缘的距离
        val distToLeft = currentX + bubbleSizePx // 左边露出距离
        val distToRight = screenWidth - currentX  // 右边露出距离

        val targetX: Int
        val snapToLeft: Boolean

        if (distToLeft < distToRight) {
            // 吸附到左边：露出 snapMarginPx
            targetX = snapMarginPx - bubbleSizePx
            snapToLeft = true
        } else {
            // 吸附到右边：露出 snapMarginPx
            targetX = screenWidth - snapMarginPx
            snapToLeft = false
        }

        // 如果是左边，还要确保 Y 不超出顶部/底部状态栏区域
        val clampedX = if (snapToLeft) {
            maxOf(targetX, -bubbleSizePx + snapMarginPx)
        } else {
            minOf(targetX, screenWidth - snapMarginPx)
        }

        animateToPosition(clampedX, params.y)
    }

    /**
     * 动画移动到目标坐标
     */
    private fun animateToPosition(targetX: Int, targetY: Int) {
        val startX = params.x
        val startY = params.y

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 200
            addUpdateListener { animation ->
                val fraction = animation.animatedFraction
                val easedFraction = easeOutCubic(fraction)

                params.x = (startX + (targetX - startX) * easedFraction).toInt()
                params.y = (startY + (targetY - startY) * easedFraction).toInt()
                windowManager.updateViewLayout(bubbleView, params)
            }
            start()
        }
    }

    /**
     * 立即取消动画
     */
    fun cancel() {
        animator?.cancel()
    }

    private fun easeOutCubic(t: Float): Float {
        return 1f - (1f - t) * (1f - t) * (1f - t)
    }
}
