package com.htdev.movie.htplay.language

import android.app.Instrumentation.ActivityMonitor
import android.content.Context
import android.os.SystemClock
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.intro.IntroActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LanguageActivityTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val preferences = context.getSharedPreferences("MovieSharedPref", Context.MODE_PRIVATE)
    private var originalLanguage: String? = null

    @Before fun startInEnglish() {
        originalLanguage = preferences.getString("KEY_LANGUAGE", null)
        instrumentation.runOnMainSync { LanguageHelper.saveLocale(context, "en") }
    }

    @After fun restoreLanguage() {
        preferences.edit().apply {
            if (originalLanguage == null) remove("KEY_LANGUAGE") else putString("KEY_LANGUAGE", originalLanguage)
        }.commit()
        instrumentation.runOnMainSync {
            LanguageHelper.clearTemporaryLocale()
            LanguageHelper.refreshApplicationLanguage(context)
        }
    }

    @Test fun previewDoesNotSaveAndSurvivesActivityRecreation() {
        ActivityScenario.launch(LanguageActivity::class.java).use { scenario ->
            onView(withText("Tiếng Việt")).perform(click())
            waitForPreview(scenario, "Lưu")
            assertEquals("en", LanguageHelper.getSavedLocale(context))
            assertEquals("vi", LanguageHelper.getEffectiveLocale(context))

            scenario.recreate()
            waitForPreview(scenario, "Lưu")
            scenario.onActivity { activity ->
                val list = activity.findViewById<RecyclerView>(R.id.rc_languages)
                val adapter = list.adapter as LanguageAdapter
                assertEquals("vi", adapter.currentList[adapter.currentPositionSelected].code)
            }
            assertEquals("en", LanguageHelper.getSavedLocale(context))
        }
        assertEquals("en", LanguageHelper.getEffectiveLocale(context))
        assertEquals("en", LanguageHelper.attachBaseContextWithLocale(context).resources.configuration.locales[0].language)
    }

    @Test fun saveOpensTheIntroInTheChosenLanguageAndKeepsItForTheNextScreen() {
        val monitor = ActivityMonitor(IntroActivity::class.java.name, null, false)
        instrumentation.addMonitor(monitor)
        try {
            ActivityScenario.launch(LanguageActivity::class.java).use { scenario ->
                onView(withText("Tiếng Việt")).perform(click())
                waitForPreview(scenario, "Lưu")
                onView(withId(R.id.btn_save_language)).perform(click())
                val intro = requireNotNull(monitor.waitForActivityWithTimeout(5_000))
                try {
                    assertEquals("vi", LanguageHelper.getSavedLocale(context))
                    instrumentation.runOnMainSync {
                        assertEquals("vi", intro.resources.configuration.locales[0].language)
                        assertEquals("Tiếp theo", intro.getString(R.string.next))
                    }
                    LanguageHelper.clearTemporaryLocale()
                    val nextContext = LanguageHelper.attachBaseContextWithLocale(context)
                    assertEquals("vi", nextContext.resources.configuration.locales[0].language)
                } finally {
                    instrumentation.runOnMainSync { intro.finish() }
                }
            }
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    @Test fun cancellingAPreviewRestoresTheSavedLanguage() {
        ActivityScenario.launch(LanguageActivity::class.java).use { scenario ->
            onView(withText("Français")).perform(click())
            waitForPreview(scenario, "Enregistrer")
            scenario.onActivity { it.backDevice() }
        }
        assertEquals("en", LanguageHelper.getSavedLocale(context))
        assertEquals("en", LanguageHelper.getEffectiveLocale(context))
    }

    private fun waitForPreview(scenario: ActivityScenario<LanguageActivity>, saveLabel: String) {
        val deadline = SystemClock.uptimeMillis() + 5_000
        var ready = false
        while (!ready && SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity { activity ->
                val save = activity.findViewById<TextView>(R.id.btn_save_language)
                ready = save.isEnabled && save.text.toString() == saveLabel
            }
            if (!ready) SystemClock.sleep(25)
        }
        assertTrue("The language preview did not finish", ready)
    }
}
