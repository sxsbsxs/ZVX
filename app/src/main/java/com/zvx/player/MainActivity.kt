package com.zvx.player

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.zvx.player.data.MediaEntry
import com.zvx.player.data.PlaybackQueueStore
import com.zvx.player.data.SavedPlaybackQueue
import com.zvx.player.ui.HomeScreen
import com.zvx.player.ui.PlayerScreen
import com.zvx.player.ui.theme.ZvxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZvxTheme {
                var activeSource by rememberSaveable { mutableStateOf<String?>(null) }
                var activeIsAudio by rememberSaveable { mutableStateOf(false) }
                var activeQueue by androidx.compose.runtime.remember { mutableStateOf<List<MediaEntry>>(emptyList()) }
                var savedQueue by androidx.compose.runtime.remember { mutableStateOf(PlaybackQueueStore.load(this@MainActivity)) }

                val filePicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { selectedUri ->
                    if (selectedUri != null) {
                        try {
                            contentResolver.takePersistableUriPermission(
                                selectedUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (_: SecurityException) {
                            // Some document providers grant temporary access only.
                        }
                        activeSource = selectedUri.toString()
                        activeQueue = emptyList()
                        savedQueue = SavedPlaybackQueue()
                        PlaybackQueueStore.save(this@MainActivity, emptyList(), null)
                        activeIsAudio = selectedUri.toString().substringBefore('?')
                            .substringAfterLast('.', "")
                            .lowercase() in setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "opus")
                    }
                }

                if (activeSource == null) {
                    HomeScreen(
                        modifier = Modifier.fillMaxSize(),
                        onChooseFile = {
                            filePicker.launch(arrayOf("video/*", "audio/*", "application/octet-stream"))
                        },
                        savedQueue = savedQueue,
                        onResumeQueue = { snapshot ->
                            val targetUri = snapshot.currentUri ?: snapshot.entries.firstOrNull()?.uri
                            if (targetUri != null && snapshot.entries.isNotEmpty()) {
                                activeSource = targetUri.toString()
                                activeIsAudio = snapshot.entries.firstOrNull { it.uri == targetUri }?.kind == com.zvx.player.data.MediaKind.AUDIO
                                activeQueue = snapshot.entries
                            }
                        },
                        onPlayMedia = { uri, isAudio, queue ->
                            activeSource = uri
                            activeIsAudio = isAudio
                            activeQueue = queue
                            savedQueue = SavedPlaybackQueue(queue, Uri.parse(uri).takeIf { candidate -> queue.any { it.uri == candidate } })
                            PlaybackQueueStore.save(this@MainActivity, queue, savedQueue.currentUri)
                        },
                        onOpenStream = { address ->
                            activeSource = address
                            activeIsAudio = false
                            activeQueue = emptyList()
                            savedQueue = SavedPlaybackQueue()
                            PlaybackQueueStore.save(this@MainActivity, emptyList(), null)
                        }
                    )
                } else {
                    PlayerScreen(
                        videoUri = Uri.parse(activeSource),
                        isAudio = activeIsAudio,
                        queue = activeQueue,
                        modifier = Modifier.fillMaxSize(),
                        onQueueChanged = { updatedQueue, currentUri ->
                            activeQueue = updatedQueue
                            savedQueue = SavedPlaybackQueue(updatedQueue, currentUri)
                            PlaybackQueueStore.save(this@MainActivity, updatedQueue, currentUri)
                        },
                        onBack = {
                            activeSource = null
                            activeIsAudio = false
                            // Retain the queue for one-tap restoration from the library screen.
                        }
                    )
                }
            }
        }
    }
}
