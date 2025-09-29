#pragma once

namespace shaders {

constexpr const char *kVertexShader = R"(#version 300 es
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec2 aUv;

uniform mat4 uMvp;

out vec2 vUv;

void main() {
    vUv = aUv;
    gl_Position = uMvp * vec4(aPosition, 1.0);
}
)";

constexpr const char *kFragmentShader = R"(#version 300 es
precision mediump float;

in vec2 vUv;

uniform vec4 uColor;

out vec4 fragColor;

void main() {
    float stripes = step(0.5, fract(vUv.y * 4.0));
    vec3 base = mix(vec3(0.12, 0.12, 0.12), vec3(0.18, 0.18, 0.18), stripes);
    fragColor = vec4(base + uColor.rgb * 0.4, uColor.a);
}
)";

} // namespace shaders

