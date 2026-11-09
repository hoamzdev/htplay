package com.htdev.movie.htplay.detail

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isSelected
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.domain.entities.MoviePlayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.hamcrest.Matchers.allOf
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class DetailActivityTest {
    @Test fun openingDetailInitializesItsScrollingSectionsWithoutCrashing() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // An empty slug exercises screen creation without a network request or video playback.
        ActivityScenario.launch<DetailActivity>(DetailActivity.createIntent(context, "")).use { scenario ->
            scenario.onActivity { activity ->
                val content = activity.findViewById<RecyclerView>(R.id.scroll_detail)
                assertTrue(content.layoutManager is LinearLayoutManager)
                assertEquals(2, content.adapter?.itemCount) // Header and recommendations.
            }
        }
    }

    @Test fun longSeriesRecyclesRowsWithoutLosingNumbersClicksOrSelection() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val episodes = (1..125).map { MoviePlayer("episode-$it", "https://example.com/$it.m3u8") }
        var clicked: MoviePlayer? = null
        val rowsInserted = CountDownLatch(1)
        ActivityScenario.launch<DetailActivity>(DetailActivity.createIntent(context, "")).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<View>(R.id.tv_episodes).performClick()
                val content = activity.findViewById<RecyclerView>(R.id.scroll_detail)
                val adapter = (content.adapter as ConcatAdapter).adapters[1] as EpisodeAdapter
                adapter.onEpisodeClick = { clicked = it }
                adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                        rowsInserted.countDown()
                    }
                })
                adapter.submitEpisodes(episodes)
                adapter.selectedEpisodeId = episodes.last().id
            }
            assertTrue("Episode rows were not committed", rowsInserted.await(5, TimeUnit.SECONDS))
            scenario.onActivity { activity ->
                val content = activity.findViewById<RecyclerView>(R.id.scroll_detail)
                val adapter = (content.adapter as ConcatAdapter).adapters[1] as EpisodeAdapter
                assertEquals(21, adapter.itemCount)
                content.scrollToPosition(21) // Header at 0, final episode row at 21.
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val content = activity.findViewById<RecyclerView>(R.id.scroll_detail)
                val row = requireNotNull(content.findViewHolderForAdapterPosition(21)).itemView
                val lastEpisode = row.findViewById<TextView>(R.id.episode_5)
                assertEquals("125", lastEpisode.text.toString())
                assertTrue(lastEpisode.isSelected)
                assertEquals(View.INVISIBLE, row.findViewById<View>(R.id.episode_6).visibility)
                lastEpisode.performClick()
                assertEquals(episodes.last(), clicked)
                content.scrollToPosition(1)
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val content = activity.findViewById<RecyclerView>(R.id.scroll_detail)
                val row = requireNotNull(content.findViewHolderForAdapterPosition(1)).itemView
                val first = row.findViewById<TextView>(R.id.episode_1)
                val sixth = row.findViewById<TextView>(R.id.episode_6)
                assertEquals("1", first.text.toString())
                assertEquals("6", sixth.text.toString())
                assertEquals(View.VISIBLE, sixth.visibility)
                assertFalse(first.isSelected)
                assertFalse(sixth.isSelected)
                sixth.performClick()
                assertEquals(episodes[5], clicked)
                val adapter = (content.adapter as ConcatAdapter).adapters[1] as EpisodeAdapter
                adapter.selectedEpisodeId = episodes[5].id
            }
            onView(allOf(withId(R.id.episode_6), withText("6"))).check(matches(isSelected()))
        }
    }
}
