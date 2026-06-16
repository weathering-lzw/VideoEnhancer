package com.videoenhancer

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.videoenhancer.service.FloatingBubbleService
import com.videoenhancer.service.ScreenCaptureService
import com.videoenhancer.ui.bubble.ControlPanelSheet
import com.videoenhancer.ui.player.VideoPlayerScreen
import com.videoenhancer.ui.settings.SettingsScreen
import com.videoenhancer.ui.theme.*
import com.videoenhancer.util.PermissionManager
import com.videoenhancer.viewmodel.BubbleViewModel
import com.videoenhancer.viewmodel.EnhancementViewModel
import com.videoenhancer.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {

    /** 屏幕捕获授权结果 */
    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            ScreenCaptureService.start(
                this,
                result.resultCode,
                result.data
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val showPanel = intent.getBooleanExtra("show_control_panel", false)

        setContent {
            VideoEnhancerTheme {
                MainScreen(
                    showControlPanel = showPanel,
                    onStartBubble = { FloatingBubbleService.start(this) },
                    onStopBubble = { FloatingBubbleService.stop(this) },
                    onRequestOverlay = {
                        PermissionManager.openOverlaySettings(this)
                    },
                    onStartMagnifier = {
                        val projectionManager =
                            getSystemService(MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
                        mediaProjectionLauncher.launch(
                            projectionManager.createScreenCaptureIntent()
                        )
                    },
                    onOpenVideo = { launchVideoPicker() }
                )
            }
        }
    }

    private fun launchVideoPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
        }
        videoPickerLauncher.launch(intent)
    }

    private val videoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.data?.let { uri ->
            // 持久化读取权限
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            // 通知 UI 打开视频播放器
            _selectedVideoUri = uri
        }
    }

    companion object {
        private var _selectedVideoUri: Uri? = null
    }
}

/**
 * 主屏幕 — 权限引导 + 悬浮球控制 + 功能导航
 */
