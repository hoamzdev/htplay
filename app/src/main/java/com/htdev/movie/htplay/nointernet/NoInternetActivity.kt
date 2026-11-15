package com.htdev.movie.htplay.nointernet

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.base.BaseActivity
import com.htdev.movie.htplay.databinding.ActivityNoInternetBinding
import com.htdev.movie.htplay.home.HomeRouterActivity
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
        finish()
    }

}