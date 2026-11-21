package com.zvx.player

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Android media playback foreground service.
 *
 * Media3 publishes system media controls and a playback notification while
 * audio is playing, including when the activity is no longer visible.
 */
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSession.Builder(this, PlaybackPlayerHolder.get(this)).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        PlaybackPlayerHolder.release()
        super.onDestroy()
    }
}
