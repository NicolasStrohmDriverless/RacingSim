#pragma once

#include <GLES3/gl3.h>
#include <chrono>
#include <mutex>

#include "math_glm/glm.hpp"
#include "track_mesh.h"
#include "vehicle.h"

class Renderer {
public:
    void init();
    void resize(int width, int height);
    void render();
    void onTouch(int action, float x, float y);
    void loadTrack(const float *xy, int count, float width);

private:
    void destroy();
    void uploadMeshLocked();
    void updatePhysics(float dt);
    void resetCamera();

    bool initialized_ = false;
    GLuint program_ = 0;
    GLuint vao_ = 0;
    GLuint vbo_ = 0;
    GLuint ibo_ = 0;
    GLint uMvpLocation_ = -1;
    GLint uColorLocation_ = -1;

    int viewportWidth_ = 0;
    int viewportHeight_ = 0;

    TrackMesh mesh_;
    TrackGeometry geometry_;
    bool meshDirty_ = false;

    Car car_;
    size_t lastNearestSample_ = 0;

    float steerInput_ = 0.0f;
    float throttleInput_ = 0.0f;
    float brakeInput_ = 0.0f;

    glm::vec3 cameraPos_{0.0f, 4.0f, 12.0f};
    glm::vec3 cameraTarget_{0.0f, 0.0f, 0.0f};
    glm::mat4 projection_ = glm::mat4::identity();

    std::chrono::steady_clock::time_point lastFrameTime_{};
    float physicsAccumulator_ = 0.0f;

    std::mutex mutex_;
};

Renderer &GetRenderer();

