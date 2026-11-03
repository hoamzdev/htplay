package com.htdev.movie.htplay.favorite

import androidx.activity.viewModels
import com.htdev.movie.htplay.MainActivity
import com.htdev.movie.htplay.base.AuthenticatedActivity
import com.htdev.movie.htplay.data.db.LocalMovieCatalog
import com.htdev.movie.htplay.databinding.ActivityChooseFavotireBinding
import com.htdev.movie.htplay.widget.onClickListener
import dagger.hilt.android.AndroidEntryPoint
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.util.getLocalizedGenres
import com.htdev.movie.htplay.util.launchRepeatOnStarted
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class ChooseFavoritesActivity : AuthenticatedActivity<ActivityChooseFavotireBinding>() {
    override fun createViewBinding(): ActivityChooseFavotireBinding {
        return ActivityChooseFavotireBinding.inflate(layoutInflater)
    }

    private val chooseFavoriteAdapter: ChooseFavoriteAdapter by lazy {
        ChooseFavoriteAdapter()
    }

    private val chooseFavoriteViewModel : FavoriteViewModel by viewModels()

    override fun initView() {
        chooseFavoriteAdapter.submitList(LocalMovieCatalog.getLocalizedGenres(this).map { it ->
            ChooseFavoriteItem(
                nameGenre = it.name,
                urlPoster = it.poster,
                slug = it.slug,
            )
        })
        binding.rcFavorites.apply {
            adapter = chooseFavoriteAdapter
        }
    }

    override fun handleClickEvent() {
        binding.apply {
            btnChoose.onClickListener {
                val Genres = chooseFavoriteAdapter.getSelectedGenres()
                if(Genres.isEmpty()){
                    showToastApp(getString(R.string.favorite_choose_at_least_one))
                }
                else{
                    chooseFavoriteViewModel.saveGenresFavorite(Genres)
                }
            }
        }
    }

    override fun observerData() {
        launchRepeatOnStarted {
            chooseFavoriteViewModel.saveResult.collectLatest { result ->
                binding.btnChoose.isEnabled = result !is Result.Loading
                when (result) {
                    is Result.Success -> {
                        if (result.data) {
                            chooseFavoriteViewModel.clearSaveResult()
                            newActivity(MainActivity::class.java)
                            finish()
                        } else {
                            showToastApp(getString(R.string.favorite_save_error))
                            chooseFavoriteViewModel.clearSaveResult()
                        }
                    }
                    is Result.Error -> {
                        showToastApp(getString(R.string.favorite_save_error))
                        chooseFavoriteViewModel.clearSaveResult()
                    }
                    else -> Unit
                }
            }
        }
    }
}
