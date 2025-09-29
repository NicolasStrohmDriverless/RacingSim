cmake_minimum_required(VERSION 3.22.1)

project(racingsim)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

add_library(racingsim SHARED
        jni_bridge.cpp
        renderer.cpp
        track_mesh.cpp
        vehicle.cpp)

target_include_directories(racingsim
        PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_CURRENT_SOURCE_DIR}/math_glm)

find_library(log-lib log)
find_library(android-lib android)
find_library(gles-lib GLESv3)
find_library(egl-lib EGL)

target_link_libraries(racingsim
        PRIVATE
        ${log-lib}
        ${android-lib}
        ${gles-lib}
        ${egl-lib})
