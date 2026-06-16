#!/bin/bash
# AI 超分辨率模型下载脚本
# 将模型文件下载到 app/src/main/assets/ 目录
#
# 使用方式:
#   chmod +x download_models.sh
#   ./download_models.sh

ASSETS_DIR="app/src/main/assets"
mkdir -p "$ASSETS_DIR"

echo "=== 下载 AI 超分辨率模型 ==="

# FSRCNN 2x 模型 (轻量快速)
echo "[1/2] 下载 FSRCNN 2x 超分模型..."
FSRCNN_URL="https://storage.googleapis.com/tflite-models/fsrcnn_2x.tflite"
if command -v curl &> /dev/null; then
    curl -L -o "$ASSETS_DIR/fsrcnn_2x.tflite" "$FSRCNN_URL"
elif command -v wget &> /dev/null; then
    wget -O "$ASSETS_DIR/fsrcnn_2x.tflite" "$FSRCNN_URL"
else
    echo "错误: 请安装 curl 或 wget"
    exit 1
fi

# ESRGAN-lite 4x 模型 (高质量)
echo "[2/2] 下载 ESRGAN-lite 4x 超分模型..."
ESRGAN_URL="https://storage.googleapis.com/tflite-models/esrgan_lite_4x.tflite"
if command -v curl &> /dev/null; then
    curl -L -o "$ASSETS_DIR/esrgan_lite_4x.tflite" "$ESRGAN_URL"
elif command -v wget &> /dev/null; then
    wget -O "$ASSETS_DIR/esrgan_lite_4x.tflite" "$ESRGAN_URL"
fi

echo "=== 下载完成 ==="
echo "模型文件位置:"
ls -lh "$ASSETS_DIR"/*.tflite 2>/dev/null || echo "(模型文件可能需手动下载)"
