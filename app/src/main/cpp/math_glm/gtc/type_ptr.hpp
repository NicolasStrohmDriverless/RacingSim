#pragma once

#include "../glm.hpp"

namespace glm {

inline float *value_ptr(mat4 &m) {
    return m.data.data();
}

inline const float *value_ptr(const mat4 &m) {
    return m.data.data();
}

inline const float *value_ptr(const vec3 &v) {
    return &v.x;
}

} // namespace glm

