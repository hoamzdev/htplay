package com.example.movie


import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.domain.common.Result
import com.example.movie.adapter.HomeAdapter
import com.example.movie.adapter.formatTime
import com.example.movie.base.BaseActivity
import com.example.movie.databinding.ActivityMainBinding
import com.example.movie.entities.HomeItem
import com.example.movie.mock.MockHomeData
import com.example.movie.util.launchRepeatOnStarted
import com.example.movie.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest


@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private val viewModel: MainViewModel by viewModels()

    private val homeAdapter = HomeAdapter(
        onMovieClick = { showMockMessage(it.nameFile.orEmpty()) },
        onContinueClick = {
            showMockMessage("${formatTime(it.currentProgress ?: 0)} / ${formatTime(it.totalProgress ?: 0)}")
        },
        onComingSoonClick = { showMockMessage("${it.name} - ${it.date}") },
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
        homeAdapter.submitList(MockHomeData.homeItems)
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
                        showToastApp("Loading...")
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
                    section.movies.map { "${formatTime(it.currentProgress ?: 0)} / ${formatTime(it.totalProgress ?: 0)}" }

            is HomeItem.ComingSoonSector -> getString(R.string.coming_soon) to
                    section.movies.map { "${it.name.orEmpty()} - ${it.date.orEmpty()}" }

            is HomeItem.MovieSector -> section.genre to section.movies.map { it.nameFile.orEmpty() }
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(titles.toTypedArray()) { _, position -> showMockMessage(titles[position]) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

}
