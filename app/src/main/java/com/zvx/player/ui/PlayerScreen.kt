package com.zvx.player.ui

import android.app.Activity
import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.app.PictureInPictureParams
import android.util.Rational
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.zvx.player.data.PlaybackProgressStore
import com.zvx.player.data.PlaybackSettingsStore
import com.zvx.player.data.PlaybackPreferences
import com.zvx.player.data.SubtitleTimeline
import com.zvx.player.data.MediaEntry
import com.zvx.player.platform.MediaMetadataProbe
import com.zvx.player.platform.NativeAudioProcessor
import com.zvx.player.platform.NativeMediaBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    videoUri: Uri,
    modifier: Modifier = Modifier,
    isAudio: Boolean = false,
    languageCode: String = "en",
    queue: List<MediaEntry> = emptyList(),
    onQueueChanged: (List<MediaEntry>, Uri?) -> Unit = { _, _ -> },
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sessionToken = remember(context) {
        SessionToken(context, ComponentName(context, com.zvx.player.PlaybackService::class.java))
    }
    val controllerFuture = remember(sessionToken) {
        MediaController.Builder(context, sessionToken).buildAsync()
    }
    var controller by remember(controllerFuture) { mutableStateOf<MediaController?>(null) }
    var connectionError by remember(controllerFuture) { mutableStateOf<String?>(null) }

    DisposableEffect(controllerFuture) {
        val listener = Runnable {
            try {
                controller = controllerFuture.get()
            } catch (error: Exception) {
                connectionError = error.message ?: "Unable to connect to the playback service."
            }
        }
        controllerFuture.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {
            MediaController.releaseFuture(controllerFuture)
        }
    }

    val activeController = controller
    if (activeController == null) {
        Box(
            modifier = modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                connectionError ?: tr("Connecting to ZVX player…", languageCode),
                color = Color.White
            )
        }
    } else {
        PlayerScreenContent(
            videoUri = videoUri,
            modifier = modifier,
            isAudio = isAudio,
            languageCode = languageCode,
            queue = queue,
            onQueueChanged = onQueueChanged,
            onBack = onBack,
            player = activeController
        )
    }
}

