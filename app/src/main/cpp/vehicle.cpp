#include "vehicle.h"

#include <algorithm>
#include <cmath>

namespace {
constexpr float kMaxSteer = 0.5f;           // radians
constexpr float kMaxSpeed = 60.0f;          // m/s equivalent
constexpr float kMaxReverseSpeed = -10.0f;
constexpr float kEngineAcceleration = 12.0f;
constexpr float kBrakeDeceleration = 25.0f;
constexpr float kRollingFriction = 3.0f;
}

void step(Car &car, float dt) {
    if (dt <= 0.0f) {
        return;
    }

    float steer = glm::clamp(car.steer, -kMaxSteer, kMaxSteer);
    float throttle = glm::clamp(car.throttle, 0.0f, 1.0f);
    float brake = glm::clamp(car.brake, 0.0f, 1.0f);

    float acceleration = throttle * kEngineAcceleration - brake * kBrakeDeceleration;
    acceleration -= glm::clamp(car.speed, -kMaxSpeed, kMaxSpeed) * kRollingFriction * 0.02f;

    car.speed += acceleration * dt;
    car.speed = glm::clamp(car.speed, kMaxReverseSpeed, kMaxSpeed);

    float angularVelocity = 0.0f;
    if (std::fabs(steer) > 1e-4f) {
        float turnRadius = car.wheelbase / std::tan(steer);
        angularVelocity = car.speed / turnRadius;
    }
    car.heading += angularVelocity * dt;

    glm::vec2 forward{std::cos(car.heading), std::sin(car.heading)};
    car.pos = car.pos + forward * (car.speed * dt);
}

