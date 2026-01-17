package com.curbos.pos.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeUtils {
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
        .withZone(ZoneId.systemDefault())

    fun formatTime(timestamp: Long): String {
        return timeFormatter.format(Instant.ofEpochMilli(timestamp))
    }
}
