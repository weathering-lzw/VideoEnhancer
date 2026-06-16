# AI 超分辨率模型下载脚本 (PowerShell)
# 将模型文件下载到 app/src/main/assets/ 目录
#
# 使用方式:
#   powershell -ExecutionPolicy Bypass -File download_models.ps1

$AssetsDir = "app/src/main/assets"
New-Item -ItemType Directory -Force -Path $AssetsDir | Out-Null

Write-Host "=== 下载 AI 超分辨率模型 ===" -ForegroundColor Cyan

# FSRCNN 2x 模型
Write-Host "[1/2] 下载 FSRCNN 2x 超分模型..." -ForegroundColor Yellow
$FsrcnnUrl = "https://storage.googleapis.com/tflite-models/fsrcnn_2x.tflite"
try {
    Invoke-WebRequest -Uri $FsrcnnUrl -OutFile "$AssetsDir/fsrcnn_2x.tflite" -ErrorAction Stop
    Write-Host "  ✓ FSRCNN 模型下载成功" -ForegroundColor Green
} catch {
    Write-Host "  ✗ FSRCNN 模型下载失败: $_" -ForegroundColor Red
}

# ESRGAN-lite 4x 模型
Write-Host "[2/2] 下载 ESRGAN-lite 4x 超分模型..." -ForegroundColor Yellow
$EsrganUrl = "https://storage.googleapis.com/tflite-models/esrgan_lite_4x.tflite"
try {
    Invoke-WebRequest -Uri $EsrganUrl -OutFile "$AssetsDir/esrgan_lite_4x.tflite" -ErrorAction Stop
    Write-Host "  ✓ ESRGAN 模型下载成功" -ForegroundColor Green
} catch {
    Write-Host "  ✗ ESRGAN 模型下载失败: $_" -ForegroundColor Red
}

Write-Host "=== 下载完成 ===" -ForegroundColor Cyan
Get-ChildItem -Path $AssetsDir -Filter "*.tflite" | Select-Object Name, Length
