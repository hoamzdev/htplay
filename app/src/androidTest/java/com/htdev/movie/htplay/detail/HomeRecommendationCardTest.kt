package com.htdev.movie.htplay.detail

import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.adapter.HomeAdapter
import com.htdev.movie.htplay.domain.entities.DataMovieDetail
import com.htdev.movie.htplay.entities.HomeItem
import com.htdev.movie.htplay.entities.HomeRecommendation
import com.htdev.movie.htplay.widget.checkNetworkConnected
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeRecommendationCardTest {
    @Test fun trailerRowEmbedsTheSelectedMovieAndUnloadsWhenDetached() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val committed = CountDownLatch(1)
        var watchedSlug: String? = null
        var favoriteClicks = 0
        val adapter = HomeAdapter(
            onWatchRecommendation = { watchedSlug = it },
            onFavoriteRecommendation = { favoriteClicks++ },
        )
        lateinit var recycler: RecyclerView
        ActivityScenario.launch<DetailActivity>(DetailActivity.createIntent(context, "")).use { scenario ->
            try {
                scenario.onActivity { activity ->
                    recycler = RecyclerView(activity).apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        layoutManager = LinearLayoutManager(activity)
                        this.adapter = adapter
                    }
                    activity.setContentView(recycler)
                    adapter.submitList(listOf(HomeItem.TrailerSector(HomeRecommendation(
                        movie = DataMovieDetail(slug = "picked", name = "Picked movie", trailerUrl = "https://youtu.be/M7lc1UVf-VE"),
                        isLoading = false, isFavorite = false,
                    )))) { committed.countDown() }
                    adapter.resumeTrailers()
                }
                assertTrue(committed.await(5, TimeUnit.SECONDS))
                val deadline = SystemClock.elapsedRealtime() + 10_000
                var source = ""
                var origin = ""
                while (SystemClock.elapsedRealtime() < deadline && !source.contains("youtube.com/embed/M7lc1UVf-VE")) {
                    val evaluated = CountDownLatch(1)
                    scenario.onActivity {
                        val row = recycler.findViewHolderForAdapterPosition(0)?.itemView
                        val web = row?.findViewById<WebView>(R.id.wb_trailer)
                        if (web == null) evaluated.countDown() else {
                            web.evaluateJavascript("[document.querySelector('iframe') ? document.querySelector('iframe').src : '', location.origin]") {
                                val page = JSONArray(it)
                                source = page.optString(0)
                                origin = page.optString(1)
                                evaluated.countDown()
                            }
                        }
                    }
                    assertTrue(evaluated.await(2, TimeUnit.SECONDS))
                    if (!source.contains("youtube.com/embed/M7lc1UVf-VE")) SystemClock.sleep(100)
                }
                assertTrue("The row did not load the selected trailer: $source", source.contains("youtube.com/embed/M7lc1UVf-VE"))
                assertEquals("https://${context.packageName}", origin)
                lateinit var web: WebView
                scenario.onActivity {
                    val row = requireNotNull(recycler.findViewHolderForAdapterPosition(0)).itemView
                    web = row.findViewById(R.id.wb_trailer)
                    assertFalse(web.settings.allowFileAccess)
                    assertFalse(web.settings.allowContentAccess)
                    assertEquals(View.GONE, row.findViewById<View>(R.id.tv_trailer_status).visibility)
                    if (checkNetworkConnected(context)) {
                        row.findViewById<View>(R.id.btn_watch_now).performClick()
                        row.findViewById<View>(R.id.btn_favorite).performClick()
                        assertEquals("picked", watchedSlug)
                        assertEquals(1, favoriteClicks)
                    }
                    recycler.adapter = null
                }
                val pauseDeadline = SystemClock.elapsedRealtime() + 5_000
                var blank = false
                while (SystemClock.elapsedRealtime() < pauseDeadline && !blank) {
                    val evaluated = CountDownLatch(1)
                    scenario.onActivity {
                        web.evaluateJavascript("document.querySelector('iframe') === null") {
                            blank = it == "true"
                            evaluated.countDown()
                        }
                    }
                    assertTrue(evaluated.await(2, TimeUnit.SECONDS))
                    if (!blank) SystemClock.sleep(100)
                }
                assertTrue("Detached trailer kept its player page", blank)
            } finally {
                scenario.onActivity {
                    recycler.adapter = null
                    adapter.releaseTrailers()
                }
            }
        }
    }
}
