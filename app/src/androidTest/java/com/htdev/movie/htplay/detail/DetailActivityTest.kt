package com.htdev.movie.htplay.detail

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.htdev.movie.htplay.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetailActivityTest {
    @Test fun openingDetailInitializesItsScrollingSectionsWithoutCrashing() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // An empty slug exercises screen creation without a network request or video playback.
        ActivityScenario.launch<DetailActivity>(DetailActivity.createIntent(context, "")).use { scenario ->
            scenario.onActivity { activity ->
                val content = activity.findViewById<RecyclerView>(R.id.scroll_detail)
                assertTrue(content.layoutManager is GridLayoutManager)
                assertEquals(2, content.adapter?.itemCount) // Header and recommendations.
            }
        }
    }
}
