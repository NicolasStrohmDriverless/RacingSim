#pragma once

#include "math_glm/glm.hpp"

struct Car {
    float steer = 0.0f;
    float throttle = 0.0f;
    float brake = 0.0f;
    float heading = 0.0f;
    float speed = 0.0f;
    glm::vec2 pos{0.0f, 0.0f};
    float wheelbase = 2.6f;
};

void step(Car &car, float dt);

