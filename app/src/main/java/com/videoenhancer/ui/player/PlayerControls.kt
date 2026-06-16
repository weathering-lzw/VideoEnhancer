package com.videoenhancer.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
 * 视频播放器简约控制覆盖层
 */
@Composable
fun PlayerControls(
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onFullScreen: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playPauseIcon by animateColorAsState(
        targetValue = if (isPlaying) AccentBlue else TextPrimary,
        label = "playPauseColor"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x80000000))
            .padding(16.dp)
    ) {
        // 顶部返回按钮
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.Start)
                .size(40.dp)
                .clip(CircleShape)
                .background(SurfaceOverlay)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "返回",
                tint = TextPrimary
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 居中播放/暂停按钮
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(64.dp)
                .clip(CircleShape)
                .background(SurfaceOverlay)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "暂停" else "播放",
                tint = TextPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 底部进度条
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceOverlay)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 进度条
            Slider(
                value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                onValueChange = { fraction ->
                    onSeek((fraction * duration).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = Primary,
                    activeTrackColor = Primary,
                    inactiveTrackColor = SliderTrack
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(currentPosition),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = formatDuration(duration),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
