#include <jni.h>
#include <algorithm>
#include <string>
#include "subtitle_timeline.h"

namespace {
constexpr char kEngineVersion[] = "ZVX Native Core 0.1.0";
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_zvx_player_platform_NativeMediaBridge_nativeEngineVersion(
    JNIEnv* env, jclass /* clazz */) {
    return env->NewStringUTF(kEngineVersion);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_zvx_player_platform_NativeMediaBridge_nativeClampSeekPosition(
    JNIEnv* /* env */, jclass /* clazz */, jlong requested_ms, jlong duration_ms) {
    const jlong non_negative = std::max<jlong>(0, requested_ms);
    if (duration_ms <= 0) return non_negative;
    return std::min(non_negative, duration_ms);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_zvx_player_platform_NativeMediaBridge_nativeShiftSubtitleTimeline(
    JNIEnv* env, jclass /* clazz */, jstring content, jint offset_ms) {
    if (content == nullptr) return nullptr;
    const char* utf_chars = env->GetStringUTFChars(content, nullptr);
    if (utf_chars == nullptr) return nullptr;
    const std::string input(utf_chars);
    env->ReleaseStringUTFChars(content, utf_chars);

    const std::string shifted = zvx::shiftSubtitleTimeline(input, offset_ms);
    return env->NewStringUTF(shifted.c_str());
}
