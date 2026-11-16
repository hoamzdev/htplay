package com.htdev.movie.htplay

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.htdev.movie.htplay.home.home.HomeRecommendationSession
import com.htdev.movie.htplay.player.InAppMiniPlayer
import javax.inject.Inject
import com.htdev.movie.htplay.language.LanguageHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MovieApplication : Application(), Application.ActivityLifecycleCallbacks,
    DefaultLifecycleObserver {
    @Inject lateinit var recommendationSession: HomeRecommendationSession
    @Inject lateinit var miniPlayer: InAppMiniPlayer
    private var hasEnteredForeground = false

    override fun onCreate() {
        super<Application>.onCreate()

        LanguageHelper.clearLegacyFrameworkLocale(this)
        LanguageHelper.refreshApplicationLanguage(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        registerActivityLifecycleCallbacks(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        miniPlayer.onAppForeground()
        if (hasEnteredForeground) recommendationSession.startNewSession()
        hasEnteredForeground = true
    }

    override fun onStop(owner: LifecycleOwner) {
        miniPlayer.onAppBackground()
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LanguageHelper.attachBaseContextWithLocale(base))
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?
    ) = Unit

    override fun onActivityDestroyed(activity: Activity) = miniPlayer.onActivityStopped(activity)

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityResumed(activity: Activity) = miniPlayer.attach(activity)

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle
    ) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = miniPlayer.onActivityStopped(activity)
}
