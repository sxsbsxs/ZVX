package com.zvx.player.data

import androidx.media3.common.Format
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackDiagnosticsSnapshot(
    val mimeType: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val frameRate: Float = Format.NO_VALUE.toFloat(),
    val bitrate: Int = Format.NO_VALUE,
    val decoderName: String = "",
    val decoderInitializationMs: Long = 0L,
    val droppedFrames: Int = 0,
    val droppedFrameElapsedMs: Long = 0L,
    val codecError: String = ""
)

/** Thread-safe playback telemetry written by Media3 and observed by the player UI. */
object PlaybackDiagnostics {
    private val mutableSnapshot = MutableStateFlow(PlaybackDiagnosticsSnapshot())
    val snapshot: StateFlow<PlaybackDiagnosticsSnapshot> = mutableSnapshot.asStateFlow()

    @Synchronized
    fun reset() {
        mutableSnapshot.value = PlaybackDiagnosticsSnapshot()
    }

    @Synchronized
    fun recordVideoFormat(format: Format) {
        mutableSnapshot.value = mutableSnapshot.value.copy(
            mimeType = format.sampleMimeType.orEmpty(),
            width = format.width,
            height = format.height,
            frameRate = format.frameRate,
            bitrate = format.bitrate
        )
    }

    @Synchronized
    fun recordDecoder(name: String, initializationMs: Long) {
        mutableSnapshot.value = mutableSnapshot.value.copy(
            decoderName = name,
            decoderInitializationMs = initializationMs
        )
    }

    @Synchronized
    fun addDroppedFrames(count: Int, elapsedMs: Long) {
        if (count <= 0) return
        mutableSnapshot.value = mutableSnapshot.value.copy(
            droppedFrames = mutableSnapshot.value.droppedFrames + count,
            droppedFrameElapsedMs = mutableSnapshot.value.droppedFrameElapsedMs + elapsedMs.coerceAtLeast(0L)
        )
    }

    @Synchronized
    fun recordCodecError(message: String) {
        mutableSnapshot.value = mutableSnapshot.value.copy(codecError = message)
    }
}
