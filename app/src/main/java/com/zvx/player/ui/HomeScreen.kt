package com.zvx.player.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zvx.player.ui.theme.ZvxAccent
import com.zvx.player.ui.theme.ZvxBackground
import com.zvx.player.ui.theme.ZvxMuted
import com.zvx.player.ui.theme.ZvxSurface
import com.zvx.player.ui.theme.ZvxSurfaceRaised
import com.zvx.player.ui.theme.ZvxText

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onChooseVideo: () -> Unit,
    onOpenStream: (String) -> Unit
) {
    var showStreamDialog by remember { mutableStateOf(false) }

    Surface(modifier = modifier, color = ZvxBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BrandMark()
                Spacer(Modifier.size(12.dp))
                Text(
                    text = "ZVX",
                    color = ZvxText,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "VIDEO PLAYER",
                    color = ZvxMuted,
                    fontSize = 10.sp,
                    letterSpacing = 1.6.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "JUST PRESS PLAY",
                    color = ZvxAccent,
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 2.2.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Your screen.\nYour rules.",
                    color = ZvxText,
                    fontSize = 42.sp,
                    lineHeight = 47.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.5).sp
                )
                Text(
                    text = "A simple space for everything you want to watch.",
                    color = ZvxMuted,
                    fontSize = 15.sp,
                    lineHeight = 23.sp
                )
            }

            HeroPanel()

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onChooseVideo,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZvxAccent,
                        contentColor = ZvxBackground
                    )
                ) {
                    Icon(Icons.Default.VideoLibrary, contentDescription = null)
                    Spacer(Modifier.size(10.dp))
                    Text("Open a video", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                OutlinedButton(
                    onClick = { showStreamDialog = true },
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ZvxText),
                    border = BorderStroke(1.dp, ZvxSurfaceRaised)
                ) {
                    Icon(Icons.Default.Link, contentDescription = null)
                    Spacer(Modifier.size(10.dp))
                    Text("Play from a link", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "READY FOR YOUR MEDIA",
                    color = ZvxMuted,
                    fontSize = 10.sp,
                    letterSpacing = 1.8.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormatTag("MP4")
                    FormatTag("WEBM")
                    FormatTag("MKV")
                    FormatTag("HLS")
                    FormatTag("DASH")
                }
                Text(
                    text = "Actual format and codec support depends on your device.",
                    color = ZvxMuted.copy(alpha = 0.72f),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showStreamDialog) {
        StreamDialog(
            onDismiss = { showStreamDialog = false },
            onOpen = { address ->
                onOpenStream(address)
                showStreamDialog = false
            }
        )
    }
}

@Composable
private fun BrandMark() {
    Box(
        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(ZvxAccent),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ZvxBackground, modifier = Modifier.size(27.dp))
    }
}

@Composable
private fun HeroPanel() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = ZvxSurface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF262E20), ZvxSurface, Color(0xFF191C22))))
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(ZvxAccent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ZvxAccent, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    Text("01 / PLAYER", color = ZvxMuted, fontSize = 10.sp, letterSpacing = 1.4.sp)
                }
                Text(
                    text = "Nothing in the way\nof the picture.",
                    color = ZvxText,
                    fontSize = 22.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Local files and streaming links. One clean player.",
                    color = ZvxMuted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
private fun FormatTag(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ZvxSurface)
            .border(1.dp, ZvxSurfaceRaised, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(label, color = ZvxText, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun StreamDialog(
    onDismiss: () -> Unit,
    onOpen: (String) -> Unit
) {
    var address by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Play from a link", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Paste a direct video, HLS or DASH URL.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Video URL") },
                    placeholder = { Text("https://example.com/video.m3u8") },
                    isError = error != null
                )
                if (error != null) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val candidate = address.trim()
                    val parsed = runCatching { Uri.parse(candidate) }.getOrNull()
                    val valid = parsed != null &&
                        parsed.scheme?.lowercase() in setOf("http", "https") &&
                        !parsed.host.isNullOrBlank()
                    if (valid) onOpen(candidate) else error = "Enter a valid HTTP or HTTPS address."
                }
            ) {
                Text("Play", color = ZvxAccent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = ZvxSurface,
        titleContentColor = ZvxText,
        textContentColor = ZvxMuted
    )
}
