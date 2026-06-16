package com.videoenhancer.ui.bubble

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MagnifyScan
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videoenhancer.ui.theme.*
import com.videoenhancer.viewmodel.EnhancementPreset
import com.videoenhancer.viewmodel.EnhancementViewModel

/**
 * 控制面板 — 悬浮球点击后展开的底部弹出面板
 *
 * 极简玻璃风格 UI，包含预设切换、精细调节滑条、功能入口
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlPanelSheet(
    viewModel: EnhancementViewModel,
    onDismiss: () -> Unit,
    onOpenVideo: () -> Unit,
    onOpenMagnifier: () -> Unit,
    onOpenSettings: () -> Unit,
    onStopService: () -> Unit
) {
    val params = viewModel.params
    val activePreset = viewModel.activePreset
    val isEnabled = viewModel.isEnhancementEnabled
    var showAdvanced by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceOverlay,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TextTertiary)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // --- 标题栏 ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "画质增强",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                // 增强开关
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { viewModel.toggleEnhancement() },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Primary,
                        checkedThumbColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- 预设选择 ---
            Text(
                text = "预设模式",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EnhancementPreset.entries.forEach { preset ->
                    PresetButton(
                        label = preset.label,
                        isSelected = activePreset == preset,
                        onClick = { viewModel.applyPreset(preset) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- 精细调节区域 ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "精细调节",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                TextButton(
                    onClick = { showAdvanced = !showAdvanced }
                ) {
                    Text(
                        text = if (showAdvanced) "收起" else "展开",
                        color = AccentBlue,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // 基础滑块（始终显示）
            EnhancedSlider(
                label = "亮度",
                value = params.brightness,
                valueRange = -1f..1f,
                displayValue = String.format("%+.0f%%", params.brightness * 100),
                onValueChange = { viewModel.setBrightness(it) }
            )

            EnhancedSlider(
                label = "对比度",
                value = params.contrast,
                valueRange = 0f..2f,
                displayValue = String.format("%.0f%%", params.contrast * 100),
                onValueChange = { viewModel.setContrast(it) }
            )

            // 高级调节（可收起）
            AnimatedVisibility(
                visible = showAdvanced,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    EnhancedSlider(
                        label = "饱和度",
                        value = params.saturation,
                        valueRange = 0f..2f,
                        displayValue = String.format("%.0f%%", params.saturation * 100),
                        onValueChange = { viewModel.setSaturation(it) }
                    )

                    EnhancedSlider(
                        label = "锐度",
                        value = params.sharpness,
                        valueRange = 0f..2f,
                        displayValue = String.format("%.0f%%", params.sharpness * 100),
                        onValueChange = { viewModel.setSharpness(it) }
                    )

                    EnhancedSlider(
                        label = "放大",
                        value = params.zoom,
                        valueRange = 1f..5f,
                        displayValue = String.format("%.1fx", params.zoom),
                        onValueChange = { viewModel.setZoom(it) }
                    )

                    // AI 超分开关
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI 超分辨率",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                        Switch(
                            checked = viewModel.aiSuperResolution,
                            onCheckedChange = { viewModel.toggleAI() },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = Primary,
                                checkedThumbColor = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 功能入口按钮 ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FunctionButton(
                    icon = Icons.Default.Movie,
                    label = "打开视频",
                    onClick = {
                        onDismiss()
                        onOpenVideo()
                    }
                )
                FunctionButton(
                    icon = Icons.Default.MagnifyScan,
                    label = "放大镜",
                    onClick = {
                        onDismiss()
                        onOpenMagnifier()
                    }
                )
                FunctionButton(
                    icon = Icons.Default.Settings,
                    label = "设置",
                    onClick = {
                        onDismiss()
                        onOpenSettings()
                    }
                )
                FunctionButton(
                    icon = Icons.Default.Stop,
                    label = "关闭",
                    onClick = {
                        onDismiss()
                        onStopService()
                    }
                )
            }
        }
    }
}

@Composable
private fun PresetButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Primary else SurfaceVariant
    val textColor = if (isSelected) Color.White else TextSecondary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun EnhancedSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
            Text(
                text = displayValue,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Primary,
                activeTrackColor = Primary,
                inactiveTrackColor = SliderTrack
            )
        )
    }
}

@Composable
private fun FunctionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = TextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
