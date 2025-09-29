#include "renderer.h"

#include <android/log.h>
#include <algorithm>
#include <cmath>
#include <limits>
#include <utility>
#include <vector>

#include "shader_sources.h"

namespace {

constexpr const char *kTag = "Renderer";
constexpr float kPhysicsStep = 1.0f / 60.0f;

Renderer gRenderer;

#ifndef NDEBUG
void checkGlError(const char *label) {
    GLenum error = glGetError();
    if (error != GL_NO_ERROR) {
        __android_log_print(ANDROID_LOG_ERROR, kTag, "%s: GL error 0x%x", label, error);
    }
}
#else
void checkGlError(const char *) {}
#endif

GLuint compileShader(GLenum type, const char *source) {
    GLuint shader = glCreateShader(type);
    glShaderSource(shader, 1, &source, nullptr);
    glCompileShader(shader);
    GLint status = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &status);
    if (status != GL_TRUE) {
        GLint length = 0;
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &length);
        std::vector<char> info(static_cast<size_t>(std::max(1, length)));
        glGetShaderInfoLog(shader, length, nullptr, info.data());
        __android_log_print(ANDROID_LOG_ERROR, kTag, "Shader compile failed: %s", info.data());
        glDeleteShader(shader);
        return 0;
    }
    return shader;
}

GLuint linkProgram(GLuint vertexShader, GLuint fragmentShader) {
    GLuint program = glCreateProgram();
    glAttachShader(program, vertexShader);
    glAttachShader(program, fragmentShader);
    glLinkProgram(program);
    GLint status = 0;
    glGetProgramiv(program, GL_LINK_STATUS, &status);
    if (status != GL_TRUE) {
        GLint length = 0;
        glGetProgramiv(program, GL_INFO_LOG_LENGTH, &length);
        std::vector<char> info(static_cast<size_t>(std::max(1, length)));
        glGetProgramInfoLog(program, length, nullptr, info.data());
        __android_log_print(ANDROID_LOG_ERROR, kTag, "Program link failed: %s", info.data());
        glDeleteProgram(program);
        return 0;
    }
    return program;
}

} // namespace

Renderer &GetRenderer() {
    return gRenderer;
}

void Renderer::init() {
    std::lock_guard<std::mutex> lock(mutex_);
    if (initialized_) {
        return;
    }

    GLuint vertex = compileShader(GL_VERTEX_SHADER, shaders::kVertexShader);
    GLuint fragment = compileShader(GL_FRAGMENT_SHADER, shaders::kFragmentShader);
    if (vertex == 0 || fragment == 0) {
        return;
    }

    program_ = linkProgram(vertex, fragment);
    glDeleteShader(vertex);
    glDeleteShader(fragment);

    if (program_ == 0) {
        return;
    }

    glGenVertexArrays(1, &vao_);
    glGenBuffers(1, &vbo_);
    glGenBuffers(1, &ibo_);

    uMvpLocation_ = glGetUniformLocation(program_, "uMvp");
    uColorLocation_ = glGetUniformLocation(program_, "uColor");

    glEnable(GL_DEPTH_TEST);
    glEnable(GL_CULL_FACE);
    glCullFace(GL_BACK);

    initialized_ = true;
    lastFrameTime_ = std::chrono::steady_clock::now();
    physicsAccumulator_ = 0.0f;
    checkGlError("init");
}

void Renderer::resize(int width, int height) {
    std::lock_guard<std::mutex> lock(mutex_);
    viewportWidth_ = std::max(width, 1);
    viewportHeight_ = std::max(height, 1);
    float aspect = static_cast<float>(viewportWidth_) / static_cast<float>(viewportHeight_);
    projection_ = glm::perspective(glm::radians(60.0f), aspect, 0.1f, 500.0f);
}

void Renderer::render() {
    std::lock_guard<std::mutex> lock(mutex_);
    if (!initialized_) {
        return;
    }

    auto now = std::chrono::steady_clock::now();
    if (lastFrameTime_.time_since_epoch().count() == 0) {
        lastFrameTime_ = now;
    }
    float delta = std::chrono::duration<float>(now - lastFrameTime_).count();
    lastFrameTime_ = now;
    delta = std::min(delta, 0.25f);
    physicsAccumulator_ += delta;

    while (physicsAccumulator_ >= kPhysicsStep) {
        updatePhysics(kPhysicsStep);
        physicsAccumulator_ -= kPhysicsStep;
    }

    if (meshDirty_) {
        uploadMeshLocked();
    }

    int width = std::max(viewportWidth_, 1);
    int height = std::max(viewportHeight_, 1);
    glViewport(0, 0, width, height);
    glClearColor(0.02f, 0.04f, 0.08f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    if (mesh_.indices.empty() || vao_ == 0 || program_ == 0) {
        return;
    }

    glm::mat4 view = glm::lookAt(cameraPos_, cameraTarget_, glm::vec3(0.0f, 1.0f, 0.0f));
    glm::mat4 mvp = projection_ * view;

    glUseProgram(program_);
    glUniformMatrix4fv(uMvpLocation_, 1, GL_FALSE, glm::value_ptr(mvp));
    glUniform4f(uColorLocation_, 0.3f, 0.7f, 0.3f, 1.0f);

    glBindVertexArray(vao_);
    glDrawElements(GL_TRIANGLES, static_cast<GLsizei>(mesh_.indices.size()), GL_UNSIGNED_INT, nullptr);
    glBindVertexArray(0);

    checkGlError("render");
}

void Renderer::onTouch(int action, float x, float y) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (action == 1 /*UP*/ || action == 3 /*CANCEL*/) {
        steerInput_ = 0.0f;
        throttleInput_ = 0.0f;
        brakeInput_ = 0.0f;
        return;
    }
    float steer = glm::clamp(x, -1.0f, 1.0f);
    float drive = glm::clamp(y, -1.0f, 1.0f);
    steerInput_ = steer;
    if (drive >= 0.0f) {
        throttleInput_ = drive;
        brakeInput_ = 0.0f;
    } else {
        throttleInput_ = 0.0f;
        brakeInput_ = -drive;
    }
}

