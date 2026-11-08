package com.htdev.movie.htplay.watching

import com.htdev.movie.htplay.base.BaseActivity
import com.htdev.movie.htplay.databinding.ActivityWatchingBinding

class WatchingActivity : BaseActivity<ActivityWatchingBinding>(){
    override fun createViewBinding(): ActivityWatchingBinding {
        return ActivityWatchingBinding.inflate(layoutInflater)
    }



}