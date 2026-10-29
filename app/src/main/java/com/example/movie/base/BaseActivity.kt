package com.example.movie.base

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {
    protected lateinit var binding: VB
    abstract fun createViewBinding() : VB

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

    open fun newActivity(dist : Activity, bundle: Bundle? = null) {
        val intent = Intent(this, dist::class.java)
        intent.apply {
            putExtras(bundle ?: Bundle())
        }
        startActivity(intent)
    }

    open fun backDevice() {
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
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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