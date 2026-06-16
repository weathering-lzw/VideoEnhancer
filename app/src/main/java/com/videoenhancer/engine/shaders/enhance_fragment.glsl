// 完整画质增强片段着色器（单 Pass）
// 包含：亮度、对比度、饱和度、锐度、数字变焦
// OpenGL ES 3.0

#version 300 es
precision highp float;

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uTexture;
uniform float uBrightness;    // -1.0 ~ 1.0
uniform float uContrast;      // 0.0 ~ 2.0
uniform float uSaturation;    // 0.0 ~ 2.0
uniform float uSharpness;     // 0.0 ~ 2.0
uniform float uZoom;          // 1.0 ~ 5.0
uniform float uEnabled;       // 0.0 或 1.0
uniform vec2 uResolution;     // 屏幕宽高

// 获取亮度分量
float getLuminance(vec3 c) {
    return dot(c, vec3(0.299, 0.587, 0.114));
}

// 拉普拉斯锐化
float sharpenSample(vec2 uv, vec2 texelSize) {
    vec4 c = texture(uTexture, uv);
    
    vec4 l = texture(uTexture, uv - vec2(texelSize.x, 0.0));
    vec4 r = texture(uTexture, uv + vec2(texelSize.x, 0.0));
    vec4 t = texture(uTexture, uv - vec2(0.0, texelSize.y));
    vec4 b = texture(uTexture, uv + vec2(0.0, texelSize.y));

    vec4 laplacian = c * 5.0 - l - r - t - b;
    return clamp(c.r + laplacian.r * 0.3, 0.0, 1.0);
}

void main() {
    // 数字变焦：缩放纹理坐标
    vec2 uv = (vTexCoord - 0.5) / max(uZoom, 0.001) + 0.5;
    
    // 边缘检测
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }

    vec4 color = texture(uTexture, uv);

    if (uEnabled < 0.5) {
        fragColor = color;
        return;
    }

    vec3 result = color.rgb;

    // 1. 亮度
    result = result + uBrightness;

    // 2. 对比度
    result = (result - 0.5) * uContrast + 0.5;

    // 3. 饱和度
    float lum = getLuminance(result);
    result = mix(vec3(lum), result, uSaturation);

    // 4. 锐度
    if (uSharpness > 0.01) {
        vec2 texelSize = 1.0 / uResolution;
        float sharpLum = sharpenSample(uv, texelSize);
        float origLum = getLuminance(result);
        result = result * (1.0 + (sharpLum - origLum) * uSharpness * 0.5);
    }

    result = clamp(result, 0.0, 1.0);
    fragColor = vec4(result, color.a);
}
