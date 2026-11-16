package com.htdev.movie.htplay.base

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.viewbinding.ViewBinding
import com.htdev.movie.htplay.widget.CustomToast
import com.htdev.movie.htplay.language.LanguageHelper
import com.htdev.movie.htplay.MovieApplication

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {
    protected lateinit var binding: VB
    abstract fun createViewBinding() : VB

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHelper.attachBaseContextWithLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        enableEdgeToEdge()
        binding = createViewBinding()
        setContentView(binding.root)
        init()
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backDevice()
            }
        })
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        (application as MovieApplication).miniPlayer.onUserLeaveHint(this)
    }

    override fun startActivity(intent: Intent, options: Bundle?) {
        // Internal navigation keeps the in-app mini player instead of triggering legacy system PiP.
        if (intent.component?.packageName == packageName) intent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        super.startActivity(intent, options)
    }

    override fun onPictureInPictureModeChanged(inPip: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(inPip, newConfig)
        (application as MovieApplication).miniPlayer.onSystemPipChanged(this, inPip)
    }

    open fun newActivity(dist : Class<*>, bundle: Bundle? = null) {
        val intent = Intent(this, dist)
        intent.apply {
            putExtras(bundle ?: Bundle())
        }
        startActivity(intent)
    }

    open fun backDevice() {
        if (isTaskRoot && (application as MovieApplication).miniPlayer.enterSystemPip(this)) return
        finish()
    }

    open fun onFinish(){
        finish()
    }

    @OptIn(UnstableApi::class)
    open fun loge(message : String){
        Log.e("htdev", message)
    }

    @OptIn(UnstableApi::class)
    open fun logd(message : String){
        Log.e("htdev", message)
    }

    open fun showToastApp(message : String){
        CustomToast.showToast(this, message)
    }

    fun init() {
        initView()
        initViewModel()
        handleClickEvent()
        observerData()
    }

    open fun initView() {}
    open fun initViewModel() {}
    open fun handleClickEvent() {}
    open fun observerData() {}
}
