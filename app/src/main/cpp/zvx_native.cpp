#include <jni.h>
#include <algorithm>
#include <string>

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
