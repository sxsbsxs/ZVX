package com.zvx.player.platform;

import android.content.Context;
import androidx.media3.common.C;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.audio.BaseAudioProcessor;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Media3 processor routing decoded PCM16 audio to the native three-band tone engine. */
public final class NativeAudioProcessor extends BaseAudioProcessor {
    public static final int PROFILE_FLAT = 0;
    public static final int PROFILE_WARM = 1;
    public static final int PROFILE_VOCAL = 2;
    public static final int PROFILE_BRIGHT = 3;
    private static final String PREFS_NAME = "zvx_audio_tone";
    private static final String KEY_PROFILE = "profile";
    private static volatile int activeProfile = PROFILE_FLAT;
    private long nativeHandle;

    public NativeAudioProcessor(Context context) {
        activeProfile = getProfile(context);
    }

    public static int getProfile(Context context) {
        int stored = context.getApplicationContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_PROFILE, PROFILE_FLAT);
        activeProfile = clampProfile(stored);
        return activeProfile;
    }

    public static void setProfile(Context context, int profile) {
        int safeProfile = clampProfile(profile);
        activeProfile = safeProfile;
        context.getApplicationContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_PROFILE, safeProfile)
            .apply();
    }

    private static int clampProfile(int profile) {
        return Math.max(PROFILE_FLAT, Math.min(PROFILE_BRIGHT, profile));
    }

    @Override
    public AudioFormat onConfigure(AudioFormat inputAudioFormat)
            throws AudioProcessor.UnhandledAudioFormatException {
        releaseNativeHandle();
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT ||
                inputAudioFormat.sampleRate <= 0 || inputAudioFormat.channelCount <= 0) {
            return AudioFormat.NOT_SET;
        }
        nativeHandle = NativeMediaBridge.createAudioDsp(inputAudioFormat.sampleRate, inputAudioFormat.channelCount);
        return nativeHandle != 0L ? inputAudioFormat : AudioFormat.NOT_SET;
    }

    @Override
    public void queueInput(ByteBuffer inputBuffer) {
        int byteCount = inputBuffer.remaining();
        if (byteCount == 0) return;
        ByteBuffer outputBuffer = replaceOutputBuffer(byteCount);
        outputBuffer.order(ByteOrder.nativeOrder());
        outputBuffer.put(inputBuffer);
        int profile = activeProfile;
        if (nativeHandle != 0L && profile != PROFILE_FLAT) {
            NativeMediaBridge.processAudioDsp(nativeHandle, outputBuffer, byteCount, profile);
        }
        outputBuffer.flip();
    }

    @Override
    protected void onFlush(AudioProcessor.StreamMetadata streamMetadata) {
        if (nativeHandle != 0L) NativeMediaBridge.resetAudioDsp(nativeHandle);
    }

    @Override
    protected void onReset() {
        releaseNativeHandle();
    }

    private void releaseNativeHandle() {
        if (nativeHandle != 0L) {
            NativeMediaBridge.releaseAudioDsp(nativeHandle);
            nativeHandle = 0L;
        }
    }
}
