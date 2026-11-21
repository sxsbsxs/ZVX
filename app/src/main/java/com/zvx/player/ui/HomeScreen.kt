package com.zvx.player.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.os.Build
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zvx.player.data.MediaEntry
import com.zvx.player.data.MediaFavoritesStore
import com.zvx.player.data.MediaKind
import com.zvx.player.data.MediaPermission
import com.zvx.player.data.MediaStoreRepository
import com.zvx.player.data.PlaybackHistoryStore
import com.zvx.player.data.LibraryPreferencesStore
import com.zvx.player.ui.theme.ZvxAccent
import com.zvx.player.ui.theme.ZvxBackground
import com.zvx.player.ui.theme.ZvxMuted
import com.zvx.player.ui.theme.ZvxSurface
import com.zvx.player.ui.theme.ZvxSurfaceRaised
import com.zvx.player.ui.theme.ZvxText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI

private enum class LibraryFilter(val label: String) {
    ALL("All"), VIDEO("Videos"), AUDIO("Audio"), RECENT("Recent"), FAVORITES("Favorites")
}

private enum class LibrarySort(val label: String) {
    NEWEST("Newest first"), NAME("Name"), LONGEST("Longest"), LARGEST("Largest")
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onChooseFile: () -> Unit,
    onPlayMedia: (String, Boolean, List<MediaEntry>) -> Unit,
    onOpenStream: (String) -> Unit
) {
    val context = LocalContext.current
    var access by remember { mutableStateOf(MediaPermission.current(context)) }
    var media by remember { mutableStateOf<List<MediaEntry>>(emptyList()) }
    var recentUris by remember { mutableStateOf(PlaybackHistoryStore.getRecent(context)) }
    var favoriteUris by remember { mutableStateOf(MediaFavoritesStore.getFavorites(context)) }
    var searchText by remember { mutableStateOf("") }
    val savedLibraryPreferences = remember(context) { LibraryPreferencesStore.load(context) }
    var selectedFilter by remember {
        mutableStateOf(LibraryFilter.entries.firstOrNull { it.name == savedLibraryPreferences.filterName } ?: LibraryFilter.ALL)
    }
    var selectedFolder by remember { mutableStateOf("All folders") }
    var selectedSort by remember {
        mutableStateOf(LibrarySort.entries.firstOrNull { it.name == savedLibraryPreferences.sortName } ?: LibrarySort.NEWEST)
    }
    var showSortDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showHistoryScreen by remember { mutableStateOf(false) }
    var showFavoritesScreen by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearFavoritesDialog by remember { mutableStateOf(false) }
    var showStreamDialog by remember { mutableStateOf(false) }
    var importPlaylistUri by remember { mutableStateOf<Uri?>(null) }
    var playlistImportError by remember { mutableStateOf<String?>(null) }
    var reloadToken by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        access = MediaPermission.current(context)
        reloadToken++
    }

    val playlistLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { selectedUri ->
        if (selectedUri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    selectedUri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // The provider may grant temporary read access only.
            }
            playlistImportError = null
            importPlaylistUri = selectedUri
        }
    }

    LaunchedEffect(selectedFilter, selectedSort) {
        LibraryPreferencesStore.save(context, selectedFilter.name, selectedSort.name)
    }

    LaunchedEffect(importPlaylistUri) {
        val selectedUri = importPlaylistUri ?: return@LaunchedEffect
        val content = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(selectedUri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
            }.getOrNull()
        }
        if (content.isNullOrBlank()) {
            playlistImportError = "Unable to read this playlist file."
        } else {
            val imported = parseM3uPlaylist(content, selectedUri)
            if (imported.isEmpty()) {
                playlistImportError = "No supported media entries were found. Relative paths are resolved against the playlist when its storage provider allows access to sibling files."
            } else {
                val selectedKind = imported.first().kind
                val playableQueue = imported.filter { it.kind == selectedKind }
                onPlayMedia(
                    playableQueue.first().uri.toString(),
                    selectedKind == MediaKind.AUDIO,
                    playableQueue
                )
            }
        }
        importPlaylistUri = null
    }

    LaunchedEffect(access, reloadToken) {
        if (!access.hasAny) {
            media = emptyList()
            isLoading = false
        } else {
            isLoading = true
            media = withContext(Dispatchers.IO) {
                MediaStoreRepository.load(context.applicationContext, access)
            }
            isLoading = false
            recentUris = PlaybackHistoryStore.getRecent(context)
            favoriteUris = MediaFavoritesStore.getFavorites(context)
        }
    }

    BackHandler(enabled = showHistoryScreen || showFavoritesScreen) {
        showHistoryScreen = false
        showFavoritesScreen = false
    }

    val mediaByUri = media.associateBy { it.uri.toString() }
    val recentEntries = recentUris.mapIndexedNotNull { index, uriString ->
        mediaByUri[uriString] ?: savedUriEntry(uriString, index, "History")
    }
    val favoriteEntries = favoriteUris.mapIndexedNotNull { index, uriString ->
        mediaByUri[uriString] ?: savedUriEntry(uriString, index, "Favorites")
    }
    val folders = media.map { it.folderName }.filter { it.isNotBlank() }
        .distinct().sortedBy { it.lowercase() }
    val query = searchText.trim().lowercase()
    val visibleMedia = media.filter { entry ->
        val matchesFilter = when (selectedFilter) {
            LibraryFilter.ALL -> true
            LibraryFilter.VIDEO -> entry.kind == MediaKind.VIDEO
            LibraryFilter.AUDIO -> entry.kind == MediaKind.AUDIO
            LibraryFilter.RECENT -> entry.uri.toString() in recentUris
            LibraryFilter.FAVORITES -> entry.uri.toString() in favoriteUris
        }
        val matchesSearch = query.isEmpty() ||
            entry.title.lowercase().contains(query) ||
            entry.artist.orEmpty().lowercase().contains(query) ||
            entry.album.orEmpty().lowercase().contains(query) ||
            entry.displayName.lowercase().contains(query)
        val matchesFolder = selectedFolder == "All folders" || entry.folderName == selectedFolder
        matchesFilter && matchesSearch && matchesFolder
    }.let { filtered ->
        if (selectedFilter == LibraryFilter.RECENT) {
            val order = recentUris.withIndex().associate { it.value to it.index }
            filtered.sortedBy { order[it.uri.toString()] ?: Int.MAX_VALUE }
        } else {
            when (selectedSort) {
                LibrarySort.NEWEST -> filtered.sortedByDescending { it.dateAddedSeconds }
                LibrarySort.NAME -> filtered.sortedBy { it.title.lowercase() }
                LibrarySort.LONGEST -> filtered.sortedByDescending { it.durationMs }
                LibrarySort.LARGEST -> filtered.sortedByDescending { it.sizeBytes }
            }
        }
    }

    Surface(modifier = modifier, color = ZvxBackground) {
        if (showHistoryScreen) {
            PlaybackHistoryScreen(
                entries = recentEntries,
                favoriteUris = favoriteUris,
                onBack = { showHistoryScreen = false },
                onClearAll = { showClearHistoryDialog = true },
                onRemove = { uri ->
                    PlaybackHistoryStore.remove(context, uri)
                    recentUris = PlaybackHistoryStore.getRecent(context)
                },
                onToggleFavorite = { uri ->
                    favoriteUris = MediaFavoritesStore.toggle(context, uri)
                },
                onPlay = { entry ->
                    PlaybackHistoryStore.markPlayed(context, entry.uri.toString())
                    recentUris = PlaybackHistoryStore.getRecent(context)
                    val queue = recentEntries.filter { it.kind == entry.kind }
                    onPlayMedia(entry.uri.toString(), entry.kind == MediaKind.AUDIO, queue)
                }
            )
        } else if (showFavoritesScreen) {
            FavoritesScreen(
                entries = favoriteEntries,
                onBack = { showFavoritesScreen = false },
                onClearAll = { showClearFavoritesDialog = true },
                onRemove = { uri ->
                    favoriteUris = MediaFavoritesStore.toggle(context, uri)
                },
                onPlay = { entry ->
                    PlaybackHistoryStore.markPlayed(context, entry.uri.toString())
                    recentUris = PlaybackHistoryStore.getRecent(context)
                    val queue = favoriteEntries.filter { it.kind == entry.kind }
                    onPlayMedia(entry.uri.toString(), entry.kind == MediaKind.AUDIO, queue)
                }
            )
        } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BrandMark()
                Spacer(Modifier.size(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("ZVX", color = ZvxText, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text("YOUR MEDIA", color = ZvxMuted, fontSize = 9.sp, letterSpacing = 1.7.sp)
                }
                IconButton(onClick = {
                    access = MediaPermission.current(context)
                    reloadToken++
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh library", tint = ZvxMuted)
                }
                IconButton(onClick = onChooseFile) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Open file", tint = ZvxText)
                }
                IconButton(
                    onClick = {
                        playlistLauncher.launch(
                            arrayOf("audio/x-mpegurl", "application/vnd.apple.mpegurl", "text/plain", "application/octet-stream")
                        )
                    }
                ) {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = "Import M3U playlist", tint = ZvxAccent)
                }
                IconButton(onClick = { showStreamDialog = true }) {
                    Icon(Icons.Default.Link, contentDescription = "Play from URL", tint = ZvxAccent)
                }
                IconButton(onClick = { showSettingsDialog = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = ZvxText)
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Your library", color = ZvxText, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)
                Text(
                    if (!access.hasAny) {
                        "Give ZVX access to build your local collection."
                    } else {
                        media.count { it.kind == MediaKind.VIDEO }.toString() + " videos  ·  " +
                            media.count { it.kind == MediaKind.AUDIO }.toString() + " audio tracks"
                    },
                    color = ZvxMuted,
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ZvxMuted) },
                    placeholder = { Text("Search title, artist or album") },
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ZvxAccent,
                        unfocusedBorderColor = ZvxSurfaceRaised,
                        focusedTextColor = ZvxText,
                        unfocusedTextColor = ZvxText,
                        cursorColor = ZvxAccent
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LibraryFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.label) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFolder == "All folders",
                        onClick = { selectedFolder = "All folders" },
                        label = { Text("All folders") }
                    )
                    folders.forEach { folder ->
                        FilterChip(
                            selected = selectedFolder == folder,
                            onClick = { selectedFolder = folder },
                            label = { Text(folder) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        visibleMedia.size.toString() + " items",
                        color = ZvxMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showSortDialog = true }) {
                        Text("Sort · " + selectedSort.label, color = ZvxAccent, fontSize = 12.sp)
                    }
                }
            }

            if (!access.hasAny || !access.hasAudio || !access.hasVideo) {
                PermissionCard(
                    videoAccess = access.hasVideo,
                    audioAccess = access.hasAudio,
                    onRequest = {
                        permissionLauncher.launch(MediaPermission.permissionsToRequest())
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ZvxAccent)
                }
            } else if (visibleMedia.isEmpty()) {
                EmptyLibrary(
                    hasAccess = access.hasAny,
                    filter = selectedFilter,
                    onChooseFile = onChooseFile,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(visibleMedia, key = { it.uri.toString() }) { entry ->
                        MediaRow(
                            entry = entry,
                            favorite = entry.uri.toString() in favoriteUris,
                            onToggleFavorite = {
                                favoriteUris = MediaFavoritesStore.toggle(context, entry.uri.toString())
                            },
                            onClick = {
                                PlaybackHistoryStore.markPlayed(context, entry.uri.toString())
                                recentUris = PlaybackHistoryStore.getRecent(context)
                                val queue = visibleMedia.filter { it.kind == entry.kind }
                                onPlayMedia(entry.uri.toString(), entry.kind == MediaKind.AUDIO, queue)
                            }
                        )
                    }
                }
            }
        }
        }
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("ZVX settings", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("LIBRARY", color = ZvxMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Default filter: ${selectedFilter.label}", color = ZvxText, fontSize = 13.sp)
                    Text("Default sorting: ${selectedSort.label}", color = ZvxText, fontSize = 13.sp)
                    Text("Your filter and sorting choices are saved on this device.", color = ZvxMuted, fontSize = 11.sp)
                    Text("DATA MANAGEMENT", color = ZvxMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Playback history · ${recentUris.size} items", color = ZvxText, fontSize = 13.sp)
                    TextButton(onClick = { showSettingsDialog = false; showHistoryScreen = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Open playback history", color = ZvxAccent)
                    }
                    TextButton(onClick = { showSettingsDialog = false; showClearHistoryDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Clear playback history", color = MaterialTheme.colorScheme.error)
                    }
                    Text("Favorites · ${favoriteUris.size} items", color = ZvxText, fontSize = 13.sp)
                    TextButton(onClick = { showSettingsDialog = false; showFavoritesScreen = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Open favorites", color = ZvxAccent)
                    }
                    TextButton(onClick = { showSettingsDialog = false; showClearFavoritesDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Clear favorites", color = MaterialTheme.colorScheme.error)
                    }
                    Text("Media permissions and playback data remain on your device. ZVX does not upload this library information.", color = ZvxMuted, fontSize = 11.sp)
                }
            },
            confirmButton = { TextButton(onClick = { showSettingsDialog = false }) { Text("Done", color = ZvxAccent) } },
            containerColor = ZvxSurface,
            titleContentColor = ZvxText,
            textContentColor = ZvxMuted
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear playback history?") },
            text = { Text("This removes the recent-played list. Saved playback positions are kept.") },
            confirmButton = {
                TextButton(onClick = {
                    PlaybackHistoryStore.clear(context)
                    recentUris = emptyList()
                    showClearHistoryDialog = false
                }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showClearHistoryDialog = false
                    showSettingsDialog = !showHistoryScreen
                }) { Text("Cancel") }
            },
            containerColor = ZvxSurface,
            titleContentColor = ZvxText,
            textContentColor = ZvxMuted
        )
    }

    if (showClearFavoritesDialog) {
        AlertDialog(
            onDismissRequest = { showClearFavoritesDialog = false },
            title = { Text("Clear all favorites?") },
            text = { Text("This removes favorite markers but does not delete your media files.") },
            confirmButton = {
                TextButton(onClick = {
                    MediaFavoritesStore.clear(context)
                    favoriteUris = emptyList()
                    showClearFavoritesDialog = false
                }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showClearFavoritesDialog = false
                    showSettingsDialog = !showFavoritesScreen
                }) { Text("Cancel") }
            },
            containerColor = ZvxSurface,
            titleContentColor = ZvxText,
            textContentColor = ZvxMuted
        )
    }

    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text("Sort library", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LibrarySort.entries.forEach { sort ->
                        TextButton(
                            onClick = {
                                selectedSort = sort
                                showSortDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                sort.label,
                                color = if (selectedSort == sort) ZvxAccent else ZvxText,
                                modifier = Modifier.weight(1f)
                            )
                            if (selectedSort == sort) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Selected", tint = ZvxAccent)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortDialog = false }) { Text("Done") }
            },
            containerColor = ZvxSurface,
            titleContentColor = ZvxText,
            textContentColor = ZvxMuted
        )
    }

    if (playlistImportError != null) {
        AlertDialog(
            onDismissRequest = { playlistImportError = null },
            title = { Text("Playlist import") },
            text = { Text(playlistImportError.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { playlistImportError = null }) { Text("OK") }
            },
            containerColor = ZvxSurface,
            titleContentColor = ZvxText,
            textContentColor = ZvxMuted
        )
    }

    if (showStreamDialog) {
        StreamDialog(
            onDismiss = { showStreamDialog = false },
            onOpen = { address ->
                showStreamDialog = false
                onOpenStream(address)
            }
        )
    }
}

