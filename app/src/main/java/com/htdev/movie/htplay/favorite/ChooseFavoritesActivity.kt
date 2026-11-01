package com.htdev.movie.htplay.favorite

import androidx.activity.viewModels
import com.htdev.movie.htplay.MainActivity
import com.htdev.movie.htplay.base.AuthenticatedActivity
import com.htdev.movie.htplay.data.db.DataGenre
import com.htdev.movie.htplay.databinding.ActivityChooseFavotireBinding
import com.htdev.movie.htplay.widget.onClickListener
import dagger.hilt.android.AndroidEntryPoint

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
        chooseFavoriteAdapter.submitList(DataGenre.genres.map { it ->
            ChooseFavoriteItem(
                nameGenre = it.name,
                urlPoster = it.poster
            )
        })
        binding.rcFavorites.apply {
            adapter = chooseFavoriteAdapter
        }
    }

    override fun handleClickEvent() {
        binding.apply {
            btnChoose.onClickListener {
                val genres = chooseFavoriteAdapter.getSelectedGenres()
                if(genres.isEmpty()){
                    showToastApp("Please choose at least one genre")
                }
                else{
                    chooseFavoriteViewModel.saveGenresFavorite(genres)
                    newActivity(MainActivity::class.java)
                    finish()
                }
            }
        }
    }

    override fun observerData() {

    }
}
