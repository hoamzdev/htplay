package com.htdev.movie.htplay.home.favorite

import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.base.BaseFragment
import com.htdev.movie.htplay.databinding.FavoriteFragmentBinding
import com.htdev.movie.htplay.detail.DetailActivity
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.util.launchRepeatOnStarted
import com.htdev.movie.htplay.widget.gone
import com.htdev.movie.htplay.widget.onClickListener
import com.htdev.movie.htplay.widget.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest


@AndroidEntryPoint
class FavoriteFragment : BaseFragment<FavoriteFragmentBinding>() {
    private val favoriteMovieViewModel: FavoriteMovieViewModel by viewModels()
    private val favoriteMovieAdapter: FavoriteMovieAdapter by lazy {
        FavoriteMovieAdapter(
            onClickMovie = { slug ->
                handleClickMovie(slug)
            },
            onDeleteFavorite = { slug ->
                handleDeleteFavorite(slug)
            }
        )
    }

    override fun createViewBinding(): FavoriteFragmentBinding {
        return FavoriteFragmentBinding.inflate(layoutInflater)
    }


    override fun initView() {
        binding.rcFavorites.apply {
            adapter = favoriteMovieAdapter
            setHasFixedSize(true)
        }
    }

    override fun bindViewModel() {
        launchRepeatOnStarted {
            favoriteMovieViewModel.favorites.collectLatest { state ->
                when (state) {
                    is Result.Loading -> {
                        binding.pgLoading.visible()
                    }

                    is Result.Success -> {
                        binding.pgLoading.gone()
                        val favorites = state.data
                        favoriteMovieAdapter.submitList(favorites)
                        if (favorites.isEmpty()) {
                            binding.apply {
                                rcFavorites.gone()
                                llEmpty.visible()
                            }
                        } else {
                            binding.apply {
                                rcFavorites.visible()
                                llEmpty.gone()
                            }
                        }
                    }

                    else -> Unit
                }
            }
        }
    }

    private fun handleDeleteFavorite(slug: String) {
        favoriteMovieViewModel.deleteFavorite(slug)
    }


    private fun handleClickMovie(slug: String) {
        val intent = DetailActivity.createIntent(requireContext(), movieSlug = slug)
        startActivity(intent)
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
            btnWatchNow.onClickListener {
                findNavController().navigate(R.id.homeFragment)
            }
        }
    }
}
