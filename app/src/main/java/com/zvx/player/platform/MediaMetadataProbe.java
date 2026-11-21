package com.zvx.player.platform;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

/**
 * Shared Android platform probe for audio metadata and embedded album artwork.
 * Keeps MediaMetadataRetriever lifecycle and defensive artwork decoding in Java.
 */
public final class MediaMetadataProbe {
    private static final int MAX_ARTWORK_BYTES = 16 * 1024 * 1024;
    private static final int MAX_ARTWORK_EDGE_PX = 1024;

    public static final class Snapshot {
        public final String title;
        public final String artist;
        public final String album;
        public final long durationMs;
        public final Bitmap artwork;

        private Snapshot(String title, String artist, String album, long durationMs, Bitmap artwork) {
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.durationMs = durationMs;
            this.artwork = artwork;
        }
    }

    private MediaMetadataProbe() {}

    /** Returns null when the URI is unavailable or unsupported by the platform extractor. */
    public static Snapshot inspect(Context context, Uri uri) {
        if (context == null || uri == null) return null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, uri);
            return new Snapshot(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                parseDuration(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)),
                decodeArtwork(retriever.getEmbeddedPicture())
            );
        } catch (RuntimeException ignored) {
            return null;
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException ignored) {
                // Release is best-effort if initialization failed.
            }
        }
    }

    private static long parseDuration(String value) {
        if (value == null) return 0L;
        try {
            return Math.max(0L, Long.parseLong(value));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static Bitmap decodeArtwork(byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_ARTWORK_BYTES) return null;
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

        int sampleSize = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / sampleSize > MAX_ARTWORK_EDGE_PX) {
            sampleSize *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
    }
}
