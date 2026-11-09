package com.htdev.movie.htplay.language

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import java.util.Locale

/** Keeps previews in memory; only Save commits the language, as in INV184. */
object LanguageHelper {
    private const val PREFERENCES = "MovieSharedPref"
    private const val KEY_LANGUAGE = "KEY_LANGUAGE"
    private const val DEFAULT_LANGUAGE = "en"
    private var pendingLocale: String? = null

    fun getSavedLocale(context: Context): String =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, DEFAULT_LANGUAGE)
            ?.takeIf { it.isNotBlank() } ?: DEFAULT_LANGUAGE

    fun getEffectiveLocale(context: Context): String = pendingLocale ?: getSavedLocale(context)

    fun applyLocaleTemporarily(language: String) {
        pendingLocale = language
    }

    fun clearTemporaryLocale() {
        pendingLocale = null
    }

    fun saveLocale(context: Context, language: String) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit {
            putString(KEY_LANGUAGE, language)
        }
        clearTemporaryLocale()
        refreshApplicationLanguage(context)
    }

    fun localizedContext(context: Context, language: String): Context {
        val locale = Locale.forLanguageTag(language.replace('_', '-'))
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return context.createConfigurationContext(configuration)
    }

    fun attachBaseContextWithLocale(context: Context): Context {
        val language = getEffectiveLocale(context)
        Locale.setDefault(Locale.forLanguageTag(language.replace('_', '-')))
        return localizedContext(context, language)
    }

    // Older builds persisted an English override through LocaleManager. The saved app
    // preference and wrapped contexts now own the locale, just as they do in INV184.
    fun clearLegacyFrameworkLocale(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            if (!localeManager.applicationLocales.isEmpty) {
                localeManager.applicationLocales = LocaleList.getEmptyLocaleList()
            }
        }
    }

    @Suppress("DEPRECATION")
    fun refreshApplicationLanguage(context: Context) {
        val application = context.applicationContext
        val locale = Locale.forLanguageTag(getSavedLocale(context).replace('_', '-'))
        Locale.setDefault(locale)
        val configuration = Configuration(application.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        application.resources.updateConfiguration(configuration, application.resources.displayMetrics)
    }
}