void Renderer::loadTrack(const float *xy, int count, float width) {
    if (xy == nullptr || count < 2) {
        __android_log_print(ANDROID_LOG_WARN, kTag, "nativeLoadTrack called with insufficient data");
        return;
    }

    std::vector<glm::vec2> points;
    points.reserve(static_cast<size_t>(count));
    for (int i = 0; i < count; ++i) {
        points.emplace_back(xy[i * 2], xy[i * 2 + 1]);
    }

    TrackMesh mesh;
    TrackGeometry geometry;
    if (!buildTrackMesh(points, width, mesh, geometry)) {
        return;
    }

    std::lock_guard<std::mutex> lock(mutex_);
    mesh_ = std::move(mesh);
    geometry_ = std::move(geometry);
    meshDirty_ = true;
    car_ = Car{};
    if (!geometry_.samples.empty()) {
        car_.pos = geometry_.samples.front();
        glm::vec2 tangent = geometry_.tangents.front();
        if (glm::length(tangent) < 1e-4f && geometry_.samples.size() > 1) {
            tangent = geometry_.samples[1] - geometry_.samples[0];
        }
        if (glm::length(tangent) < 1e-4f) {
            tangent = {1.0f, 0.0f};
        }
        car_.heading = std::atan2(tangent.y, tangent.x);
    }
    steerInput_ = 0.0f;
    throttleInput_ = 0.0f;
    brakeInput_ = 0.0f;
    lastNearestSample_ = 0;
    resetCamera();
}

void Renderer::destroy() {
    if (vao_ != 0) {
        glDeleteVertexArrays(1, &vao_);
        vao_ = 0;
    }
    if (vbo_ != 0) {
        glDeleteBuffers(1, &vbo_);
        vbo_ = 0;
    }
    if (ibo_ != 0) {
        glDeleteBuffers(1, &ibo_);
        ibo_ = 0;
    }
    if (program_ != 0) {
        glDeleteProgram(program_);
        program_ = 0;
    }
    initialized_ = false;
}

void Renderer::uploadMeshLocked() {
    if (mesh_.vertices.empty() || mesh_.indices.empty() || vao_ == 0) {
        return;
    }
    glBindVertexArray(vao_);
    glBindBuffer(GL_ARRAY_BUFFER, vbo_);
    glBufferData(GL_ARRAY_BUFFER,
                 static_cast<GLsizeiptr>(mesh_.vertices.size() * sizeof(float)),
                 mesh_.vertices.data(),
                 GL_STATIC_DRAW);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo_);
    glBufferData(GL_ELEMENT_ARRAY_BUFFER,
                 static_cast<GLsizeiptr>(mesh_.indices.size() * sizeof(uint32_t)),
                 mesh_.indices.data(),
                 GL_STATIC_DRAW);

    GLsizei strideBytes = static_cast<GLsizei>(mesh_.stride * sizeof(float));
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, strideBytes, reinterpret_cast<void *>(0));
    glEnableVertexAttribArray(1);
    glVertexAttribPointer(1, 2, GL_FLOAT, GL_FALSE, strideBytes, reinterpret_cast<void *>(3 * sizeof(float)));

    glBindVertexArray(0);
    meshDirty_ = false;
    checkGlError("uploadMesh");
}

void Renderer::updatePhysics(float dt) {
    car_.steer = steerInput_ * 0.5f;
    car_.throttle = throttleInput_;
    car_.brake = brakeInput_;
    step(car_, dt);

    if (!geometry_.samples.empty()) {
        size_t bestIndex = lastNearestSample_;
        float bestDistance = std::numeric_limits<float>::max();
        size_t searchStart = (bestIndex > 5) ? bestIndex - 5 : 0;
        size_t searchEnd = std::min(geometry_.samples.size(), bestIndex + 6);
        for (size_t i = searchStart; i < searchEnd; ++i) {
            float distance = glm::distance(car_.pos, geometry_.samples[i]);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }
        lastNearestSample_ = bestIndex;
        glm::vec2 nearest = geometry_.samples[bestIndex];
        glm::vec2 offset = car_.pos - nearest;
        float offsetLength = glm::length(offset);
        float maxOffset = geometry_.width * 0.45f;
        if (offsetLength > maxOffset && offsetLength > 1e-4f) {
            offset = offset * (maxOffset / offsetLength);
            car_.pos = nearest + offset;
        }
    }

    glm::vec2 forward2{std::cos(car_.heading), std::sin(car_.heading)};
    glm::vec3 target{car_.pos.x, 0.5f, car_.pos.y};
    glm::vec3 desiredPos = target - glm::vec3(forward2.x, 0.0f, forward2.y) * 8.0f + glm::vec3(0.0f, 3.0f, 0.0f);
    float smoothing = 1.0f - std::exp(-dt * 6.0f);
    cameraPos_ = glm::mix(cameraPos_, desiredPos, smoothing);
    cameraTarget_ = glm::mix(cameraTarget_, target, smoothing);
}

void Renderer::resetCamera() {
    glm::vec2 forward2{std::cos(car_.heading), std::sin(car_.heading)};
    glm::vec3 target{car_.pos.x, 0.5f, car_.pos.y};
    cameraTarget_ = target;
    cameraPos_ = target - glm::vec3(forward2.x, 0.0f, forward2.y) * 8.0f + glm::vec3(0.0f, 3.0f, 0.0f);
}

