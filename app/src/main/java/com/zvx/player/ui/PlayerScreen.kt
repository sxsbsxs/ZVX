package com.zvx.player.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.zvx.player.data.PlaybackProgressStore
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    videoUri: Uri,
    modifier: Modifier = Modifier,
    isAudio: Boolean = false,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val savedPosition = remember(videoUri) {
        PlaybackProgressStore.getPosition(context, videoUri.toString())
    }
    var resumeApplied by remember(videoUri) { mutableStateOf(false) }
    val player = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            playWhenReady = true
            prepare()
        }
    }

    var controlsVisible by remember(videoUri) { mutableStateOf(true) }
    var isPlaying by remember(videoUri) { mutableStateOf(true) }
    var currentPosition by remember(videoUri) { mutableLongStateOf(0L) }
    var duration by remember(videoUri) { mutableLongStateOf(0L) }
    var scrubPosition by remember(videoUri) { mutableStateOf<Long?>(null) }
    var playbackError by remember(videoUri) { mutableStateOf<String?>(null) }
    var speedIndex by remember(videoUri) { mutableStateOf(1) }
    var trackDialogVisible by remember(videoUri) { mutableStateOf(false) }
    var availableTracks by remember(player) { mutableStateOf(player.currentTracks) }
    val speeds = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
    val title = videoUri.lastPathSegment?.substringAfterLast('/')?.take(64) ?: "Now playing"

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
                if (currentItem == null) {
                    playbackError = "There is no active video to attach a subtitle to."
                } else {
                    val subtitle = MediaItem.SubtitleConfiguration.Builder(selectedUri)
                        .setMimeType(mimeType)
                        .setLanguage("und")
                        .setLabel(selectedUri.lastPathSegment?.substringAfterLast('/') ?: "External subtitle")
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                    val existing = currentItem.localConfiguration?.subtitleConfigurations.orEmpty()
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
            }
        }
    }

    BackHandler { onBack() }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && !resumeApplied) {
                    resumeApplied = true
                    val totalDuration = player.duration
                    val canResume = savedPosition >= 5_000L &&
                        (totalDuration <= 0L || savedPosition < totalDuration - 10_000L)
                    if (canResume) {
                        player.seekTo(savedPosition)
                        currentPosition = savedPosition
                    }
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                availableTracks = tracks
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = error.message ?: error.errorCodeName
                controlsVisible = true
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            PlaybackProgressStore.save(
                context,
                videoUri.toString(),
                player.currentPosition,
                player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
            )
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            currentPosition = player.currentPosition.coerceAtLeast(0L)
            val playerDuration = player.duration
            duration = if (playerDuration == C.TIME_UNSET) 0L else playerDuration.coerceAtLeast(0L)
            isPlaying = player.isPlaying
            PlaybackProgressStore.save(
                context,
                videoUri.toString(),
                player.currentPosition,
                player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
            )
            delay(5_000L)
        }
    }

    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying && !trackDialogVisible) {
            delay(3_500L)
            controlsVisible = false
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize().pointerInput(player) {
                detectTapGestures(onTap = { controlsVisible = !controlsVisible })
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
            }
        )

        if (isAudio) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFFC7F36B),
                    modifier = Modifier.size(72.dp)
                )
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "NOW PLAYING",
                    color = Color.White.copy(alpha = 0.58f),
                    fontSize = 10.sp,
                    letterSpacing = 2.sp
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
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
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
                    IconButton(onClick = { trackDialogVisible = true }) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Audio and subtitle tracks",
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
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            player.seekTo((player.currentPosition - 10_000L).coerceAtLeast(0L))
                        },
                        modifier = Modifier.size(54.dp)
                    ) {
                        Icon(
                            Icons.Default.Replay10,
                            contentDescription = "Back 10 seconds",
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
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                    Spacer(Modifier.size(28.dp))
                    IconButton(
                        onClick = {
                            val end = if (duration > 0L) duration else Long.MAX_VALUE
                            player.seekTo((player.currentPosition + 10_000L).coerceAtMost(end))
                        },
                        modifier = Modifier.size(54.dp)
                    ) {
                        Icon(
                            Icons.Default.Forward10,
                            contentDescription = "Forward 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
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
                            scrubPosition?.let(player::seekTo)
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
                    Text("Playback information", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(playbackError.orEmpty(), color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp)
                    Text(
                        "Check the media source and supported formats.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }

    if (trackDialogVisible) {
        TrackSelectionDialog(
            tracks = availableTracks,
            onDismiss = { trackDialogVisible = false },
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
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                    .build()
                trackDialogVisible = false
            },
            onSubtitleOff = {
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
        title = { Text("Audio & subtitles", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("AUDIO", color = Color(0xFF7F8A78), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                if (audioGroups.isEmpty()) {
                    Text("No audio tracks reported by this source.", color = Color.LightGray, fontSize = 13.sp)
                } else {
                    audioGroups.forEach { group ->
                        for (index in 0 until group.length) {
                            val format = group.getTrackFormat(index)
                            val label = trackLabel(format.label, format.language, "Audio", index)
                            TrackOption(
                                label = label,
                                selected = group.isTrackSelected(index),
                                enabled = group.isTrackSupported(index),
                                onClick = { onSelectAudio(group, index) }
                            )
                        }
                    }
                }

                Spacer(Modifier.size(4.dp))
                Text("SUBTITLES", color = Color(0xFF7F8A78), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                TrackOption(
                    label = "Off",
                    selected = !tracks.groups.any { it.type == C.TRACK_TYPE_TEXT && it.isSelected },
                    onClick = onSubtitleOff
                )
                if (subtitleGroups.isEmpty()) {
                    Text("No embedded subtitles reported.", color = Color.LightGray, fontSize = 13.sp)
                } else {
                    subtitleGroups.forEach { group ->
                        for (index in 0 until group.length) {
                            val format = group.getTrackFormat(index)
                            val label = trackLabel(format.label, format.language, "Subtitle", index)
                            TrackOption(
                                label = label,
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
                    Text("Load external subtitle")
                }
                Text(
                    "Supported external formats: SRT, VTT, TTML, SSA and ASS.",
                    color = Color.LightGray.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun TrackOption(
    label: String,
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
            Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color(0xFFC7F36B))
        }
    }
}

private fun trackLabel(
    label: String?,
    language: String?,
    kind: String,
    index: Int
): String {
    val primary = label?.takeIf { it.isNotBlank() }
        ?: language?.takeIf { it.isNotBlank() && it != "und" }?.uppercase()
        ?: (kind + " " + (index + 1))
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
