#include <jni.h>
#include <algorithm>
#include <string>
#include "subtitle_timeline.h"
#include "audio_dsp.h"

#include <cstdint>
#include <new>

namespace {
constexpr char kEngineVersion[] = "ZVX Native Core 0.2.0";
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


extern "C" JNIEXPORT jlong JNICALL
Java_com_zvx_player_platform_NativeMediaBridge_nativeCreateAudioDsp(
    JNIEnv* /* env */, jclass /* clazz */, jint sample_rate, jint channel_count) {
    if (sample_rate < 8'000 || sample_rate > 384'000 || channel_count < 1 || channel_count > 8) return 0;
    try {
        return reinterpret_cast<jlong>(new zvx::AudioDsp(sample_rate, channel_count));
    } catch (...) {
        return 0;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_zvx_player_platform_NativeMediaBridge_nativeProcessAudioDsp(
    JNIEnv* env, jclass /* clazz */, jlong handle, jobject buffer, jint byte_count, jint profile) {
    if (handle == 0 || buffer == nullptr || byte_count < 0 || (byte_count & 1) != 0) return;
    auto* samples = static_cast<std::int16_t*>(env->GetDirectBufferAddress(buffer));
    const jlong capacity = env->GetDirectBufferCapacity(buffer);
    if (samples == nullptr || capacity < 0 || static_cast<jlong>(byte_count) > capacity) return;
    reinterpret_cast<zvx::AudioDsp*>(handle)->process(
        samples, static_cast<std::size_t>(byte_count) / sizeof(std::int16_t), profile);
}

extern "C" JNIEXPORT void JNICALL
Java_com_zvx_player_platform_NativeMediaBridge_nativeResetAudioDsp(
    JNIEnv* /* env */, jclass /* clazz */, jlong handle) {
    if (handle != 0) reinterpret_cast<zvx::AudioDsp*>(handle)->reset();
}

extern "C" JNIEXPORT void JNICALL
Java_com_zvx_player_platform_NativeMediaBridge_nativeReleaseAudioDsp(
    JNIEnv* /* env */, jclass /* clazz */, jlong handle) {
    if (handle != 0) delete reinterpret_cast<zvx::AudioDsp*>(handle);
}
