#pragma once

#include "glm.hpp"

namespace glm {

inline float dot(const vec2 &a, const vec2 &b) {
    return a.x * b.x + a.y * b.y;
}

inline float dot(const vec3 &a, const vec3 &b) {
    return a.x * b.x + a.y * b.y + a.z * b.z;
}

inline float length(const vec2 &v) {
    return std::sqrt(dot(v, v));
}

inline float length(const vec3 &v) {
    return std::sqrt(dot(v, v));
}

inline vec2 normalize(const vec2 &v) {
    float len = length(v);
    if (len <= 1e-6f) {
        return {0.0f, 0.0f};
    }
    return v / len;
}

inline vec3 normalize(const vec3 &v) {
    float len = length(v);
    if (len <= 1e-6f) {
        return {0.0f, 0.0f, 0.0f};
    }
    return v / len;
}

inline vec3 cross(const vec3 &a, const vec3 &b) {
    return {a.y * b.z - a.z * b.y,
            a.z * b.x - a.x * b.z,
            a.x * b.y - a.y * b.x};
}

inline float distance(const vec2 &a, const vec2 &b) {
    return length(a - b);
}

inline float distance(const vec3 &a, const vec3 &b) {
    return length(a - b);
}

} // namespace glm

