package com.htdev.movie.htplay


import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.adapter.HomeAdapter
import com.htdev.movie.htplay.adapter.formatTime
import com.htdev.movie.htplay.base.BaseActivity
import com.htdev.movie.htplay.databinding.ActivityMainBinding
import com.htdev.movie.htplay.entities.HomeItem
import com.htdev.movie.htplay.mock.MockHomeData
import com.htdev.movie.htplay.util.launchRepeatOnStarted
import com.htdev.movie.htplay.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest


@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private val viewModel: MainViewModel by viewModels()

    private val homeAdapter = HomeAdapter(
        onMovieClick = { showMockMessage(it.nameFile.orEmpty()) },
        onContinueClick = {
            showMockMessage(getString(R.string.playback_progress_spaced, formatTime(it.currentProgress ?: 0), formatTime(it.totalProgress ?: 0)))
        },
        onComingSoonClick = { showMockMessage(getString(R.string.movie_name_with_date, it.name.orEmpty(), it.date.orEmpty())) },
        onViewAllClick = { showAll(it) },
    )

    override fun createViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun initView() {
        binding.rcMain.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = homeAdapter
        }
        homeAdapter.submitList(MockHomeData(this).homeItems)
    }

    override fun observerData() {
        launchRepeatOnStarted {
            viewModel.movies.collectLatest { status ->
                when (status) {
                    is Result.Success -> {
                        val movies = status.data
                        loge(movies.toString())
                    }

                    is Result.Error -> {
                        val error = status.throwable.message.toString()
                        showToastApp(error)
                        loge(error)
                    }

                    is Result.Loading -> {
                        showToastApp(getString(R.string.loading))
                    }

                    else -> Unit
                }
            }
        }

    }

    private fun showMockMessage(message: String) {
        Toast.makeText(this, getString(R.string.mock_selection, message), Toast.LENGTH_SHORT).show()
    }

    private fun showAll(section: HomeItem) {
        val (title, titles) = when (section) {
            is HomeItem.ContinueWatchingSector -> getString(R.string.continue_watching) to
                    section.movies.map { getString(R.string.playback_progress_spaced, formatTime(it.currentProgress ?: 0), formatTime(it.totalProgress ?: 0)) }

            is HomeItem.ComingSoonSector -> getString(R.string.coming_soon) to
                    section.movies.map { getString(R.string.movie_name_with_date, it.name.orEmpty(), it.date.orEmpty()) }

            is HomeItem.MovieSector -> section.genre to section.movies.map { it.nameFile.orEmpty() }
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(titles.toTypedArray()) { _, position -> showMockMessage(titles[position]) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

}