private fun savedUriEntry(uriString: String, index: Int, fallbackFolder: String): MediaEntry? {
    return runCatching {
        val uri = Uri.parse(uriString)
        val displayName = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: "Saved media item"
        val extension = displayName.substringAfterLast('.', "").lowercase()
        val kind = if (extension in setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "opus", "wma")) {
            MediaKind.AUDIO
        } else {
            MediaKind.VIDEO
        }
        MediaEntry(
            id = -(index.toLong() + 1L),
            uri = uri,
            title = displayName.substringBeforeLast('.', displayName),
            displayName = displayName,
            mimeType = if (kind == MediaKind.AUDIO) "audio/*" else "video/*",
            durationMs = 0L,
            sizeBytes = 0L,
            dateAddedSeconds = 0L,
            folderName = uri.host ?: fallbackFolder,
            kind = kind
        )
    }.getOrNull()
}

@Composable
private fun FavoritesScreen(
    entries: List<MediaEntry>,
    onBack: () -> Unit,
    onClearAll: () -> Unit,
    onRemove: (String) -> Unit,
    onPlay: (MediaEntry) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ZvxText)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Favorites", color = ZvxText, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("${entries.size} saved items", color = ZvxMuted, fontSize = 11.sp)
            }
            TextButton(onClick = onClearAll, enabled = entries.isNotEmpty()) {
                Text("Clear all", color = if (entries.isEmpty()) ZvxMuted else MaterialTheme.colorScheme.error)
            }
        }
        if (entries.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.StarBorder, contentDescription = null, tint = ZvxMuted, modifier = Modifier.size(46.dp))
                Spacer(Modifier.height(12.dp))
                Text("No favorites yet", color = ZvxText, fontWeight = FontWeight.SemiBold)
                Text("Tap the star on any library item to save it here.", color = ZvxMuted, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(entries, key = { it.uri.toString() }) { entry ->
                    Surface(
                        onClick = { onPlay(entry) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(17.dp),
                        color = ZvxSurface
                    ) {
                        Row(
                            modifier = Modifier.padding(11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MediaThumbnail(entry = entry, modifier = Modifier.size(58.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(entry.title, color = ZvxText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(entry.subtitleLine(), color = ZvxMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(entry.folderName, color = ZvxMuted.copy(alpha = 0.7f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { onRemove(entry.uri.toString()) }) {
                                Icon(Icons.Default.Star, contentDescription = "Remove from favorites", tint = ZvxAccent)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaybackHistoryScreen(
    entries: List<MediaEntry>,
    favoriteUris: List<String>,
    onBack: () -> Unit,
    onClearAll: () -> Unit,
    onRemove: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onPlay: (MediaEntry) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ZvxText)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Playback history", color = ZvxText, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("${entries.size} recently played", color = ZvxMuted, fontSize = 11.sp)
            }
            TextButton(onClick = onClearAll, enabled = entries.isNotEmpty()) {
                Text("Clear all", color = if (entries.isEmpty()) ZvxMuted else MaterialTheme.colorScheme.error)
            }
        }
        if (entries.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = ZvxMuted, modifier = Modifier.size(46.dp))
                Spacer(Modifier.height(12.dp))
                Text("No playback history yet", color = ZvxText, fontWeight = FontWeight.SemiBold)
                Text("Videos and audio you play will appear here.", color = ZvxMuted, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(entries, key = { it.uri.toString() }) { entry ->
                    Surface(
                        onClick = { onPlay(entry) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(17.dp),
                        color = ZvxSurface
                    ) {
                        Row(
                            modifier = Modifier.padding(11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MediaThumbnail(entry = entry, modifier = Modifier.size(58.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(entry.title, color = ZvxText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(entry.subtitleLine(), color = ZvxMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(entry.folderName, color = ZvxMuted.copy(alpha = 0.7f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { onToggleFavorite(entry.uri.toString()) }) {
                                Icon(
                                    imageVector = if (entry.uri.toString() in favoriteUris) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Toggle favorite",
                                    tint = if (entry.uri.toString() in favoriteUris) ZvxAccent else ZvxMuted
                                )
                            }
                            IconButton(onClick = { onRemove(entry.uri.toString()) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove from history", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
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
private fun PermissionCard(
    videoAccess: Boolean,
    audioAccess: Boolean,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ZvxSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(
                if (!videoAccess && !audioAccess) "Your library is waiting" else "Expand your library",
                color = ZvxText,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                when {
                    !videoAccess && !audioAccess -> "Allow access to local videos and music. On Android 14+, you can limit video access to items you select."
                    !audioAccess -> "Video access is ready. Allow audio access to include music in your library."
                    else -> "Allow video access to add your local movies and clips."
                },
                color = ZvxMuted,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = ZvxAccent, contentColor = ZvxBackground)
            ) {
                Text("Manage library access", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyLibrary(
    hasAccess: Boolean,
    filter: LibraryFilter,
    onChooseFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (filter == LibraryFilter.AUDIO) Icons.Default.AudioFile else Icons.Default.Movie,
            contentDescription = null,
            tint = ZvxMuted,
            modifier = Modifier.size(46.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            when {
                !hasAccess -> "No library access yet"
                filter == LibraryFilter.RECENT -> "Nothing played recently"
                filter == LibraryFilter.AUDIO -> "No audio found"
                filter == LibraryFilter.VIDEO -> "No videos found"
                else -> "Your library is empty"
            },
            color = ZvxText,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            when {
                !hasAccess -> "You can still open a single file without scanning the whole library."
                filter == LibraryFilter.RECENT -> "Play something and it will appear here."
                else -> "Choose a file directly, or add media to your device and refresh."
            },
            color = ZvxMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onChooseFile, border = BorderStroke(1.dp, ZvxSurfaceRaised)) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Open a file")
        }
    }
}

@Composable
private fun MediaRow(
    entry: MediaEntry,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = ZvxSurface
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            MediaThumbnail(entry = entry, modifier = Modifier.size(62.dp))
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(entry.title, color = ZvxText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(entry.subtitleLine(), color = ZvxMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    entry.folderName + "  ·  " + formatDuration(entry.durationMs) + "  ·  " + formatBytes(entry.sizeBytes),
                    color = ZvxMuted.copy(alpha = 0.72f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (favorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (favorite) "Remove from favorites" else "Add to favorites",
                    tint = if (favorite) ZvxAccent else ZvxMuted
                )
            }
            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = ZvxAccent, modifier = Modifier.padding(start = 4.dp).size(24.dp))
        }
    }
}

@Composable
private fun MediaThumbnail(entry: MediaEntry, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(entry.uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(entry.uri) {
        bitmap = kotlinx.coroutines.withContext(Dispatchers.IO) {
            runCatching {
                if (entry.kind == MediaKind.VIDEO && Build.VERSION.SDK_INT >= 29) {
                    context.contentResolver.loadThumbnail(entry.uri, Size(192, 192), null)
                } else if (entry.kind == MediaKind.AUDIO) {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, entry.uri)
                        val bytes = retriever.embeddedPicture
                        if (bytes != null) {
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } else {
                            null
                        }
                    } finally {
                        retriever.release()
                    }
                } else {
                    null
                }
            }.getOrNull()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    if (entry.kind == MediaKind.AUDIO) listOf(Color(0xFF34304B), Color(0xFF202331))
                    else listOf(Color(0xFF34402A), Color(0xFF202921))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val loaded = bitmap
        if (loaded != null) {
            Image(
                bitmap = loaded.asImageBitmap(),
                contentDescription = entry.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = if (entry.kind == MediaKind.AUDIO) Icons.Default.AudioFile else Icons.Default.Movie,
                contentDescription = null,
                tint = ZvxAccent,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}


private fun parseM3uPlaylist(content: String, playlistUri: Uri): List<MediaEntry> {
    val audioExtensions = setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "opus", "wma")
    val result = mutableListOf<MediaEntry>()
    var pendingTitle: String? = null

    content.lineSequence().forEach { rawLine ->
        val line = rawLine.trim().removePrefix("\uFEFF")
        if (line.isBlank()) return@forEach
        if (line.startsWith("#EXTINF:", ignoreCase = true)) {
            pendingTitle = line.substringAfter(',', "").trim().takeIf { it.isNotBlank() }
            return@forEach
        }
        if (line.startsWith("#")) return@forEach

        val mediaUri = resolveM3uEntryUri(line, playlistUri) ?: return@forEach
        val displayName = mediaUri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: "Playlist item " + (result.size + 1)
        val extension = displayName.substringAfterLast('.', "").lowercase()
        val kind = if (extension in audioExtensions) MediaKind.AUDIO else MediaKind.VIDEO
        val title = pendingTitle ?: displayName.substringBeforeLast('.', displayName)
        result += MediaEntry(
            id = (result.size + 1).toLong(),
            uri = mediaUri,
            title = title,
            displayName = displayName,
            mimeType = if (kind == MediaKind.AUDIO) "audio/*" else "video/*",
            durationMs = 0L,
            sizeBytes = 0L,
            dateAddedSeconds = 0L,
            folderName = "M3U playlist",
            kind = kind
        )
        pendingTitle = null
    }
    return result
}

private fun resolveM3uEntryUri(rawPath: String, playlistUri: Uri): Uri? {
    val path = rawPath.trim().trim('"').replace('\\', '/')
    if (path.isBlank()) return null
    val parsed = runCatching { Uri.parse(path) }.getOrNull() ?: return null
    when (parsed.scheme?.lowercase()) {
        "http", "https" -> return parsed.takeIf { !it.host.isNullOrBlank() }
        "content", "file" -> return parsed
    }
    if (path.startsWith("/")) return runCatching { Uri.fromFile(File(path).canonicalFile) }.getOrNull()

    return when (playlistUri.scheme?.lowercase()) {
        "http", "https" -> runCatching {
            val resolved = URI(playlistUri.toString()).resolve(path)
            Uri.parse(resolved.toString()).takeIf { it.scheme?.lowercase() in setOf("http", "https") && !it.host.isNullOrBlank() }
        }.getOrNull()
        "file" -> runCatching {
            val parent = File(playlistUri.path.orEmpty()).parentFile ?: return@runCatching null
            Uri.fromFile(File(parent, path).canonicalFile)
        }.getOrNull()
        "content" -> resolveDocumentRelativeUri(path, playlistUri)
        else -> null
    }
}

private fun resolveDocumentRelativeUri(path: String, playlistUri: Uri): Uri? {
    val authority = playlistUri.authority ?: return null
    val documentId = runCatching { DocumentsContract.getDocumentId(playlistUri) }.getOrNull() ?: return null
    val parentId = documentId.substringBeforeLast('/', missingDelimiterValue = "")
    if (parentId.isBlank()) return null
    val rootPrefix = parentId.substringBefore(':', missingDelimiterValue = "").takeIf { ':' in parentId }?.plus(":").orEmpty()
    val parentPath = if (rootPrefix.isNotEmpty()) parentId.removePrefix(rootPrefix) else parentId
    val segments = parentPath.split('/').filter { it.isNotBlank() }.toMutableList()
    path.split('/').forEach { segment ->
        when (segment) {
            "", "." -> Unit
            ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.lastIndex) else return null
            else -> segments += segment
        }
    }
    return runCatching {
        DocumentsContract.buildDocumentUri(authority, rootPrefix + segments.joinToString("/"))
    }.getOrNull()
}

private fun MediaEntry.subtitleLine(): String {
    return if (kind == MediaKind.AUDIO) {
        listOfNotNull(
            artist?.takeIf { it.isNotBlank() && it != "<unknown>" },
            album?.takeIf { it.isNotBlank() && it != "<unknown>" }
        ).joinToString(" · ").ifBlank { displayName }
    } else {
        val resolution = if (width != null && height != null && width > 0 && height > 0) {
            width.toString() + "×" + height.toString()
        } else null
        listOfNotNull(resolution, mimeType?.substringAfter('/')?.uppercase())
            .joinToString(" · ").ifBlank { displayName }
    }
}

private fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0L) / 1_000L
    val hours = seconds / 3_600L
    val minutes = (seconds / 60L) % 60L
    val remaining = seconds % 60L
    return if (hours > 0L) "%d:%02d:%02d".format(hours, minutes, remaining)
    else "%d:%02d".format(seconds / 60L, remaining)
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "Unknown size"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024.0) "%.1f GB".format(mb / 1024.0) else "%.0f MB".format(mb)
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
                Text("Paste a direct video, audio, HLS or DASH URL.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Media URL") },
                    placeholder = { Text("https://example.com/video.m3u8") },
                    isError = error != null
                )
                if (error != null) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val candidate = address.trim()
                val parsed = runCatching { Uri.parse(candidate) }.getOrNull()
                val valid = parsed != null &&
                    parsed.scheme?.lowercase() in setOf("http", "https") &&
                    !parsed.host.isNullOrBlank()
                if (valid) onOpen(candidate) else error = "Enter a valid HTTP or HTTPS address."
            }) { Text("Play", color = ZvxAccent, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = ZvxSurface,
        titleContentColor = ZvxText,
        textContentColor = ZvxMuted
    )
}
