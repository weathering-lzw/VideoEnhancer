# 画质增强器 (VideoEnhancer)

> Android 端视频画质增强工具 — 实时调色 + AI 超分 + 悬浮球控制

## 功能特点

- **悬浮球控制** — 小圆形浮窗，可拖拽/边缘吸附，一键控制画质增强
- **实时画质增强** — OpenGL ES 3.0 着色器管线，支持亮度/对比度/饱和度/锐度调节
- **AI 超分辨率** — TensorFlow Lite 端侧推理，2x/4x 分辨率提升
- **画面放大镜** — 悬浮窗口放大屏幕内容（MediaProjection 模式）
- **预设模式** — 原始/鲜艳/柔和/电影，一键切换
- **极简玻璃 UI** — 深色主题，毛玻璃效果，Material You 设计语言

## 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material3 |
| 视频播放 | Media3 ExoPlayer |
| 图形渲染 | OpenGL ES 3.0 (Fragment Shaders) |
| AI 推理 | TensorFlow Lite + GPU Delegate |
| 悬浮窗 | WindowManager (TYPE_APPLICATION_OVERLAY) |
| 屏幕捕获 | MediaProjection API |
| 最低 SDK | Android 10 (API 29) |
| 架构 | MVVM |

## 项目结构

```
app/src/main/java/com/videoenhancer/
├── App.kt                    # Application 入口
├── MainActivity.kt           # 主界面 + 权限引导
├── service/
│   ├── FloatingBubbleService.kt    # 悬浮球服务
│   └── ScreenCaptureService.kt     # 屏幕捕获服务
├── ui/
│   ├── bubble/               # 悬浮球 + 控制面板 UI
│   ├── player/               # 内置视频播放器
│   ├── magnifier/            # 放大镜 UI
│   ├── settings/             # 设置页面
│   └── theme/                # 主题/颜色/字体
├── engine/
│   ├── VideoEnhancementEngine.kt   # 增强引擎协调器
│   ├── GlRenderer.kt               # OpenGL 渲染器
│   ├── GlFilter.kt                 # 滤镜基类 + GLSL 源码
│   └── shaders/                    # GLSL 着色器文件
├── ml/
│   ├── SuperResolutionModel.kt     # TFLite 超分模型
│   └── SRDelegate.kt              # GPU 委托加速
├── util/
│   ├── PermissionManager.kt       # 权限管理
│   └── EdgeSnapHelper.kt          # 边缘吸附动画
└── viewmodel/
    ├── EnhancementViewModel.kt    # 增强参数状态
    ├── BubbleViewModel.kt         # 悬浮球设置
    └── PlayerViewModel.kt         # 播放器状态
```

## 开发环境要求

- Android Studio Hedgehog (2023.1.1) 或更新
- JDK 17+
- Android SDK 34
- Gradle 8.2+

## 构建步骤

### 1. 克隆项目

```bash
git clone <your-repo-url> VideoEnhancer
cd VideoEnhancer
```

### 2. 下载 AI 模型文件

**选项 A — 自动下载脚本：**

```bash
# Windows (PowerShell)
powershell -ExecutionPolicy Bypass -File download_models.ps1

# macOS / Linux
chmod +x download_models.sh
./download_models.sh
```

**选项 B — 手动下载：**
将以下文件放入 `app/src/main/assets/`：
- `fsrcnn_2x.tflite` — FSRCNN 2x 超分模型
- `esrgan_lite_4x.tflite` — ESRGAN-lite 4x 超分模型

### 3. 使用 Android Studio 打开项目

1. 启动 Android Studio
2. File → Open → 选择 `VideoEnhancer` 目录
3. 等待 Gradle 同步完成
4. Run → Run 'app'

### 4. 首次使用

1. 启动应用
2. 按提示开启 **悬浮窗权限** （设置 → 应用 → 画质增强器 → 显示在其他应用上层）
3. 点击「启动悬浮球」按钮
4. 悬浮球出现在屏幕边缘
5. 点击悬浮球 → 控制面板
6. 选择预设或手动调节参数

## 权限说明

| 权限 | 用途 | 如何开启 |
|------|------|----------|
| SYSTEM_ALERT_WINDOW | 悬浮球显示 | 设置 → 应用 → 画质增强器 |
| FOREGROUND_SERVICE | 后台运行 | 自动请求 |
| MediaProjection | 放大镜模式 | 点击「放大镜」时授权 |
| POST_NOTIFICATIONS | 通知（Android 13+） | 启动时弹出请求 |

## 技术说明

### 画质增强工作原理

```
ExoPlayer 视频帧输出
    ↓ Surface
OpenGL ES 3.0 纹理接收
    ↓ Fragment Shader 管线
亮度 → 对比度 → 饱和度 → 锐度 → 数字变焦
    ↓ FBO (帧缓冲对象)
SurfaceView 显示
```

所有效果合并为**单 Pass 着色器**，最小化 GPU 渲染开销。

### AI 超分辨率

- 使用 **FSRCNN**（2x）或 **ESRGAN-lite**（4x）轻量模型
- 通过 **GPU Delegate** 加速推理（支持设备）
- 帧级别处理：对解码后的每帧进行超分→渲染
- 目标帧率：720p 输入下 >15fps（GPU 加速时）

### 跨应用增强说明

受 Android 系统限制，第三方应用无法直接截取其他 App 的视频流。因此：
- **本应用播放器**：完全增强（调色 + AI 超分 + 锐化）
- **其他 App**：色彩滤镜覆盖（有限）或放大镜模式

## License

MIT License
