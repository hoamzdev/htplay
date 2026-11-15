package com.htdev.movie.htplay.util

import java.text.Normalizer
import java.util.Locale

internal fun String.searchKey(): String = Normalizer.normalize(
    lowercase(Locale.ROOT).replace('đ', 'd'), Normalizer.Form.NFD,
).replace(SEARCH_DIACRITICS, "").replace('-', ' ').trim().replace(SEARCH_WHITESPACE, " ")

private val SEARCH_DIACRITICS = Regex("\\p{M}+")
private val SEARCH_WHITESPACE = Regex("\\s+")
