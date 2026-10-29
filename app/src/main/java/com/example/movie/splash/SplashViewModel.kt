package com.example.movie.splash

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.movie.R

@HiltViewModel
class SplashViewModel @Inject constructor() : ViewModel(){
    fun getListSplashAnim() : List<Int>{
        return listOf(
            R.drawable.img_spl,
            R.drawable.img_1_spl,
            R.drawable.img_2_spl,
            R.drawable.img_3_spl,
            R.drawable.img_4_spl,
            R.drawable.img_5_spl,
            R.drawable.img_6_spl,
            R.drawable.img_7_spl,
        )
    }
}