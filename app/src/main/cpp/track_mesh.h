#pragma once

#include <cstdint>
#include <vector>

#include "math_glm/glm.hpp"

struct TrackMesh {
    std::vector<float> vertices;
    std::vector<uint32_t> indices;
    int stride = 5;
};

struct TrackGeometry {
    std::vector<glm::vec2> samples;
    std::vector<glm::vec2> tangents;
    float width = 0.0f;
    float totalLength = 0.0f;
};

bool buildTrackMesh(const std::vector<glm::vec2> &centerline,
                    float width,
                    TrackMesh &outMesh,
                    TrackGeometry &outGeometry);

