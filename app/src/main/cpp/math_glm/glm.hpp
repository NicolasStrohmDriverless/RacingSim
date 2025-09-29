#pragma once

#include <algorithm>
#include <array>
#include <cmath>

namespace glm {

struct vec2 {
    float x;
    float y;

    constexpr vec2() : x(0.0f), y(0.0f) {}
    constexpr vec2(float xVal, float yVal) : x(xVal), y(yVal) {}
};

struct vec3 {
    float x;
    float y;
    float z;

    constexpr vec3() : x(0.0f), y(0.0f), z(0.0f) {}
    constexpr vec3(float xVal, float yVal, float zVal) : x(xVal), y(yVal), z(zVal) {}
    constexpr explicit vec3(const vec2 &v, float zVal = 0.0f) : x(v.x), y(v.y), z(zVal) {}
};

struct vec4 {
    float x;
    float y;
    float z;
    float w;

    constexpr vec4() : x(0.0f), y(0.0f), z(0.0f), w(0.0f) {}
    constexpr vec4(float xVal, float yVal, float zVal, float wVal)
            : x(xVal), y(yVal), z(zVal), w(wVal) {}
};

struct mat4 {
    std::array<float, 16> data;

    mat4() {
        data.fill(0.0f);
    }

    static mat4 identity() {
        mat4 result;
        result.data = {1.0f, 0.0f, 0.0f, 0.0f,
                       0.0f, 1.0f, 0.0f, 0.0f,
                       0.0f, 0.0f, 1.0f, 0.0f,
                       0.0f, 0.0f, 0.0f, 1.0f};
        return result;
    }
};

inline vec2 operator+(const vec2 &a, const vec2 &b) {
    return {a.x + b.x, a.y + b.y};
}

inline vec2 operator-(const vec2 &a, const vec2 &b) {
    return {a.x - b.x, a.y - b.y};
}

inline vec2 operator*(const vec2 &a, float s) {
    return {a.x * s, a.y * s};
}

inline vec2 operator*(float s, const vec2 &a) {
    return a * s;
}

inline vec2 operator/(const vec2 &a, float s) {
    return {a.x / s, a.y / s};
}

inline vec3 operator+(const vec3 &a, const vec3 &b) {
    return {a.x + b.x, a.y + b.y, a.z + b.z};
}

inline vec3 operator-(const vec3 &a, const vec3 &b) {
    return {a.x - b.x, a.y - b.y, a.z - b.z};
}

inline vec3 operator*(const vec3 &a, float s) {
    return {a.x * s, a.y * s, a.z * s};
}

inline vec3 operator*(float s, const vec3 &a) {
    return a * s;
}

inline vec3 operator/(const vec3 &a, float s) {
    return {a.x / s, a.y / s, a.z / s};
}

inline vec4 operator*(const vec4 &a, float s) {
    return {a.x * s, a.y * s, a.z * s, a.w * s};
}

inline vec4 operator+(const vec4 &a, const vec4 &b) {
    return {a.x + b.x, a.y + b.y, a.z + b.z, a.w + b.w};
}

inline vec4 operator-(const vec4 &a, const vec4 &b) {
    return {a.x - b.x, a.y - b.y, a.z - b.z, a.w - b.w};
}

inline vec4 operator*(const mat4 &m, const vec4 &v) {
    vec4 result;
    result.x = m.data[0] * v.x + m.data[4] * v.y + m.data[8] * v.z + m.data[12] * v.w;
    result.y = m.data[1] * v.x + m.data[5] * v.y + m.data[9] * v.z + m.data[13] * v.w;
    result.z = m.data[2] * v.x + m.data[6] * v.y + m.data[10] * v.z + m.data[14] * v.w;
    result.w = m.data[3] * v.x + m.data[7] * v.y + m.data[11] * v.z + m.data[15] * v.w;
    return result;
}

inline mat4 operator*(const mat4 &a, const mat4 &b) {
    mat4 result;
    for (int col = 0; col < 4; ++col) {
        for (int row = 0; row < 4; ++row) {
            result.data[col * 4 + row] =
                    a.data[0 * 4 + row] * b.data[col * 4 + 0] +
                    a.data[1 * 4 + row] * b.data[col * 4 + 1] +
                    a.data[2 * 4 + row] * b.data[col * 4 + 2] +
                    a.data[3 * 4 + row] * b.data[col * 4 + 3];
        }
    }
    return result;
}

template<typename T>
inline T clamp(T value, T minVal, T maxVal) {
    return std::max(minVal, std::min(value, maxVal));
}

template<typename T>
inline T mix(const T &a, const T &b, float factor) {
    return a * (1.0f - factor) + b * factor;
}

inline float mix(float a, float b, float factor) {
    return a * (1.0f - factor) + b * factor;
}

inline float radians(float degrees) {
    return degrees * 0.017453292519943295769f;
}

inline float degrees(float radiansVal) {
    return radiansVal * 57.295779513082320876f;
}

} // namespace glm

#include "geometric.hpp"
#include "gtc/constants.hpp"
#include "gtc/matrix_transform.hpp"
#include "gtc/type_ptr.hpp"

