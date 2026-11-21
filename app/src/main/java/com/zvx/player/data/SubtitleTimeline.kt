package com.zvx.player.data

/**
 * Applies a millisecond offset to SubRip and WebVTT cue boundaries.
 * Positive offsets move cues later; negative offsets move them earlier.
 */
object SubtitleTimeline {
    private val cueTimingLine = Regex(
        """^(\\s*)(\\d{1,}:\\d{2}(?::\\d{2})?[,.]\\d{3})(\\s+-->\\s+)(\\d{1,}:\\d{2}(?::\\d{2})?[,.]\\d{3})(.*)$"""
    )

    fun shift(content: String, offsetMs: Int): String {
        if (offsetMs == 0) return content
        return content.lineSequence().joinToString("\n") { line ->
            val match = cueTimingLine.matchEntire(line) ?: return@joinToString line
            val prefix = match.groupValues[1]
            val start = shiftTimestamp(match.groupValues[2], offsetMs)
            val separator = match.groupValues[3]
            val end = shiftTimestamp(match.groupValues[4], offsetMs)
            val suffix = match.groupValues[5]
            prefix + start + separator + end + suffix
        }
    }

    private fun shiftTimestamp(timestamp: String, offsetMs: Int): String {
        val normalized = timestamp.replace(',', '.')
        val parts = normalized.split(':')
        if (parts.size !in 2..3) return timestamp
        val finalPart = parts.last().split('.')
        if (finalPart.size != 2) return timestamp

        val seconds = finalPart[0].toLongOrNull() ?: return timestamp
        val milliseconds = finalPart[1].toLongOrNull() ?: return timestamp
        val baseMs = if (parts.size == 3) {
            val hours = parts[0].toLongOrNull() ?: return timestamp
            val minutes = parts[1].toLongOrNull() ?: return timestamp
            hours * 3_600_000L + minutes * 60_000L + seconds * 1_000L + milliseconds
        } else {
            val minutes = parts[0].toLongOrNull() ?: return timestamp
            minutes * 60_000L + seconds * 1_000L + milliseconds
        }
        val shifted = (baseMs + offsetMs.toLong()).coerceAtLeast(0L)
        val wholeSeconds = shifted / 1_000L
        val outputSeconds = wholeSeconds % 60L
        val outputMinutes = if (parts.size == 3) (wholeSeconds / 60L) % 60L else wholeSeconds / 60L
        val outputHours = wholeSeconds / 3_600L
        val outputMillis = shifted % 1_000L
        val separator = if (timestamp.contains(',')) ',' else '.'
        val formatted = if (parts.size == 3) {
            "%02d:%02d:%02d.%03d".format(outputHours, outputMinutes, outputSeconds, outputMillis)
        } else {
            "%02d:%02d.%03d".format(outputMinutes, outputSeconds, outputMillis)
        }
        return if (separator == ',') formatted.replace('.', ',') else formatted
    }
}
