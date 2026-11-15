package com.htdev.movie.htplay.home.download

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.htdev.movie.htplay.util.searchKey

internal data class DownloadMetadata(
    val title: String,
    val episodeNumber: Int?,
    val movieSlug: String? = null,
    val posterUrl: String? = null,
) {
    fun matchesSearch(query: String, episodeLabel: String): Boolean {
        val key = query.searchKey()
        return title.searchKey().contains(key) || movieSlug.orEmpty().searchKey().contains(key) ||
            episodeLabel.searchKey().contains(key)
    }

    fun toByteArray(): ByteArray = JsonObject().apply {
        addProperty("title", title)
        addProperty("episodeNumber", requireNotNull(episodeNumber).also { require(it > 0) })
        movieSlug?.takeIf { it.isNotBlank() }?.let { addProperty("movieSlug", it) }
        posterUrl?.takeIf { it.isNotBlank() }?.let { addProperty("posterUrl", it) }
    }.toString().toByteArray(Charsets.UTF_8)

    companion object {
        fun fromByteArray(data: ByteArray): DownloadMetadata {
            val text = data.toString(Charsets.UTF_8)
            if (text.startsWith("{")) {
                runCatching {
                    val json = JsonParser.parseString(text).asJsonObject
                    val number = json.get("episodeNumber").asInt
                    require(number > 0)
                    DownloadMetadata(
                        json.get("title").asString, number,
                        json.get("movieSlug")?.takeUnless { it.isJsonNull }?.asString?.takeIf { it.isNotBlank() },
                        json.get("posterUrl")?.takeUnless { it.isJsonNull }?.asString?.takeIf { it.isNotBlank() },
                    )
                }.getOrNull()?.let { return it }
            }

            // Older downloads stored "movie title - episode slug" as plain text.
            val title = text.substringBeforeLast(" - ", text)
            val slug = text.substringAfterLast(" - ", "").trim()
            val number = LEGACY_EPISODE.matchEntire(slug)?.groupValues?.get(1)
                ?.toIntOrNull()?.takeIf { it > 0 }
                ?: 1.takeIf { slug.equals("full", ignoreCase = true) }
            return if (number != null) DownloadMetadata(title, number) else DownloadMetadata(text, null)
        }

        private val LEGACY_EPISODE = Regex("(?:tap[- _]?|episode[- _]?|ep[- _]?)?(\\d+)", RegexOption.IGNORE_CASE)
    }
}
