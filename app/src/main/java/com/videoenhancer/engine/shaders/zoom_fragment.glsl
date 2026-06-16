// 纯数字变焦片段着色器
// OpenGL ES 3.0

#version 300 es
precision highp float;

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uTexture;
uniform float uZoom;

void main() {
    vec2 uv = (vTexCoord - 0.5) / max(uZoom, 0.001) + 0.5;
    
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }
    
    fragColor = texture(uTexture, uv);
}
