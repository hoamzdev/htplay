package com.htdev.movie.htplay

import android.app.Activity
import android.app.Application
import android.app.LocaleManager
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.DefaultLifecycleObserver
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MovieApplication : Application(), Application.ActivityLifecycleCallbacks,
    DefaultLifecycleObserver {

    override fun onCreate() {
        super<Application>.onCreate()

        // Apply English before any screen is created, regardless of the device language.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags("en")
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        }
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?
    ) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle
    ) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit
}
