#include <jni.h>
#include <android/log.h>

#include "renderer.h"

namespace {
constexpr const char *kTag = "JNI";
}

extern "C" {

JNIEXPORT void JNICALL
Java_com_example_racingsim_preview_PreviewActivity_nativeInit(JNIEnv *, jclass) {
    GetRenderer().init();
}

JNIEXPORT void JNICALL
Java_com_example_racingsim_preview_PreviewActivity_nativeResize(JNIEnv *, jclass, jint width, jint height) {
    GetRenderer().resize(width, height);
}

JNIEXPORT void JNICALL
Java_com_example_racingsim_preview_PreviewActivity_nativeRender(JNIEnv *, jclass) {
    GetRenderer().render();
}

JNIEXPORT void JNICALL
Java_com_example_racingsim_preview_PreviewActivity_nativeOnTouch(JNIEnv *, jclass, jint action, jfloat x, jfloat y) {
    GetRenderer().onTouch(action, x, y);
}

JNIEXPORT void JNICALL
Java_com_example_racingsim_preview_PreviewActivity_nativeLoadTrack(JNIEnv *env, jclass, jfloatArray pointsArray, jint count, jfloat width) {
    if (pointsArray == nullptr) {
        __android_log_print(ANDROID_LOG_WARN, kTag, "nativeLoadTrack received null array");
        return;
    }
    jsize length = env->GetArrayLength(pointsArray);
    if (length < count * 2) {
        __android_log_print(ANDROID_LOG_WARN, kTag, "nativeLoadTrack invalid length=%d for count=%d", length, count);
        return;
    }
    jboolean isCopy = JNI_FALSE;
    jfloat *data = env->GetFloatArrayElements(pointsArray, &isCopy);
    if (!data) {
        __android_log_print(ANDROID_LOG_WARN, kTag, "nativeLoadTrack unable to access array");
        return;
    }
    GetRenderer().loadTrack(data, count, width);
    env->ReleaseFloatArrayElements(pointsArray, data, JNI_ABORT);
}

}