@Composable
private fun PlayerScreenContent(
    videoUri: Uri,
    modifier: Modifier,
    isAudio: Boolean,
    languageCode: String,
    queue: List<MediaEntry>,
    onQueueChanged: (List<MediaEntry>, Uri?) -> Unit,
    onBack: () -> Unit,
    player: Player
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val storedPreferences = remember(context) { PlaybackSettingsStore.load(context) }
    val originalScreenBrightness = remember(activity) { activity?.window?.attributes?.screenBrightness ?: -1f }
    val originalRequestedOrientation = remember(activity) { activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    var immersiveLandscape by remember(videoUri) { mutableStateOf(false) }
    val savedPosition = remember(videoUri) {
        PlaybackProgressStore.getPosition(context, videoUri.toString())
    }
    var controlsVisible by remember(videoUri) { mutableStateOf(true) }
    var isPlaying by remember(videoUri) { mutableStateOf(true) }
    var currentPosition by remember(videoUri) { mutableLongStateOf(0L) }
    var duration by remember(videoUri) { mutableLongStateOf(0L) }
    var scrubPosition by remember(videoUri) { mutableStateOf<Long?>(null) }
    var playbackError by remember(videoUri) { mutableStateOf<String?>(null) }
    var speedIndex by remember(videoUri) { mutableIntStateOf(storedPreferences.speedIndex) }
    var trackDialogVisible by remember(videoUri) { mutableStateOf(false) }
    var queueDialogVisible by remember(videoUri) { mutableStateOf(false) }
    var editableQueue by remember(videoUri, queue) { mutableStateOf(queue.toList()) }
    var optionsDialogVisible by remember(videoUri) { mutableStateOf(false) }
    var aspectModeIndex by remember(videoUri) { mutableIntStateOf(storedPreferences.aspectMode) }
    var sleepTimerMinutes by remember(videoUri) { mutableIntStateOf(0) }
    var subtitleSizeSp by remember(videoUri) { mutableIntStateOf(storedPreferences.subtitleSizeSp) }
    var subtitleBackgroundEnabled by remember(videoUri) { mutableStateOf(storedPreferences.subtitleBackgroundEnabled) }
    var subtitleDelayMs by remember(videoUri) { mutableIntStateOf(storedPreferences.subtitleDelayMs) }
    var externalSubtitleUri by remember(videoUri) { mutableStateOf<Uri?>(null) }
    var externalSubtitleMimeType by remember(videoUri) { mutableStateOf<String?>(null) }
    var externalSubtitleLabel by remember(videoUri) { mutableStateOf<String?>(null) }
    var externalSubtitleTargetUri by remember(videoUri) { mutableStateOf<Uri?>(null) }
    var shuffleEnabled by remember(player) { mutableStateOf(player.shuffleModeEnabled) }
    var repeatMode by remember(player) { mutableIntStateOf(player.repeatMode) }
    var currentMediaIndex by remember(player) { mutableIntStateOf(player.currentMediaItemIndex) }
    var queueSize by remember(player) { mutableIntStateOf(player.mediaItemCount) }
    var currentTitle by remember(videoUri) { mutableStateOf(videoUri.lastPathSegment?.substringAfterLast('/')?.take(64) ?: "Now playing") }
    var currentArtist by remember(videoUri) { mutableStateOf("") }
    var currentAlbum by remember(videoUri) { mutableStateOf("") }
    var audioArtwork by remember(videoUri) { mutableStateOf<ImageBitmap?>(null) }
    var seekFeedback by remember(videoUri) { mutableStateOf<String?>(null) }
    var availableTracks by remember(player) { mutableStateOf(player.currentTracks) }
    var audioToneProfile by remember(context) {
        mutableIntStateOf(NativeAudioProcessor.getProfile(context))
    }
    val speeds = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
    val aspectModeLabels = listOf("Fit", "Fill", "Crop")
    val title = currentTitle
    val leavePlayer = {
        if (!isAudio) player.pause()
        onBack()
    }

    LaunchedEffect(player, videoUri) {
        val currentUri = player.currentMediaItem?.localConfiguration?.uri
        if (currentUri != videoUri) {
            val startPosition = savedPosition.takeIf { it >= 5_000L } ?: C.TIME_UNSET
            val playlist = editableQueue.map { entry ->
                MediaItem.Builder()
                    .setUri(entry.uri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(entry.title)
                            .setArtist(entry.artist.orEmpty())
                            .setAlbumTitle(entry.album)
                            .build()
                    )
                    .build()
            }
            val selectedIndex = editableQueue.indexOfFirst { it.uri.toString() == videoUri.toString() }
            if (playlist.isNotEmpty() && selectedIndex >= 0) {
                player.setMediaItems(playlist, selectedIndex, startPosition)
            } else {
                val mediaItem = MediaItem.Builder()
                    .setUri(videoUri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(title)
                            .setArtist("ZVX")
                            .build()
                    )
                    .build()
                player.setMediaItem(mediaItem, startPosition)
            }
            player.prepare()
        }
        player.playWhenReady = true
        player.play()
    }

    val subtitlePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { selectedUri ->
        if (selectedUri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    selectedUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Temporary access is still valid for this playback session.
            }

            val mimeType = subtitleMimeType(
                context.contentResolver.getType(selectedUri),
                selectedUri.lastPathSegment
            )
            if (mimeType == null) {
                playbackError = "Unsupported subtitle. Choose SRT, VTT, TTML, SSA or ASS."
            } else {
                val currentItem = player.currentMediaItem
                val targetUri = currentItem?.localConfiguration?.uri
                if (currentItem == null || targetUri == null) {
                    playbackError = "There is no active media item to attach a subtitle to."
                } else {
                    externalSubtitleUri = selectedUri
                    externalSubtitleMimeType = mimeType
                    externalSubtitleLabel = selectedUri.lastPathSegment?.substringAfterLast('/') ?: "External subtitle"
                    externalSubtitleTargetUri = targetUri
                    playbackError = null
                }
            }
        }
    }

    LaunchedEffect(
        externalSubtitleUri,
        externalSubtitleMimeType,
        externalSubtitleLabel,
        externalSubtitleTargetUri,
        subtitleDelayMs,
        currentMediaIndex
    ) {
        val sourceUri = externalSubtitleUri ?: return@LaunchedEffect
        val mimeType = externalSubtitleMimeType ?: return@LaunchedEffect
        val subtitleLabel = externalSubtitleLabel ?: "External subtitle"
        val targetUri = externalSubtitleTargetUri ?: return@LaunchedEffect
        val activeItem = player.currentMediaItem ?: return@LaunchedEffect
        if (activeItem.localConfiguration?.uri != targetUri) return@LaunchedEffect

        val preparedUri = try {
            if (subtitleDelayMs == 0 ||
                (mimeType != MimeTypes.APPLICATION_SUBRIP && mimeType != MimeTypes.TEXT_VTT)
            ) {
                sourceUri
            } else {
                withContext(Dispatchers.IO) {
                    val content = context.contentResolver.openInputStream(sourceUri)
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                        ?: throw IOException("Unable to read the selected subtitle.")
                    if (content.length > 5 * 1024 * 1024) {
                        throw IOException("Subtitle file is larger than 5 MB.")
                    }
                    val shiftedContent = SubtitleTimeline.shift(content, subtitleDelayMs)
                    val outputDirectory = File(context.cacheDir, "zvx-subtitles").apply { mkdirs() }
                    val extension = if (mimeType == MimeTypes.TEXT_VTT) "vtt" else "srt"
                    val outputFile = File(
                        outputDirectory,
                        "subtitle_${sourceUri.toString().hashCode()}_${subtitleDelayMs}.$extension"
                    )
                    outputFile.writeText(shiftedContent, Charsets.UTF_8)
                    Uri.fromFile(outputFile)
                }
            }
        } catch (error: Exception) {
            playbackError = "Unable to apply subtitle timing: ${error.message ?: "unknown error"}"
            return@LaunchedEffect
        }

        val currentItem = player.currentMediaItem ?: return@LaunchedEffect
        if (currentItem.localConfiguration?.uri != targetUri) return@LaunchedEffect
        val subtitle = MediaItem.SubtitleConfiguration.Builder(preparedUri)
            .setMimeType(mimeType)
            .setLanguage("und")
            .setLabel(subtitleLabel)
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
        val existing = currentItem.localConfiguration?.subtitleConfigurations.orEmpty()
            .filterNot { it.label == subtitleLabel }
        val updatedItem = currentItem.buildUpon()
            .setSubtitleConfigurations(existing + subtitle)
            .build()
        player.replaceMediaItem(player.currentMediaItemIndex, updatedItem)
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .build()
        playbackError = null
    }

    BackHandler { leavePlayer() }

    DisposableEffect(activity) {
        onDispose {
            activity?.let { host ->
                host.requestedOrientation = originalRequestedOrientation
                host.window?.let { window ->
                    WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
                    val attributes = window.attributes
                    attributes.screenBrightness = originalScreenBrightness
                    window.attributes = attributes
                }
            }
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onTracksChanged(tracks: Tracks) {
                availableTracks = tracks
            }

            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                queueSize = player.mediaItemCount
                currentMediaIndex = player.currentMediaItemIndex
                onQueueChanged(editableQueue, player.currentMediaItem?.localConfiguration?.uri)
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                shuffleEnabled = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(repeatModeValue: Int) {
                repeatMode = repeatModeValue
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentMediaIndex = player.currentMediaItemIndex
                queueSize = player.mediaItemCount
                duration = 0L
                onQueueChanged(editableQueue, mediaItem?.localConfiguration?.uri)
                currentTitle = mediaItem?.mediaMetadata?.title?.toString()
                    ?: mediaItem?.localConfiguration?.uri?.lastPathSegment?.substringAfterLast('/')
                    ?: "Now playing"
                currentArtist = mediaItem?.mediaMetadata?.artist?.toString().orEmpty()
                currentAlbum = mediaItem?.mediaMetadata?.albumTitle?.toString().orEmpty()
                val itemUri = mediaItem?.localConfiguration?.uri?.toString()
                val resumePosition = itemUri?.let { PlaybackProgressStore.getPosition(context, it) } ?: 0L
                if (resumePosition >= 5_000L && player.currentPosition < 5_000L) {
                    player.seekTo(resumePosition)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = error.message ?: error.errorCodeName
                controlsVisible = true
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            val finalDuration = player.duration
            val activeUri = player.currentMediaItem?.localConfiguration?.uri?.toString()
            if (activeUri != null && finalDuration > 0L && finalDuration != C.TIME_UNSET) {
                PlaybackProgressStore.save(
                    context,
                    activeUri,
                    player.currentPosition,
                    finalDuration
                )
            }
        }
    }

    LaunchedEffect(player) {
        while (true) {
            currentPosition = player.currentPosition.coerceAtLeast(0L)
            val playerDuration = player.duration
            if (playerDuration != C.TIME_UNSET && playerDuration > 0L) {
                duration = playerDuration
            }
            isPlaying = player.isPlaying
            val currentDuration = player.duration
            val activeUri = player.currentMediaItem?.localConfiguration?.uri?.toString()
            if (activeUri != null && currentDuration > 0L && currentDuration != C.TIME_UNSET) {
                PlaybackProgressStore.save(
                    context,
                    activeUri,
                    player.currentPosition,
                    currentDuration
                )
            }
            delay(5_000L)
        }
    }

    LaunchedEffect(isAudio, currentMediaIndex, videoUri) {
        if (isAudio) {
            val artworkUri = player.currentMediaItem?.localConfiguration?.uri ?: videoUri
            val snapshot = withContext(Dispatchers.IO) {
                MediaMetadataProbe.inspect(context, artworkUri)
            }
            audioArtwork = snapshot?.artwork?.asImageBitmap()
            if (duration <= 0L && (snapshot?.durationMs ?: 0L) > 0L) {
                duration = snapshot?.durationMs ?: 0L
            }
            if (currentArtist.isBlank() && !snapshot?.artist.isNullOrBlank()) {
                currentArtist = snapshot?.artist.orEmpty()
            }
            if (currentAlbum.isBlank() && !snapshot?.album.isNullOrBlank()) {
                currentAlbum = snapshot?.album.orEmpty()
            }
            if (!snapshot?.title.isNullOrBlank()) {
                currentTitle = snapshot?.title.orEmpty()
            }
        } else {
            audioArtwork = null
        }
    }

    LaunchedEffect(speedIndex, aspectModeIndex, subtitleSizeSp, subtitleBackgroundEnabled, subtitleDelayMs) {
        PlaybackSettingsStore.save(
            context,
            PlaybackPreferences(
                speedIndex = speedIndex,
                aspectMode = aspectModeIndex,
                subtitleSizeSp = subtitleSizeSp,
                subtitleBackgroundEnabled = subtitleBackgroundEnabled,
                subtitleDelayMs = subtitleDelayMs
            )
        )
    }

    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying && !trackDialogVisible) {
            delay(3_500L)
            controlsVisible = false
        }
    }

    LaunchedEffect(seekFeedback) {
        if (seekFeedback != null) {
            delay(800L)
            seekFeedback = null
        }
    }

    LaunchedEffect(sleepTimerMinutes) {
        val selectedMinutes = sleepTimerMinutes
        if (selectedMinutes > 0) {
            delay(selectedMinutes.toLong() * 60_000L)
            if (sleepTimerMinutes == selectedMinutes) {
                player.pause()
                sleepTimerMinutes = 0
                controlsVisible = true
                seekFeedback = "Sleep timer finished"
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize().pointerInput(player) {
                detectTapGestures(
                    onTap = { controlsVisible = !controlsVisible },
                    onDoubleTap = { offset ->
                        val deltaMs = if (offset.x < size.width / 2f) -10_000L else 10_000L
                        val mediaDuration = player.duration
                            .takeIf { it > 0L && it != C.TIME_UNSET } ?: 0L
                        player.seekTo(
                            NativeMediaBridge.clampSeekPosition(
                                player.currentPosition + deltaMs,
                                mediaDuration
                            )
                        )
                        seekFeedback = if (deltaMs < 0L) "−10 seconds" else "+10 seconds"
                        controlsVisible = true
                    }
                )
            },
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    keepScreenOn = true
                }
            },
            update = { view ->
                if (view.player !== player) view.player = player
                view.resizeMode = when (aspectModeIndex) {
                    1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    2 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
                view.subtitleView?.apply {
                    setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, subtitleSizeSp.toFloat())
                    val subtitleBackground = if (subtitleBackgroundEnabled) {
                        android.graphics.Color.argb(180, 0, 0, 0)
                    } else {
                        android.graphics.Color.TRANSPARENT
                    }
                    setStyle(
                        CaptionStyleCompat(
                            android.graphics.Color.WHITE,
                            subtitleBackground,
                            android.graphics.Color.TRANSPARENT,
                            CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                            android.graphics.Color.BLACK,
                            android.graphics.Typeface.DEFAULT
                        )
                    )
                }
            }
        )

        if (!isAudio) {
            Box(
                modifier = Modifier.fillMaxSize().pointerInput(player, activity, audioManager) {
                    var activeSide = 0
                    var brightnessValue = 0.5f
                    var startVolume = 0
                    var volumeAdjustment = 0f
                    var maxVolume = 1
                    var gestureMode = 0 // 1 = horizontal seek, 2 = vertical adjustment
                    var totalDragX = 0f
                    var totalDragY = 0f
                    var seekStartPosition = 0L
                    var seekDuration = 0L
                    var horizontalSeekDelta = 0L

                    detectDragGestures(
                        onDragStart = { offset ->
                            activeSide = if (offset.x < size.width / 2f) 0 else 1
                            gestureMode = 0
                            totalDragX = 0f
                            totalDragY = 0f
                            seekStartPosition = player.currentPosition
                            seekDuration = player.duration.takeIf { it > 0L && it != C.TIME_UNSET } ?: 0L
                            horizontalSeekDelta = 0L
                            if (activeSide == 0) {
                                brightnessValue = activity?.window?.attributes?.screenBrightness
                                    ?.takeIf { it >= 0f } ?: 0.5f
                            } else {
                                startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                volumeAdjustment = 0f
                            }
                            controlsVisible = false
                        },
                        onDragEnd = {
                            if (gestureMode == 1 && seekDuration > 0L) {
                                player.seekTo(
                                    NativeMediaBridge.clampSeekPosition(
                                        seekStartPosition + horizontalSeekDelta,
                                        seekDuration
                                    )
                                )
                            }
                            controlsVisible = true
                        },
                        onDragCancel = { controlsVisible = true }
                    ) { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y
                        if (gestureMode == 0 && (abs(totalDragX) > 10f || abs(totalDragY) > 10f)) {
                            gestureMode = if (abs(totalDragX) > abs(totalDragY)) 1 else 2
                        }

                        if (gestureMode == 1 && seekDuration > 0L) {
                            horizontalSeekDelta += (dragAmount.x * 250f).toLong()
                            val previewPosition = NativeMediaBridge.clampSeekPosition(
                                seekStartPosition + horizontalSeekDelta,
                                seekDuration
                            )
                            seekFeedback = "Seek to " + formatTime(previewPosition)
                        } else if (gestureMode == 2) {
                            val verticalFraction = -dragAmount.y / size.height.coerceAtLeast(1).toFloat()
                            if (activeSide == 0) {
                                val window = activity?.window
                                if (window != null) {
                                    brightnessValue = (brightnessValue + verticalFraction * 1.35f).coerceIn(0.02f, 1f)
                                    val attributes = window.attributes
                                    attributes.screenBrightness = brightnessValue
                                    window.attributes = attributes
                                    seekFeedback = "Brightness " + (brightnessValue * 100).roundToInt() + "%"
                                }
                            } else {
                                volumeAdjustment += verticalFraction * maxVolume * 2f
                                val targetVolume = (startVolume + volumeAdjustment.roundToInt()).coerceIn(0, maxVolume)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
                                seekFeedback = "Volume " + (targetVolume * 100f / maxVolume).roundToInt() + "%"
                            }
                        }
                    }
                }
            )
        }

        seekFeedback?.let { feedback ->
            Surface(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xCC171A1F),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = feedback,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp)
                )
            }
        }

        if (isAudio) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val artwork = audioArtwork
                if (artwork != null) {
                    Surface(
                        modifier = Modifier.size(248.dp),
                        shape = RoundedCornerShape(30.dp),
                        color = Color(0xFF252B34),
                        shadowElevation = 18.dp
                    ) {
                        Image(
                            bitmap = artwork,
                            contentDescription = tr("Album artwork", languageCode),
                            modifier = Modifier.fillMaxSize().padding(5.dp).clip(RoundedCornerShape(26.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier.size(232.dp),
                        color = Color(0xFF252B34),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color(0xFFC7F36B),
                                modifier = Modifier.size(88.dp)
                            )
                        }
                    }
                }
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentArtist.ifBlank { tr("Unknown artist", languageCode) },
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (currentAlbum.isNotBlank()) {
                    Text(
                        text = currentAlbum,
                        color = Color.White.copy(alpha = 0.48f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = tr("Audio player", languageCode).uppercase(),
                    color = Color.White.copy(alpha = 0.48f),
                    fontSize = 9.sp,
                    letterSpacing = 1.8.sp
                )
                Text(
                    text = tr("NOW PLAYING", languageCode),
                    color = Color.White.copy(alpha = 0.58f),
                    fontSize = 10.sp,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "${tr("TRACK", languageCode)} ${currentMediaIndex + 1} / ${queueSize.coerceAtLeast(1)}",
                    color = Color.White.copy(alpha = 0.42f),
                    fontSize = 9.sp,
                    letterSpacing = 1.4.sp
                )
            }
        }

        if (controlsVisible) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Black.copy(alpha = 0.78f),
                                0.30f to Color.Transparent,
                                0.62f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.88f)
                            )
                        )
                    )
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = leavePlayer) {
                        Icon(Icons.Default.ArrowBack, contentDescription = tr("Back", languageCode), tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "ZVX PLAYER",
                            color = Color.White.copy(alpha = 0.58f),
                            fontSize = 9.sp,
                            letterSpacing = 1.5.sp
                        )
                    }
                    if (!isAudio) {
                        TextButton(
                            onClick = {
                                val hostActivity = activity
                                if (hostActivity != null) {
                                    immersiveLandscape = !immersiveLandscape
                                    if (immersiveLandscape) {
                                        hostActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                        WindowInsetsControllerCompat(hostActivity.window, hostActivity.window.decorView).apply {
                                            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                                            hide(WindowInsetsCompat.Type.systemBars())
                                        }
                                        controlsVisible = false
                                    } else {
                                        hostActivity.requestedOrientation = originalRequestedOrientation
                                        WindowInsetsControllerCompat(hostActivity.window, hostActivity.window.decorView).show(WindowInsetsCompat.Type.systemBars())
                                        controlsVisible = true
                                    }
                                }
                            }
                        ) {
                            Text(tr(if (immersiveLandscape) "Exit" else "Full", languageCode), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            TextButton(
                                onClick = {
                                    val hostActivity = activity
                                    if (hostActivity != null) {
                                        controlsVisible = false
                                        hostActivity.enterPictureInPictureMode(
                                            PictureInPictureParams.Builder()
                                                .setAspectRatio(Rational(16, 9))
                                                .build()
                                        )
                                    }
                                }
                            ) {
                                Text(tr("PiP", languageCode), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (queueSize > 1) {
                        TextButton(onClick = { queueDialogVisible = true }) {
                            Text(tr("Queue", languageCode), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(onClick = { trackDialogVisible = true }) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = tr("Audio and subtitle tracks", languageCode),
                            tint = Color.White
                        )
                    }
                    TextButton(
                        onClick = {
                            speedIndex = (speedIndex + 1) % speeds.size
                            player.playbackParameters = PlaybackParameters(speeds[speedIndex])
                        }
                    ) {
                        Text(speeds[speedIndex].toString() + "×", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { optionsDialogVisible = true }) {
                        Text(tr("Options", languageCode), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (queueSize > 1) {
                        IconButton(
                            onClick = { player.seekToPreviousMediaItem() },
                            enabled = player.hasPreviousMediaItem(),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = tr("Previous item", languageCode), tint = Color.White)
                        }
                    }
                    IconButton(
                        onClick = {
                            player.seekTo(
                                NativeMediaBridge.clampSeekPosition(
                                    player.currentPosition - 10_000L,
                                    player.duration.takeIf { it > 0L && it != C.TIME_UNSET } ?: 0L
                                )
                            )
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Replay10,
                            contentDescription = tr("Back 10 seconds", languageCode),
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.size(28.dp))
                    Surface(
                        modifier = Modifier.size(68.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White.copy(alpha = 0.16f)
                    ) {
                        IconButton(
                            onClick = {
                                if (player.isPlaying) {
                                    player.pause()
                                    controlsVisible = true
                                } else {
                                    player.play()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = tr(if (isPlaying) "Pause" else "Play", languageCode),
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                    Spacer(Modifier.size(28.dp))
                    IconButton(
                        onClick = {
                            player.seekTo(
                                NativeMediaBridge.clampSeekPosition(
                                    player.currentPosition + 10_000L,
                                    player.duration.takeIf { it > 0L && it != C.TIME_UNSET } ?: 0L
                                )
                            )
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Forward10,
                            contentDescription = tr("Forward 10 seconds", languageCode),
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    if (queueSize > 1) {
                        IconButton(
                            onClick = { player.seekToNextMediaItem() },
                            enabled = player.hasNextMediaItem(),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = tr("Next item", languageCode), tint = Color.White)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(formatTime(scrubPosition ?: currentPosition), color = Color.White, fontSize = 11.sp)
                    Slider(
                        value = if (duration > 0L) {
                            ((scrubPosition ?: currentPosition).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0f
                        },
                        onValueChange = { fraction ->
                            scrubPosition = (fraction * duration).toLong()
                        },
                        onValueChangeFinished = {
                            scrubPosition?.let { requestedPosition ->
                                player.seekTo(
                                    NativeMediaBridge.clampSeekPosition(
                                        requestedPosition,
                                        duration
                                    )
                                )
                            }
                            scrubPosition = null
                        },
                        enabled = duration > 0L,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFC7F36B),
                            activeTrackColor = Color(0xFFC7F36B),
                            inactiveTrackColor = Color.White.copy(alpha = 0.28f)
                        )
                    )
                    Text(formatTime(duration), color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
                }
            }
        }

        if (playbackError != null) {
            Surface(
                modifier = Modifier.align(Alignment.Center).padding(28.dp),
                color = Color(0xE6202328),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(tr("Playback information", languageCode), color = Color.White, fontWeight = FontWeight.Bold)
                    Text(playbackError.orEmpty(), color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp)
                    Text(
                        tr("Check the media source and supported formats.", languageCode),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }

    if (optionsDialogVisible) {
        AlertDialog(
            onDismissRequest = { optionsDialogVisible = false },
            title = { Text(tr("Playback options", languageCode), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!isAudio) {
                        Text(tr("VIDEO SIZE", languageCode), color = Color(0xFF7F8A78), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            aspectModeLabels.forEachIndexed { index, label ->
                                TextButton(onClick = { aspectModeIndex = index }) {
                                    Text(
                                        label,
                                        color = if (aspectModeIndex == index) Color(0xFFC7F36B) else Color.White
                                    )
                                }
                            }
                        }
                    }
                    if (isAudio) {
                        Text(tr("AUDIO TONE", languageCode), color = Color(0xFF7F8A78), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(
                                NativeAudioProcessor.PROFILE_FLAT to "Flat",
                                NativeAudioProcessor.PROFILE_WARM to "Warm",
                                NativeAudioProcessor.PROFILE_VOCAL to "Vocal",
                                NativeAudioProcessor.PROFILE_BRIGHT to "Bright"
                            ).forEach { (profile, label) ->
                                TextButton(onClick = {
                                    NativeAudioProcessor.setProfile(context, profile)
                                    audioToneProfile = profile
                                }) {
                                    Text(
                                        tr(label, languageCode),
                                        color = if (audioToneProfile == profile) Color(0xFFC7F36B) else Color.White,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        Text(
                            tr("Tone presets work with decoded 16-bit PCM audio. Encoded passthrough is unchanged.", languageCode),
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 10.sp
                        )
                    }
                    Text(tr("SLEEP TIMER", languageCode), color = Color(0xFF7F8A78), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (sleepTimerMinutes > 0) "Playback will pause in $sleepTimerMinutes minutes."
                        else tr("Pause playback automatically after a set time.", languageCode),
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 12.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(15, 30, 45, 60).forEach { minutes ->
                            TextButton(
                                onClick = {
                                    sleepTimerMinutes = minutes
                                    optionsDialogVisible = false
                                }
                            ) {
                                Text("${minutes}m")
                            }
                        }
                    }
                    if (sleepTimerMinutes > 0) {
                        TextButton(
                            onClick = { sleepTimerMinutes = 0 },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(tr("Cancel sleep timer", languageCode))
                        }
                    }
                    Text(tr("SUBTITLE STYLE", languageCode), color = Color(0xFF7F8A78), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("${tr("Text size", languageCode)} · $subtitleSizeSp sp", color = Color.White.copy(alpha = 0.76f), fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(onClick = { subtitleSizeSp = (subtitleSizeSp - 2).coerceAtLeast(14) }) {
                            Text("A−")
                        }
                        TextButton(onClick = { subtitleSizeSp = 18 }) {
                            Text(tr("Reset", languageCode))
                        }
                        TextButton(onClick = { subtitleSizeSp = (subtitleSizeSp + 2).coerceAtMost(32) }) {
                            Text("A+")
                        }
                    }
                    Text(tr("SUBTITLE TIMING", languageCode), color = Color(0xFF7F8A78), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${tr("External SRT / WebVTT delay", languageCode)} · ${subtitleDelayMs} ms",
                        color = Color.White.copy(alpha = 0.76f),
                        fontSize = 12.sp
                    )
                    Slider(
                        value = subtitleDelayMs.toFloat(),
                        onValueChange = { value ->
                            subtitleDelayMs = ((value / 250f).roundToInt() * 250).coerceIn(-10_000, 10_000)
                        },
                        valueRange = -10_000f..10_000f,
                        steps = 79,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFC7F36B),
                            activeTrackColor = Color(0xFFC7F36B),
                            inactiveTrackColor = Color.White.copy(alpha = 0.28f)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(onClick = { subtitleDelayMs = (subtitleDelayMs - 250).coerceAtLeast(-10_000) }) {
                            Text(tr("−250 ms", languageCode))
                        }
                        TextButton(onClick = { subtitleDelayMs = 0 }) {
                            Text(tr("Reset", languageCode))
                        }
                        TextButton(onClick = { subtitleDelayMs = (subtitleDelayMs + 250).coerceAtMost(10_000) }) {
                            Text(tr("+250 ms", languageCode))
                        }
                    }
                    Text(
                        tr("Positive values delay subtitles; negative values show them earlier. Embedded subtitles follow the source timing.", languageCode),
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 10.sp
                    )
                    TextButton(
                        onClick = { subtitleBackgroundEnabled = !subtitleBackgroundEnabled },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(tr(if (subtitleBackgroundEnabled) "Subtitle background: on" else "Subtitle background: off", languageCode))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { optionsDialogVisible = false }) { Text(tr("Done", languageCode)) }
            }
        )
    }

    if (queueDialogVisible) {
        AlertDialog(
            onDismissRequest = { queueDialogVisible = false },
            title = { Text(tr("Playback queue", languageCode) + " · ${editableQueue.size}", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    editableQueue.forEachIndexed { index, entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    player.seekTo(index, 0L)
                                    player.play()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        entry.title,
                                        color = if (index == currentMediaIndex) Color(0xFFC7F36B) else Color.White,
                                        maxLines = 1
                                    )
                                    Text(
                                        entry.artist ?: entry.folderName,
                                        color = Color.White.copy(alpha = 0.56f),
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                                if (index == currentMediaIndex) {
                                    Text(tr("NOW", languageCode), color = Color(0xFFC7F36B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Column {
                                IconButton(
                                    enabled = index > 0,
                                    onClick = {
                                        player.moveMediaItem(index, index - 1)
                                        editableQueue = editableQueue.toMutableList().apply {
                                            add(index - 1, removeAt(index))
                                        }
                                        onQueueChanged(editableQueue, player.currentMediaItem?.localConfiguration?.uri)
                                    }
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = tr("Move up", languageCode), tint = Color.White)
                                }
                                IconButton(
                                    enabled = index < editableQueue.lastIndex,
                                    onClick = {
                                        player.moveMediaItem(index, index + 1)
                                        editableQueue = editableQueue.toMutableList().apply {
                                            add(index + 1, removeAt(index))
                                        }
                                        onQueueChanged(editableQueue, player.currentMediaItem?.localConfiguration?.uri)
                                    }
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = tr("Move down", languageCode), tint = Color.White)
                                }
                            }
                            IconButton(
                                onClick = {
                                    player.removeMediaItem(index)
                                    editableQueue = editableQueue.toMutableList().apply { removeAt(index) }
                                    onQueueChanged(editableQueue, player.currentMediaItem?.localConfiguration?.uri)
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = tr("Remove from queue", languageCode), tint = Color(0xFFE58C83))
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(
                            onClick = {
                                shuffleEnabled = !shuffleEnabled
                                player.shuffleModeEnabled = shuffleEnabled
                            }
                        ) {
                            Text(tr(if (shuffleEnabled) "Shuffle: on" else "Shuffle: off", languageCode))
                        }
                        TextButton(
                            onClick = {
                                repeatMode = when (repeatMode) {
                                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                    else -> Player.REPEAT_MODE_OFF
                                }
                                player.repeatMode = repeatMode
                            }
                        ) {
                            Text(
                                when (repeatMode) {
                                    Player.REPEAT_MODE_ALL -> tr("Repeat: all", languageCode)
                                    Player.REPEAT_MODE_ONE -> tr("Repeat: one", languageCode)
                                    else -> tr("Repeat: off", languageCode)
                                }
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            val playingItem = player.currentMediaItem
                            val playingEntry = editableQueue.getOrNull(player.currentMediaItemIndex)
                            if (playingItem != null) {
                                val oldIndex = player.currentMediaItemIndex
                                if (oldIndex + 1 < player.mediaItemCount) {
                                    player.removeMediaItems(oldIndex + 1, player.mediaItemCount)
                                }
                                if (oldIndex > 0) {
                                    player.removeMediaItems(0, oldIndex)
                                }
                                editableQueue = listOfNotNull(playingEntry)
                                onQueueChanged(editableQueue, playingItem.localConfiguration?.uri)
                                queueDialogVisible = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(tr("Clear queue · keep current", languageCode))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { queueDialogVisible = false }) { Text(tr("Close", languageCode)) }
            }
        )
    }

    if (trackDialogVisible) {
        TrackSelectionDialog(
            tracks = availableTracks,
            onDismiss = { trackDialogVisible = false },
            languageCode = languageCode,
            onSelectAudio = { group, trackIndex ->
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                    .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                    .build()
                trackDialogVisible = false
            },
            onSelectSubtitle = { group, trackIndex ->
                externalSubtitleUri = null
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                    .build()
                trackDialogVisible = false
            },
            onSubtitleOff = {
                externalSubtitleUri = null
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .build()
                trackDialogVisible = false
            },
            onLoadExternalSubtitle = {
                trackDialogVisible = false
                subtitlePicker.launch(arrayOf("text/*", "application/x-subrip", "application/ttml+xml"))
            }
        )
    }
}

@Composable
private fun TrackSelectionDialog(
    tracks: Tracks,
    languageCode: String,
    onDismiss: () -> Unit,
    onSelectAudio: (Tracks.Group, Int) -> Unit,
    onSelectSubtitle: (Tracks.Group, Int) -> Unit,
    onSubtitleOff: () -> Unit,
    onLoadExternalSubtitle: () -> Unit
) {
    val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
    val subtitleGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Audio & subtitles", languageCode), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(tr("AUDIO", languageCode), color = Color(0xFF7F8A78), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                if (audioGroups.isEmpty()) {
                    Text(tr("No audio tracks reported by this source.", languageCode), color = Color.LightGray, fontSize = 13.sp)
                } else {
                    audioGroups.forEach { group ->
                        for (index in 0 until group.length) {
                            val format = group.getTrackFormat(index)
                            val label = trackLabel(format.label, format.language, "Audio", index, languageCode)
                            TrackOption(
                                label = label,
                                languageCode = languageCode,
                                selected = group.isTrackSelected(index),
                                enabled = group.isTrackSupported(index),
                                onClick = { onSelectAudio(group, index) }
                            )
                        }
                    }
                }

                Spacer(Modifier.size(4.dp))
                Text(tr("SUBTITLES", languageCode), color = Color(0xFF7F8A78), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                TrackOption(
                    label = tr("Off", languageCode),
                    languageCode = languageCode,
                    selected = !tracks.groups.any { it.type == C.TRACK_TYPE_TEXT && it.isSelected },
                    onClick = onSubtitleOff
                )
                if (subtitleGroups.isEmpty()) {
                    Text(tr("No embedded subtitles reported.", languageCode), color = Color.LightGray, fontSize = 13.sp)
                } else {
                    subtitleGroups.forEach { group ->
                        for (index in 0 until group.length) {
                            val format = group.getTrackFormat(index)
                            val label = trackLabel(format.label, format.language, "Subtitle", index, languageCode)
                            TrackOption(
                                label = label,
                                languageCode = languageCode,
                                selected = group.isTrackSelected(index),
                                enabled = group.isTrackSupported(index),
                                onClick = { onSelectSubtitle(group, index) }
                            )
                        }
                    }
                }

                TextButton(onClick = onLoadExternalSubtitle) {
                    Icon(Icons.Default.ClosedCaption, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(tr("Load external subtitle", languageCode))
                }
                Text(
                    tr("Supported external formats: SRT, VTT, TTML, SSA and ASS.", languageCode),
                    color = Color.LightGray.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(tr("Done", languageCode)) }
        }
    )
}

@Composable
private fun TrackOption(
    label: String,
    languageCode: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFFC7F36B) else Color.White,
            modifier = Modifier.weight(1f),
            maxLines = 2
        )
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = tr("Selected", languageCode), tint = Color(0xFFC7F36B))
        }
    }
}

private fun trackLabel(
    label: String?,
    language: String?,
    kind: String,
    index: Int,
    languageCode: String
): String {
    val primary = label?.takeIf { it.isNotBlank() }
        ?: language?.takeIf { it.isNotBlank() && it != "und" }?.uppercase()
        ?: (tr(kind, languageCode) + " " + (index + 1))
    return primary
}

private fun subtitleMimeType(contentMimeType: String?, path: String?): String? {
    val extension = path?.substringAfterLast('.', "")?.lowercase()
    return when (contentMimeType?.lowercase()) {
        MimeTypes.APPLICATION_SUBRIP -> MimeTypes.APPLICATION_SUBRIP
        MimeTypes.TEXT_VTT -> MimeTypes.TEXT_VTT
        MimeTypes.APPLICATION_TTML -> MimeTypes.APPLICATION_TTML
        MimeTypes.TEXT_SSA -> MimeTypes.TEXT_SSA
        else -> when (extension) {
            "srt" -> MimeTypes.APPLICATION_SUBRIP
            "vtt" -> MimeTypes.TEXT_VTT
            "ttml", "xml", "dfxp" -> MimeTypes.APPLICATION_TTML
            "ssa", "ass" -> MimeTypes.TEXT_SSA
            else -> null
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0L) / 1_000L
    val seconds = totalSeconds % 60L
    val minutes = (totalSeconds / 60L) % 60L
    val hours = totalSeconds / 3_600L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(totalSeconds / 60L, seconds)
    }
}


private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
