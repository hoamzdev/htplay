package com.htdev.movie.htplay.home.favorite

import com.htdev.movie.htplay.base.BaseFragment
import com.htdev.movie.htplay.databinding.FavoriteFragmentBinding

class FavoriteFragment : BaseFragment<FavoriteFragmentBinding>() {
    override fun createViewBinding(): FavoriteFragmentBinding {
        return FavoriteFragmentBinding.inflate(layoutInflater)
    }



}