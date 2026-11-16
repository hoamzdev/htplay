package com.htdev.movie.htplay.data.common

import com.htdev.movie.htplay.data.BuildConfig

/** The API returns both absolute URLs and paths relative to the image host. */
internal fun String?.toImageUrl(): String {
    val path = this?.trim().orEmpty()
    return when {
        path.isEmpty() -> ""
        path.startsWith("https://", ignoreCase = true) ||
            path.startsWith("http://", ignoreCase = true) -> path
        path.startsWith("//") -> "https:$path"
        else -> "${BuildConfig.BASE_URL_IMAGE.trimEnd('/')}/${path.trimStart('/')}"
    }
}
