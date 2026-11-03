package com.htdev.movie.htplay.home.home

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.adapter.HomeGenreMovieAdapter
import com.htdev.movie.htplay.adapter.HomeGenreStatusAdapter
import com.htdev.movie.htplay.adapter.ShowAllCountryAdapter
import com.htdev.movie.htplay.base.BaseActivity
import com.htdev.movie.htplay.databinding.ActivityShowAllMovieBinding
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.entities.Country
import com.htdev.movie.htplay.domain.entities.Genre
import com.htdev.movie.htplay.entities.HomeViewAllTarget
import com.htdev.movie.htplay.home.home.viewmodel.ShowAllViewModel
import com.htdev.movie.htplay.util.getCountryName
import com.htdev.movie.htplay.util.getGenreName
import com.htdev.movie.htplay.util.launchRepeatOnStarted
import com.htdev.movie.htplay.widget.HomeGenreTabsController
import com.htdev.movie.htplay.widget.onClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ShowAllMovieActivity : BaseActivity<ActivityShowAllMovieBinding>() {
    private val viewModel: ShowAllViewModel by viewModels()
    private val sector by lazy { intent.getIntExtra(KEY_SECTOR, SECTOR_CONTINUE) }
    private val target: HomeViewAllTarget? by lazy {
        when (sector) {
            SECTOR_CONTINUE -> HomeViewAllTarget.ContinueWatching
            SECTOR_MOVIE_LATEST -> HomeViewAllTarget.Latest
            SECTOR_MOVIE_RECOMMENDED -> HomeViewAllTarget.Recommended(
                intent.getStringArrayListExtra(KEY_DATA).orEmpty(),
            )
            SECTOR_MOVIE_CHINESE -> HomeViewAllTarget.ByCountry("trung-quoc")
            SECTOR_MOVIE_KOREAN -> HomeViewAllTarget.ByCountry("han-quoc")
            SECTOR_MOVIE_ACTION -> HomeViewAllTarget.ByGenre("hanh-dong")
            SECTOR_MOVIE_ROMANCE -> HomeViewAllTarget.ByGenre("tinh-cam")
            SECTOR_MOVIE_FAMILY -> HomeViewAllTarget.ByGenre("gia-dinh")
            SECTOR_MOVIE_COUNTRIES -> HomeViewAllTarget.Countries
            SECTOR_MOVIE_BY_COUNTRY -> intent.getStringExtra(KEY_DATA)
                ?.takeIf { it.isNotBlank() }?.let { HomeViewAllTarget.ByCountry(it) }
            else -> null
        }
    }
    private var movieAdapter: HomeGenreMovieAdapter? = null
    private var displayedTarget: HomeViewAllTarget? = null
    private var tabsController: HomeGenreTabsController? = null
    private val countryAdapter = ShowAllCountryAdapter { country ->
        startActivity(countryIntent(this, country))
    }
    private val statusAdapter = HomeGenreStatusAdapter {
        if (target == HomeViewAllTarget.Countries) viewModel.fetchCountries()
        else movieAdapter?.retry()
    }
    private val mainAdapter = ConcatAdapter(statusAdapter)

    override fun createViewBinding() = ActivityShowAllMovieBinding.inflate(layoutInflater)

    override fun initView() {
        val source = target ?: run { finish(); return }
        binding.tvTitle.text = when (source) {
            HomeViewAllTarget.ContinueWatching -> getString(R.string.continue_watching)
            HomeViewAllTarget.Latest -> getString(R.string.home_latest_movies)
            is HomeViewAllTarget.Recommended -> getString(R.string.home_selected_for_you)
            is HomeViewAllTarget.ByGenre -> getGenreName(source.slug)
            is HomeViewAllTarget.ByCountry -> when (sector) {
                SECTOR_MOVIE_CHINESE -> getString(R.string.home_chinese_movies)
                SECTOR_MOVIE_KOREAN -> getString(R.string.home_korean_movies)
                else -> getCountryName(source.slug).takeUnless { it == source.slug }
                    ?: intent.getStringExtra(KEY_COUNTRY_NAME) ?: source.slug
            }
            HomeViewAllTarget.Countries -> getString(R.string.home_countries)
        }
        // Continue Watching is implemented separately.
        if (source == HomeViewAllTarget.ContinueWatching) return
        viewModel.initialize(source)
        val columns = if (source == HomeViewAllTarget.Countries) 2 else 3
        binding.rcMovies.apply {
            layoutManager = GridLayoutManager(this@ShowAllMovieActivity, columns).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        val contentCount = if (source == HomeViewAllTarget.Countries) {
                            countryAdapter.itemCount
                        } else movieAdapter?.itemCount ?: 0
                        return if (position < contentCount) 1 else columns
                    }
                }
            }
            adapter = mainAdapter
            itemAnimator = null
        }
        if (source == HomeViewAllTarget.Countries) mainAdapter.addAdapter(0, countryAdapter)
        if (source is HomeViewAllTarget.Recommended) {
            binding.genreTabs.root.isVisible = viewModel.genreSlugs.isNotEmpty()
            tabsController = HomeGenreTabsController(
                binding = binding.genreTabs,
                onSelect = { slug ->
                    if (slug != null && slug != viewModel.selectedGenre.value) {
                        viewModel.selectGenre(slug)
                        binding.rcMovies.scrollToPosition(0)
                    }
                },
                onRetry = {},
                includePopular = false,
            )
            if (viewModel.genreSlugs.isEmpty()) statusAdapter.submit(R.string.home_no_recommendations)
        }
    }


    override fun handleClickEvent() {
        binding.apply {
            icBack.onClickListener {
                finish()
            }
        }
    }

    override fun observerData() {
        when (target) {
            null, HomeViewAllTarget.ContinueWatching -> Unit
            HomeViewAllTarget.Countries -> observeCountries()
            else -> {
                observeMovies()
                if (target is HomeViewAllTarget.Recommended) {
                    launchRepeatOnStarted {
                        viewModel.selectedGenre.collectLatest { slug ->
                            val genres = viewModel.genreSlugs.map { Genre(getGenreName(it), it) }
                            tabsController?.submit(Result.Success(genres), slug)
                        }
                    }
                }
            }
        }
    }

    private fun observeMovies() = launchRepeatOnStarted {
        viewModel.movies.collectLatest { page ->
            if (!viewModel.isCurrentPage(page.target)) return@collectLatest
            if (displayedTarget != page.target) {
                movieAdapter?.let(mainAdapter::removeAdapter)
                movieAdapter = HomeGenreMovieAdapter(onClick = { /* Navigate to movie detail. */ })
                displayedTarget = page.target
                statusAdapter.submit(R.string.loading)
                mainAdapter.addAdapter(0, movieAdapter!!)
            }
            val adapter = movieAdapter ?: return@collectLatest
            coroutineScope {
                launch {
                    adapter.loadStateFlow.collect { states ->
                        if (states == null || !viewModel.isCurrentPage(page.target)) return@collect
                        val error = states.refresh is LoadState.Error || states.append is LoadState.Error
                        val loading = states.refresh is LoadState.Loading || states.append is LoadState.Loading
                        val empty = adapter.itemCount == 0 && states.refresh is LoadState.NotLoading &&
                            states.append.endOfPaginationReached
                        statusAdapter.submit(when {
                            error -> R.string.home_load_error_retry
                            loading -> R.string.loading
                            empty -> R.string.home_no_movies
                            else -> null
                        }, canRetry = error)
                    }
                }
                adapter.submitData(page.movies)
            }
        }
    }

    private fun observeCountries() = launchRepeatOnStarted {
        viewModel.countries.collectLatest { result ->
            val countries = (result as? Result.Success)?.data.orEmpty().map { country ->
                val name = getCountryName(country.slug)
                country.copy(name = name.takeUnless { it == country.slug } ?: country.name)
            }
            countryAdapter.submitList(countries)
            statusAdapter.submit(when (result) {
                is Result.Loading, is Result.Idle -> R.string.loading
                is Result.Error -> R.string.home_load_error_retry
                is Result.Success -> if (countries.isEmpty()) R.string.home_no_categories else null
            }, canRetry = result is Result.Error)
        }
    }

    override fun onDestroy() {
        binding.rcMovies.adapter = null
        super.onDestroy()
    }

    companion object {
        const val KEY_SECTOR = "show_all"
        const val KEY_DATA = "data_receiver"
        private const val KEY_COUNTRY_NAME = "country_name"
        const val SECTOR_CONTINUE = -1
        const val SECTOR_MOVIE_LATEST = 0
        const val SECTOR_MOVIE_RECOMMENDED = 1
        const val SECTOR_MOVIE_CHINESE = 2
        const val SECTOR_MOVIE_KOREAN = 3
        const val SECTOR_MOVIE_ACTION = 4
        const val SECTOR_MOVIE_ROMANCE = 5
        const val SECTOR_MOVIE_FAMILY = 6
        const val SECTOR_MOVIE_COUNTRIES = 7
        const val SECTOR_MOVIE_BY_COUNTRY = 8

        fun countryIntent(context: Context, country: Country) =
            Intent(context, ShowAllMovieActivity::class.java).apply {
                putExtra(KEY_SECTOR, SECTOR_MOVIE_BY_COUNTRY)
                putExtra(KEY_DATA, country.slug)
                putExtra(KEY_COUNTRY_NAME, country.name)
            }
    }
}
