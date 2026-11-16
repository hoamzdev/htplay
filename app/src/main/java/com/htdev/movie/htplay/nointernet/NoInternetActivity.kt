package com.htdev.movie.htplay.nointernet

import android.content.Intent
import android.content.Context
import android.provider.Settings
import com.htdev.movie.htplay.base.BaseActivity
import com.htdev.movie.htplay.databinding.ActivityNoInternetBinding
import com.htdev.movie.htplay.splash.SplashActivity
import com.htdev.movie.htplay.widget.checkNetworkConnected

class NoInternetActivity : BaseActivity<ActivityNoInternetBinding>(){
    override fun createViewBinding(): ActivityNoInternetBinding {
        return ActivityNoInternetBinding.inflate(layoutInflater)
    }

    override fun handleClickEvent() {
        binding.btnTryAgain.setOnClickListener {
            if(checkNetworkConnected(this)){
                //neu co mang
                refreshApp()
            }
            else{
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                startActivity(intent)
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        if(checkNetworkConnected(this)) refreshApp()
    }

    private fun refreshApp(){
        if (isFinishing) return
        if (intent.getBooleanExtra(EXTRA_FROM_STARTUP, false)) {
            startActivity(Intent(this, SplashActivity::class.java))
        }
        finish()
    }

    companion object {
        private const val EXTRA_FROM_STARTUP = "no_internet_from_startup"

        fun createStartupIntent(context: Context): Intent =
            Intent(context, NoInternetActivity::class.java).putExtra(EXTRA_FROM_STARTUP, true)
    }

}
