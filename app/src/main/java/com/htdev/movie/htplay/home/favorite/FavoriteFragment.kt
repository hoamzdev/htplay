package com.htdev.movie.htplay.home.favorite

import com.htdev.movie.htplay.base.BaseFragment
import com.htdev.movie.htplay.databinding.FavoriteFragmentBinding
import com.htdev.movie.htplay.widget.gone
import com.htdev.movie.htplay.widget.onClickListener
import com.htdev.movie.htplay.widget.visible

class FavoriteFragment : BaseFragment<FavoriteFragmentBinding>() {
    override fun createViewBinding(): FavoriteFragmentBinding {
        return FavoriteFragmentBinding.inflate(layoutInflater)
    }

    override fun handleClickEvent() {
        binding.apply {
            ivSearch.onClickListener {
                tvFavorites.gone()
                edtSearch.visible()
            }
            logoApp.onClickListener {
                tvFavorites.visible()
                edtSearch.gone()
            }
        }
    }

}