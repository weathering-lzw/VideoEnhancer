package com.videoenhancer.ui.bubble

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.videoenhancer.ui.theme.Primary
import com.videoenhancer.ui.theme.PrimaryLight

/**
 * 悬浮球 Composable 预览（实际悬浮球使用 WindowManager + View 实现）
 * 此 Composable 用于在控制面板/设置页面中展示悬浮球样式
 */
@Composable
fun FloatingBubblePreview(
    isActive: Boolean = true,
    size: Int = 52,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (isActive) 0.8f else 0.4f,
        label = "bubbleAlpha"
    )

    Box(
        modifier = modifier
            .size(size.dp)
            .alpha(alpha)
            .shadow(
                elevation = 6.dp,
                shape = CircleShape,
                ambientColor = Primary.copy(alpha = 0.3f),
                spotColor = Primary.copy(alpha = 0.3f)
            )
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PrimaryLight.copy(alpha = 0.6f),
                        Primary.copy(alpha = 0.3f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "画质增强",
            tint = Color.White,
            modifier = Modifier.size((size * 0.5).dp)
        )
    }
}
