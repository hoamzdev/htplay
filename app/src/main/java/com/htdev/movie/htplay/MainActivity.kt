package com.htdev.movie.htplay

import com.htdev.movie.htplay.base.AuthenticatedActivity
import com.htdev.movie.htplay.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AuthenticatedActivity<ActivityMainBinding>() {
    override fun createViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }
}
