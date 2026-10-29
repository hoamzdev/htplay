package com.example.movie.intro

import com.example.movie.base.BaseActivity
import com.example.movie.databinding.ActivityIntroBinding

class IntroActivity : BaseActivity<ActivityIntroBinding>() {

    override fun createViewBinding(): ActivityIntroBinding {
        return ActivityIntroBinding.inflate(layoutInflater)
    }



}