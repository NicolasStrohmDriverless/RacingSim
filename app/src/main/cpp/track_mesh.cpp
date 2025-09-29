#include "track_mesh.h"

#include <algorithm>
#include <android/log.h>

namespace {

constexpr const char *kTag = "TrackMesh";

std::vector<float> buildCumulativeDistances(const std::vector<glm::vec2> &points) {
    std::vector<float> cumulative(points.size(), 0.0f);
    for (size_t i = 1; i < points.size(); ++i) {
        cumulative[i] = cumulative[i - 1] + glm::distance(points[i - 1], points[i]);
    }
    return cumulative;
}

std::vector<glm::vec2> resample(const std::vector<glm::vec2> &points, float spacing) {
    if (points.size() < 2 || spacing <= 0.0f) {
        return points;
    }
    std::vector<float> cumulative = buildCumulativeDistances(points);
    float totalLength = cumulative.back();
    if (totalLength < spacing * 0.5f) {
        return points;
    }
    std::vector<glm::vec2> result;
    result.reserve(static_cast<size_t>(totalLength / spacing) + 2);
    result.push_back(points.front());
    int sampleCount = static_cast<int>(totalLength / spacing);
    for (int i = 1; i <= sampleCount; ++i) {
        float target = spacing * static_cast<float>(i);
        if (target >= totalLength) {
            break;
        }
        auto it = std::lower_bound(cumulative.begin(), cumulative.end(), target);
        size_t index = static_cast<size_t>(std::max<int>(0, static_cast<int>(it - cumulative.begin()) - 1));
        size_t nextIndex = std::min(points.size() - 1, index + 1);
        float segmentStart = cumulative[index];
        float segmentEnd = cumulative[nextIndex];
        float segmentLength = std::max(segmentEnd - segmentStart, 1e-4f);
        float factor = (target - segmentStart) / segmentLength;
        glm::vec2 sample = points[index] + (points[nextIndex] - points[index]) * factor;
        result.push_back(sample);
    }
    if (glm::distance(result.back(), points.back()) > 1e-3f) {
        result.push_back(points.back());
    }
    return result;
}

glm::vec2 computeTangent(const std::vector<glm::vec2> &samples, size_t index) {
    if (samples.size() < 2) {
        return {1.0f, 0.0f};
    }
    if (index == 0) {
        return glm::normalize(samples[1] - samples[0]);
    }
    if (index == samples.size() - 1) {
        return glm::normalize(samples[index] - samples[index - 1]);
    }
    return glm::normalize(samples[index + 1] - samples[index - 1]);
}

} // namespace

bool buildTrackMesh(const std::vector<glm::vec2> &centerline,
                    float width,
                    TrackMesh &outMesh,
                    TrackGeometry &outGeometry) {
    if (centerline.size() < 2 || width <= 0.0f) {
        __android_log_print(ANDROID_LOG_WARN, kTag, "Invalid track data: points=%zu width=%f",
                             centerline.size(), width);
        return false;
    }

    float spacing = std::max(width * 0.25f, 1.0f);
    std::vector<glm::vec2> samples = resample(centerline, spacing);
    std::vector<float> cumulative = buildCumulativeDistances(samples);

    if (samples.size() < 2) {
        __android_log_print(ANDROID_LOG_WARN, kTag, "Resampled track too short");
        return false;
    }

    outGeometry.samples = samples;
    outGeometry.width = width;
    outGeometry.totalLength = cumulative.back();
    outGeometry.tangents.resize(samples.size());

    float halfWidth = width * 0.5f;
    float uvScale = 1.0f / std::max(width, 1.0f);

    outMesh.vertices.clear();
    outMesh.indices.clear();
    outMesh.stride = 5;
    outMesh.vertices.reserve(samples.size() * 2 * outMesh.stride);
    outMesh.indices.reserve((samples.size() - 1) * 6);

    for (size_t i = 0; i < samples.size(); ++i) {
        glm::vec2 tangent = computeTangent(samples, i);
        outGeometry.tangents[i] = tangent;
        glm::vec2 normal{-tangent.y, tangent.x};
        normal = glm::normalize(normal);

        glm::vec2 center = samples[i];
        glm::vec2 left = center + normal * halfWidth;
        glm::vec2 right = center - normal * halfWidth;

        float v = cumulative[i] * uvScale;

        outMesh.vertices.push_back(left.x);
        outMesh.vertices.push_back(0.0f);
        outMesh.vertices.push_back(left.y);
        outMesh.vertices.push_back(0.0f);
        outMesh.vertices.push_back(v);

        outMesh.vertices.push_back(right.x);
        outMesh.vertices.push_back(0.0f);
        outMesh.vertices.push_back(right.y);
        outMesh.vertices.push_back(1.0f);
        outMesh.vertices.push_back(v);

        if (i + 1 < samples.size()) {
            uint32_t base = static_cast<uint32_t>(i * 2);
            outMesh.indices.push_back(base);
            outMesh.indices.push_back(base + 1);
            outMesh.indices.push_back(base + 2);

            outMesh.indices.push_back(base + 1);
            outMesh.indices.push_back(base + 3);
            outMesh.indices.push_back(base + 2);
        }
    }

    return true;
}

