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
import com.zvx.player.ui.HomeScreen
import com.zvx.player.ui.PlayerScreen
import com.zvx.player.ui.theme.ZvxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZvxTheme {
                var activeSource by rememberSaveable { mutableStateOf<String?>(null) }

                val videoPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { selectedUri ->
                    if (selectedUri != null) {
                        try {
                            contentResolver.takePersistableUriPermission(
                                selectedUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (_: SecurityException) {
                            // Some document providers grant a temporary URI only.
                        }
                        activeSource = selectedUri.toString()
                    }
                }

                if (activeSource == null) {
                    HomeScreen(
                        modifier = Modifier.fillMaxSize(),
                        onChooseVideo = {
                            videoPicker.launch(arrayOf("video/*", "application/octet-stream"))
                        },
                        onOpenStream = { address -> activeSource = address }
                    )
                } else {
                    PlayerScreen(
                        videoUri = Uri.parse(activeSource),
                        modifier = Modifier.fillMaxSize(),
                        onBack = { activeSource = null }
                    )
                }
            }
        }
    }
}
