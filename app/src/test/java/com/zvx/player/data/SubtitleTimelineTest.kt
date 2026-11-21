package com.zvx.player.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleTimelineTest {
    @Test
    fun shiftsSubRipStartAndEndTimesForward() {
        val source = "1\n00:00:01,000 --> 00:00:02,500\nHello"
        val expected = "1\n00:00:01,250 --> 00:00:02,750\nHello"
        assertEquals(expected, SubtitleTimeline.shift(source, 250))
    }

    @Test
    fun shiftsWebVttMinuteSecondTimesBackward() {
        val source = "WEBVTT\n\n00:01.500 --> 00:03.000 align:start\nHello"
        val expected = "WEBVTT\n\n00:01.000 --> 00:02.500 align:start\nHello"
        assertEquals(expected, SubtitleTimeline.shift(source, -500))
    }

    @Test
    fun clampsEarlyCuesAtZero() {
        val source = "00:00:00,100 --> 00:00:01,000\nHello"
        val expected = "00:00:00,000 --> 00:00:00,500\nHello"
        assertEquals(expected, SubtitleTimeline.shift(source, -500))
    }

    @Test
    fun zeroOffsetDoesNotChangeSource() {
        val source = "WEBVTT\n\n00:01.000 --> 00:02.000\nHello\n"
        assertEquals(source, SubtitleTimeline.shift(source, 0))
    }
}
