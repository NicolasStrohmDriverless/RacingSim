#pragma once

#include "../glm.hpp"
#include "../geometric.hpp"
#include "constants.hpp"

namespace glm {

inline mat4 translate(const mat4 &m, const vec3 &v) {
    mat4 result = m;
    result.data[12] += v.x;
    result.data[13] += v.y;
    result.data[14] += v.z;
    return result;
}

inline mat4 lookAt(const vec3 &eye, const vec3 &center, const vec3 &up) {
    vec3 f = normalize(center - eye);
    vec3 s = normalize(cross(f, up));
    vec3 u = cross(s, f);

    mat4 result = mat4::identity();
    result.data[0] = s.x;
    result.data[4] = s.y;
    result.data[8] = s.z;

    result.data[1] = u.x;
    result.data[5] = u.y;
    result.data[9] = u.z;

    result.data[2] = -f.x;
    result.data[6] = -f.y;
    result.data[10] = -f.z;

    result.data[12] = -dot(s, eye);
    result.data[13] = -dot(u, eye);
    result.data[14] = dot(f, eye);
    return result;
}

inline mat4 perspective(float fovyRadians, float aspect, float zNear, float zFar) {
    float tanHalfFovy = std::tan(fovyRadians / 2.0f);
    mat4 result;
    result.data.fill(0.0f);
    result.data[0] = 1.0f / (aspect * tanHalfFovy);
    result.data[5] = 1.0f / tanHalfFovy;
    result.data[10] = -(zFar + zNear) / (zFar - zNear);
    result.data[11] = -1.0f;
    result.data[14] = -(2.0f * zFar * zNear) / (zFar - zNear);
    return result;
}

} // namespace glm

