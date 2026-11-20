package com.zvx.player.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore

object MediaStoreRepository {
    fun load(context: Context, access: MediaAccess): List<MediaEntry> {
        val items = ArrayList<MediaEntry>()
        if (access.hasVideo) {
            runCatching { queryVideos(context) }.getOrNull()?.let(items::addAll)
        }
        if (access.hasAudio) {
            runCatching { queryAudio(context) }.getOrNull()?.let(items::addAll)
        }
        return items.sortedByDescending { it.dateAddedSeconds }
    }

    private fun queryVideos(context: Context): List<MediaEntry> {
        val resolver = context.contentResolver
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = buildList {
            add(MediaStore.Video.Media._ID)
            add(MediaStore.Video.Media.DISPLAY_NAME)
            add(MediaStore.Video.Media.TITLE)
            add(MediaStore.Video.Media.MIME_TYPE)
            add(MediaStore.Video.Media.DURATION)
            add(MediaStore.Video.Media.SIZE)
            add(MediaStore.Video.Media.DATE_ADDED)
            if (Build.VERSION.SDK_INT >= 29) {
                add(MediaStore.Video.Media.WIDTH)
                add(MediaStore.Video.Media.HEIGHT)
                add(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            }
        }.toTypedArray()
        val result = ArrayList<MediaEntry>()

        resolver.query(collection, projection, null, null, MediaStore.Video.Media.DATE_ADDED + " DESC")?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
            val titleColumn = cursor.getColumnIndex(MediaStore.Video.Media.TITLE)
            val mimeColumn = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
            val durationColumn = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
            val dateColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
            val widthColumn = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
            val heightColumn = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val displayName = cursor.readString(nameColumn) ?: "Video $id"
                val title = cursor.readString(titleColumn)?.takeIf { it.isNotBlank() }
                    ?: displayName.substringBeforeLast('.', displayName)
                result += MediaEntry(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    title = title,
                    displayName = displayName,
                    mimeType = cursor.readString(mimeColumn),
                    durationMs = cursor.readLongOrZero(durationColumn),
                    sizeBytes = cursor.readLongOrZero(sizeColumn),
                    dateAddedSeconds = cursor.readLongOrZero(dateColumn),
                    width = cursor.readIntOrNull(widthColumn),
                    height = cursor.readIntOrNull(heightColumn),
                    folderName = if (Build.VERSION.SDK_INT >= 29) {
                        cursor.readString(cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)) ?: "Other"
                    } else "Other",
                    kind = MediaKind.VIDEO
                )
            }
        }
        return result
    }

    private fun queryAudio(context: Context): List<MediaEntry> {
        val resolver = context.contentResolver
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM
        ).let { base ->
            if (Build.VERSION.SDK_INT >= 29) base + MediaStore.MediaColumns.BUCKET_DISPLAY_NAME else base
        }
        val result = ArrayList<MediaEntry>()

        resolver.query(collection, projection, null, null, MediaStore.Audio.Media.DATE_ADDED + " DESC")?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
            val titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val mimeColumn = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
            val durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val sizeColumn = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
            val dateColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
            val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val displayName = cursor.readString(nameColumn) ?: "Audio $id"
                val title = cursor.readString(titleColumn)?.takeIf { it.isNotBlank() }
                    ?: displayName.substringBeforeLast('.', displayName)
                result += MediaEntry(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    title = title,
                    displayName = displayName,
                    mimeType = cursor.readString(mimeColumn),
                    durationMs = cursor.readLongOrZero(durationColumn),
                    sizeBytes = cursor.readLongOrZero(sizeColumn),
                    dateAddedSeconds = cursor.readLongOrZero(dateColumn),
                    artist = cursor.readString(artistColumn),
                    album = cursor.readString(albumColumn),
                    folderName = if (Build.VERSION.SDK_INT >= 29) {
                        cursor.readString(cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)) ?: "Other"
                    } else "Other",
                    kind = MediaKind.AUDIO
                )
            }
        }
        return result
    }

    private fun android.database.Cursor.readString(index: Int): String? =
        if (index < 0 || isNull(index)) null else getString(index)

    private fun android.database.Cursor.readLongOrZero(index: Int): Long =
        if (index < 0 || isNull(index)) 0L else getLong(index)

    private fun android.database.Cursor.readIntOrNull(index: Int): Int? =
        if (index < 0 || isNull(index)) null else getInt(index)
}
