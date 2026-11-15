package com.htdev.movie.htplay.util

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale

internal fun trailerHtml(trailerUrl: String, appId: String): String? {
    val uri = runCatching { URI(trailerUrl.trim().let { if (it.startsWith("//")) "https:$it" else it }) }.getOrNull()
        ?: return null
    val host = uri.host?.lowercase(Locale.ROOT) ?: return null
    if (uri.userInfo != null || uri.scheme?.lowercase(Locale.ROOT) !in setOf("http", "https")) return null
    val origin = "https://${appId.lowercase(Locale.ROOT)}"
    val source: String
    if (host in YOUTUBE_HOSTS) {
        val path = uri.path.orEmpty().trim('/').split('/')
        val id = when {
            host == "youtu.be" || host == "www.youtu.be" -> path.firstOrNull()
            path.firstOrNull() in setOf("embed", "shorts", "live") -> path.getOrNull(1)
            path.firstOrNull() == "watch" -> runCatching {
                uri.rawQuery.orEmpty().split('&').firstOrNull { it.substringBefore('=') == "v" }
                    ?.substringAfter('=')?.let { URLDecoder.decode(it, "UTF-8") }
            }.getOrNull()
            else -> null
        }
        if (id == null || !YOUTUBE_ID.matches(id)) return null
        source = "https://www.youtube.com/embed/$id?autoplay=1&loop=1&playlist=$id&controls=0&playsinline=1&rel=0&origin=${URLEncoder.encode(origin, "UTF-8")}"
    } else {
        if (!uri.scheme.equals("https", ignoreCase = true)) return null
        source = uri.toASCIIString()
    }
    val escaped = source.replace("&", "&amp;").replace("\"", "&quot;")
        .replace("<", "&lt;").replace(">", "&gt;").replace("'", "&#39;")
    val player = if (host !in YOUTUBE_HOSTS && uri.path.orEmpty().lowercase(Locale.ROOT).let {
            it.endsWith(".mp4") || it.endsWith(".webm")
        }) {
        """<video autoplay loop playsinline preload="auto" src="$escaped"></video>"""
    } else {
        """<iframe src="$escaped" title="Trailer" allow="autoplay; encrypted-media; picture-in-picture; fullscreen" referrerpolicy="strict-origin-when-cross-origin" allowfullscreen></iframe>"""
    }
    return """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="referrer" content="strict-origin-when-cross-origin"><style>html,body{margin:0;width:100%;height:100%;background:#000}iframe,video{display:block;width:100%;height:100%;border:0}</style></head><body>$player</body></html>"""
}

private val YOUTUBE_ID = Regex("[A-Za-z0-9_-]{11}")
private val YOUTUBE_HOSTS = setOf(
    "youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be", "www.youtu.be",
    "youtube-nocookie.com", "www.youtube-nocookie.com",
)
