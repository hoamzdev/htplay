package com.example.movie.util

fun playbackProgressPercent(currentSeconds: Long?, totalSeconds: Long?): Int {
    val total = totalSeconds ?: return 0
    if (total <= 0) return 0
    val current = (currentSeconds ?: 0).coerceIn(0, total)
    return (current.toDouble() / total.toDouble() * 100).toInt().coerceIn(0, 100)
}
