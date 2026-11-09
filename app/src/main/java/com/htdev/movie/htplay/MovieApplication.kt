package com.htdev.movie.htplay

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import com.htdev.movie.htplay.language.LanguageHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MovieApplication : Application(), Application.ActivityLifecycleCallbacks,
    DefaultLifecycleObserver {

    override fun onCreate() {
        super<Application>.onCreate()

        LanguageHelper.clearLegacyFrameworkLocale(this)
        LanguageHelper.refreshApplicationLanguage(this)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LanguageHelper.attachBaseContextWithLocale(base))
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
