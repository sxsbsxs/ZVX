package com.zvx.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.zvx.player.data.PlaybackDiagnostics
import com.zvx.player.platform.NativeAudioProcessor

/**
 * Owns the playback engine independently of the UI Activity.
 *
 * Streaming and local files use separate load-control thresholds. The selector
 * follows the physical display by default, while the UI can still pin a source
 * track when the user explicitly chooses a video resolution.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val nativeAudioProcessor = NativeAudioProcessor(this)
        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioOutputPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioOutputPlaybackParameters(enableAudioOutputPlaybackParams)
                    .setAudioProcessors(arrayOf(nativeAudioProcessor))
                    .build()
            }
        }.setEnableDecoderFallback(true)

        val trackSelector = DefaultTrackSelector(this).apply {
            setParameters(
                buildUponParameters().setViewportSizeToPhysicalDisplaySize(true)
            )
        }
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMsForStreaming(
                50_000,
                90_000,
                2_500,
                5_000
            )
            .setBufferDurationsMsForLocalPlayback(
                1_000,
                50_000,
                1_000,
                1_000
            )
            .build()

        val exoPlayer = ExoPlayer.Builder(this, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                PlaybackDiagnostics.reset()
            }

            override fun onTracksChanged(tracks: Tracks) {
                val selectedVideoFormat = tracks.groups
                    .asSequence()
                    .filter { it.type == C.TRACK_TYPE_VIDEO }
                    .flatMap { group ->
                        (0 until group.length).asSequence()
                            .filter { group.isTrackSelected(it) }
                            .map { group.getTrackFormat(it) }
                    }
                    .firstOrNull()
                if (selectedVideoFormat != null) {
                    PlaybackDiagnostics.recordVideoFormat(selectedVideoFormat)
                }
            }
        })
        exoPlayer.addAnalyticsListener(object : AnalyticsListener {
            override fun onVideoDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long
            ) {
                PlaybackDiagnostics.recordDecoder(decoderName, initializationDurationMs)
            }

            override fun onDroppedVideoFrames(
                eventTime: AnalyticsListener.EventTime,
                droppedFrames: Int,
                elapsedMs: Long
            ) {
                PlaybackDiagnostics.addDroppedFrames(droppedFrames, elapsedMs)
            }

            override fun onVideoCodecError(
                eventTime: AnalyticsListener.EventTime,
                videoCodecError: Exception
            ) {
                PlaybackDiagnostics.recordCodecError(videoCodecError.message ?: videoCodecError.javaClass.simpleName)
            }
        })

        player = exoPlayer
        mediaSession = MediaSession.Builder(this, exoPlayer).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        super.onDestroy()
    }
}
