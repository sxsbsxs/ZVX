package com.zvx.player.platform;

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

    public static long clampSeekPosition(long requestedMs, long durationMs) {
        if (AVAILABLE) {
            return nativeClampSeekPosition(requestedMs, durationMs);
        }
        if (requestedMs < 0L) return 0L;
        return durationMs > 0L ? Math.min(requestedMs, durationMs) : requestedMs;
    }

    private static native String nativeEngineVersion();
    private static native long nativeClampSeekPosition(long requestedMs, long durationMs);
}
