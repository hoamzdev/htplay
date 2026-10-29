package com.example.movie

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MovieApplication : Application(), Application.ActivityLifecycleCallbacks,
    DefaultLifecycleObserver {

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
/*

{
  "primary": "#7A1E1E",
  "secondary": "#D96666",
  "background": "#1A1A1A",
  "gray": "#8C8C8C",
  "white": "#FFFFFF",
  "success": "#67CF7A"
}

 */