@Composable
private fun MainScreen(
    showControlPanel: Boolean = false,
    onStartBubble: () -> Unit,
    onStopBubble: () -> Unit,
    onRequestOverlay: () -> Unit,
    onStartMagnifier: () -> Unit,
    onOpenVideo: () -> Unit
) {
    val context = LocalContext.current
    val enhancementViewModel: EnhancementViewModel = viewModel()
    val bubbleViewModel: BubbleViewModel = viewModel()
    val playerViewModel: PlayerViewModel = viewModel()

    var currentScreen by remember { mutableStateOf("main") }
    var showPanel by remember { mutableStateOf(showControlPanel) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    // 检查悬浮窗权限
    val hasOverlay = PermissionManager.hasOverlayPermission(context)
    val hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        PermissionManager.hasNotificationPermission(context)
    } else true

    // 处理从其他界面传递的选中视频
    val pendingVideoUri = MainActivity._selectedVideoUri
    if (pendingVideoUri != null) {
        MainActivity._selectedVideoUri = null
        playerViewModel.setVideo(pendingVideoUri)
        currentScreen = "player"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        when (currentScreen) {
            "main" -> {
                MainContent(
                    hasOverlay = hasOverlay,
                    hasNotification = hasNotification,
                    isServiceRunning = bubbleViewModel.isServiceRunning,
                    onStartBubble = {
                        if (!hasOverlay) {
                            showPermissionDialog = true
                            return@MainContent
                        }
                        bubbleViewModel.setServiceRunning(true)
                        onStartBubble()
                    },
                    onStopBubble = {
                        bubbleViewModel.setServiceRunning(false)
                        onStopBubble()
                    },
                    onOpenControlPanel = { showPanel = true },
                    onOpenMagnifier = onStartMagnifier,
                    onOpenVideo = {
                        currentScreen = "player_picker"
                        onOpenVideo()
                    },
                    onOpenSettings = { currentScreen = "settings" }
                )

                // 权限引导对话框
                if (showPermissionDialog) {
                    PermissionDialog(
                        hasOverlay = hasOverlay,
                        hasNotification = hasNotification,
                        onDismiss = { showPermissionDialog = false },
                        onOpenOverlaySettings = onRequestOverlay,
                        onOpenNotificationSettings = {
                            PermissionManager.openNotificationSettings(context)
                        }
                    )
                }

                // 控制面板
                if (showPanel) {
                    ControlPanelSheet(
                        viewModel = enhancementViewModel,
                        onDismiss = { showPanel = false },
                        onOpenVideo = {
                            currentScreen = "player_picker"
                            onOpenVideo()
                        },
                        onOpenMagnifier = onStartMagnifier,
                        onOpenSettings = { currentScreen = "settings" },
                        onStopService = {
                            bubbleViewModel.setServiceRunning(false)
                            onStopBubble()
                            showPanel = false
                        }
                    )
                }
            }

            "settings" -> {
                SettingsScreen(
                    viewModel = bubbleViewModel,
                    onBack = { currentScreen = "main" }
                )
            }

            "player" -> {
                playerViewModel.state.videoUri?.let { uri ->
                    VideoPlayerScreen(
                        viewModel = playerViewModel,
                        enhancementViewModel = enhancementViewModel,
                        videoUri = uri,
                        onBack = {
                            playerViewModel.clearVideo()
                            currentScreen = "main"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MainContent(
    hasOverlay: Boolean,
    hasNotification: Boolean,
    isServiceRunning: Boolean,
    onStartBubble: () -> Unit,
    onStopBubble: () -> Unit,
    onOpenControlPanel: () -> Unit,
    onOpenMagnifier: () -> Unit,
    onOpenVideo: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Logo/标题
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
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
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "画质增强器",
            style = MaterialTheme.typography.displayLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "实时提升视频画质 · AI 超分辨率",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 主按钮
        Button(
            onClick = {
                if (isServiceRunning) onOpenControlPanel()
                else onStartBubble()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary
            )
        ) {
            Icon(
                imageVector = if (isServiceRunning) Icons.Default.AutoAwesome
                else Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isServiceRunning) "打开控制面板"
                else "启动悬浮球",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (isServiceRunning) {
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onStopBubble) {
                Text(
                    "关闭悬浮球",
                    color = AccentRed,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 功能入口
        if (isServiceRunning) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FeatureChip(
                    icon = Icons.Default.AutoAwesome,
                    label = "控制面板",
                    onClick = onOpenControlPanel
                )
                FeatureChip(
                    icon = Icons.Default.AutoAwesome,
                    label = "打开视频",
                    onClick = onOpenVideo
                )
                FeatureChip(
                    icon = Icons.Default.Settings,
                    label = "设置",
                    onClick = onOpenSettings
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 底部提示
        if (!hasOverlay) {
            Text(
                text = "需要开启悬浮窗权限",
                color = AccentYellow,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeatureChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .let { mod ->
                mod
            }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = TextPrimary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun PermissionDialog(
    hasOverlay: Boolean,
    hasNotification: Boolean,
    onDismiss: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "需要系统权限",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "画质增强器需要以下权限才能正常工作：",
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (!hasOverlay) {
                    PermissionRequirement(
                        label = "悬浮窗权限",
                        description = "显示悬浮球控制按钮",
                        isGranted = false
                    )
                }

                if (!hasNotification) {
                    Spacer(modifier = Modifier.height(8.dp))
                    PermissionRequirement(
                        label = "通知权限",
                        description = "保持悬浮球后台运行",
                        isGranted = false
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!hasOverlay) onOpenOverlaySettings()
                    else if (!hasNotification) onOpenNotificationSettings()
                    else onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("前往设置")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后", color = TextSecondary)
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun PermissionRequirement(
    label: String,
    description: String,
    isGranted: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isGranted) AccentGreen else AccentRed)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = label, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
            Text(text = description, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}
