package com.videoenhancer.ui.magnifier

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.videoenhancer.ui.theme.*

/**
 * 放大镜模式预览 UI
 *
 * 用于在控制面板中展示放大镜功能状态
 * 实际的放大镜悬浮窗由 ScreenCaptureService 管理
 */
@Composable
fun MagnifierPreviewPanel(
    isActive: Boolean,
    zoomLevel: Float,
    onToggle: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceVariant)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 状态指示
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "放大镜",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Switch(
                checked = isActive,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Primary,
                    checkedThumbColor = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 放大预览（模拟圆形放大区域）
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A2A2A))
                .border(2.dp, Primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "${zoomLevel}x",
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 缩放控制
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onZoomOut,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark)
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomOut,
                    contentDescription = "缩小",
                    tint = TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            Text(
                text = "${zoomLevel}x",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(24.dp))

            IconButton(
                onClick = onZoomIn,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark)
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = "放大",
                    tint = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 关闭按钮
        TextButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭",
                tint = AccentRed,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("关闭放大镜", color = AccentRed)
        }
    }
}
