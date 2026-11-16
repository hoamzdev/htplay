package com.htdev.movie.htplay.util

import org.junit.Assert.*
import org.junit.Test

class TrailerEmbedTest {
    @Test fun youtubeWatchShortEmbedAndShortsUrlsUseTheEmbeddedPlayerWithAppIdentity() {
        for (url in listOf(
            "https://www.youtube.com/watch?v=abcdefghijk&feature=share",
            "https://youtu.be/abcdefghijk?t=12",
            "https://youtube.com/embed/abcdefghijk",
            "https://m.youtube.com/shorts/abcdefghijk",
            "http://youtube.com/watch?v=abcdefghijk",
            "https://www.youtube-nocookie.com/embed/abcdefghijk",
        )) {
            val html = requireNotNull(trailerHtml(url, "com.htdev.movie.htplay"))
            assertTrue(url, html.contains("https://www.youtube.com/embed/abcdefghijk?"))
            assertTrue(html.contains("origin=https%3A%2F%2Fcom.htdev.movie.htplay"))
            assertTrue(html.contains("referrerpolicy=\"strict-origin-when-cross-origin\""))
            assertTrue(html.contains("autoplay=1"))
            assertTrue(html.contains("loop=1"))
            assertTrue(html.contains("playlist=abcdefghijk"))
            assertTrue(html.contains("controls=0"))
            assertTrue(html.contains("allow=\"autoplay;"))
        }
    }

    @Test fun directVideoUrlsAutoplayWithoutControlsAndEscapeUrlAttributes() {
        for (extension in listOf("mp4", "webm")) {
            val html = requireNotNull(trailerHtml("https://cdn.example.com/trailer.$extension?a=1&b=2", "com.example.app"))
            assertTrue(html.contains("<video autoplay loop playsinline"))
            assertFalse(html.contains("controls"))
            assertTrue(html.contains("?a=1&amp;b=2"))
        }
    }

    @Test fun absentMalformedAndNonWebUrlsDoNotCreateAnEmbeddedPage() {
        for (url in listOf("", " ", "javascript:alert(1)", "file:///sdcard/video.mp4", "data:text/html,test",
            "https://youtube.com/watch?v=bad", "https://youtube.com/watch", "http://cdn.example.com/video.mp4")) {
            assertNull(url, trailerHtml(url, "com.example.app"))
        }
    }
}
