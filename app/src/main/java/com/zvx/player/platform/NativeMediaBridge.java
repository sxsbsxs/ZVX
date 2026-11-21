package com.zvx.player.platform;

import java.nio.ByteBuffer;

/**
 * Small Java interoperability layer for Android APIs and the native ZVX core.
 * Native calls are lazy: the app can still use its Kotlin/Media3 path if the
 * native library is unavailable on a particular device or ABI.
 */
public final class NativeMediaBridge {
    private static final boolean AVAILABLE;

    static {
        boolean loaded;
        try {
            System.loadLibrary("zvx_native");
            loaded = true;
        } catch (UnsatisfiedLinkError error) {
            loaded = false;
        }
        AVAILABLE = loaded;
    }

    private NativeMediaBridge() {}

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    public static String engineVersion() {
        return AVAILABLE ? nativeEngineVersion() : "native-unavailable";
    }

    /**
     * Runs the native C++ cue-time shifter when available; null asks the caller
     * to use the portable Kotlin fallback.
     */
    public static String shiftSubtitleTimeline(String content, int offsetMs) {
        if (!AVAILABLE || content == null) return null;
        try {
            return nativeShiftSubtitleTimeline(content, offsetMs);
        } catch (UnsatisfiedLinkError error) {
            return null;
        }
    }

    public static long createAudioDsp(int sampleRate, int channelCount) {
        if (!AVAILABLE || sampleRate < 8_000 || sampleRate > 384_000 || channelCount < 1 || channelCount > 8) return 0L;
        try {
            return nativeCreateAudioDsp(sampleRate, channelCount);
        } catch (UnsatisfiedLinkError error) {
            return 0L;
        }
    }

    public static boolean processAudioDsp(long handle, ByteBuffer buffer, int byteCount, int profile) {
        if (!AVAILABLE || handle == 0L || buffer == null || !buffer.isDirect() ||
                byteCount < 0 || byteCount > buffer.capacity() || (byteCount & 1) != 0) return false;
        try {
            nativeProcessAudioDsp(handle, buffer, byteCount, profile);
            return true;
        } catch (UnsatisfiedLinkError error) {
            return false;
        }
    }

    public static void resetAudioDsp(long handle) {
        if (!AVAILABLE || handle == 0L) return;
        try { nativeResetAudioDsp(handle); } catch (UnsatisfiedLinkError ignored) { }
    }

    public static void releaseAudioDsp(long handle) {
        if (!AVAILABLE || handle == 0L) return;
        try { nativeReleaseAudioDsp(handle); } catch (UnsatisfiedLinkError ignored) { }
    }

    public static long clampSeekPosition(long requestedMs, long durationMs) {
        if (AVAILABLE) {
            return nativeClampSeekPosition(requestedMs, durationMs);
        }
        if (requestedMs < 0L) return 0L;
        return durationMs > 0L ? Math.min(requestedMs, durationMs) : requestedMs;
    }

    private static native String nativeEngineVersion();
    private static native long nativeClampSeekPosition(long requestedMs, long durationMs);
    private static native String nativeShiftSubtitleTimeline(String content, int offsetMs);
    private static native long nativeCreateAudioDsp(int sampleRate, int channelCount);
    private static native void nativeProcessAudioDsp(long handle, ByteBuffer buffer, int byteCount, int profile);
    private static native void nativeResetAudioDsp(long handle);
    private static native void nativeReleaseAudioDsp(long handle);
}
