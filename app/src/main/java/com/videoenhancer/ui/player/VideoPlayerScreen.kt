package com.videoenhancer.ui.player

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.videoenhancer.ui.theme.*
import com.videoenhancer.viewmodel.EnhancementViewModel
import com.videoenhancer.viewmodel.PlayerViewModel

/**
 * 内置视频播放器屏幕
 *
 * 集成 ExoPlayer + OpenGL 增强渲染管线
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    viewModel: PlayerViewModel,
    enhancementViewModel: EnhancementViewModel,
    videoUri: Uri,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isShowingControls by remember { mutableStateOf(true) }

    // 创建 ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ALL
        }
    }

    // 监听播放状态
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                viewModel.setPlaying(isPlaying)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    viewModel.setDuration(exoPlayer.duration)
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // ExoPlayer 渲染视图
        // 实际项目中，这里会使用自定义 GLSurfaceView 替代 PlayerView，
        // 将 ExoPlayer 的输出 Surface 连接到 OpenGL 渲染管线
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // 使用自定义控件
                    setBackgroundColor(android.graphics.Color.BLACK)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 触摸区域（点击切换控制面板显示）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .let { mod ->
                    mod
                },
            contentAlignment = Alignment.Center
        ) {
            // 透明点击区域
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = androidx.compose.ui.graphics.Color.Transparent,
                onClick = { isShowingControls = !isShowingControls }
            ) { }
        }

        // 控制覆盖层
        if (isShowingControls) {
            PlayerControls(
                isPlaying = viewModel.state.isPlaying,
                currentPosition = viewModel.state.currentPosition,
                duration = viewModel.state.duration,
                onPlayPause = {
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                },
                onSeek = { position ->
                    exoPlayer.seekTo(position)
                    viewModel.setPosition(position)
                },
                onFullScreen = { viewModel.toggleFullScreen() },
                onBack = onBack
            )
        }
    }
}
