package com.example.data.common

/** The API returns both absolute URLs and paths relative to the image host. */
internal fun String?.toImageUrl(): String {
    val path = this?.trim().orEmpty()
    return when {
        path.isEmpty() -> ""
        path.startsWith("https://", ignoreCase = true) ||
            path.startsWith("http://", ignoreCase = true) -> path
        path.startsWith("//") -> "https:$path"
        else -> "${Api.BASE_URL_IMAGE}/${path.trimStart('/')}"
    }
}
