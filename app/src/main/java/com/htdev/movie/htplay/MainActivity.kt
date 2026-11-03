package com.htdev.movie.htplay


import android.widget.Toast
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.paging.LoadState
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.adapter.HomeAdapter
import com.htdev.movie.htplay.widget.HomeGenreTabsController
import com.google.android.material.appbar.AppBarLayout
import com.htdev.movie.htplay.adapter.HomeGenreMovieAdapter
import com.htdev.movie.htplay.adapter.HomeGenreStatusAdapter
import com.htdev.movie.htplay.adapter.PosterHomeAdapter
import com.htdev.movie.htplay.adapter.formatTime
import com.htdev.movie.htplay.base.AuthenticatedActivity
import com.htdev.movie.htplay.databinding.ActivityMainBinding
import com.htdev.movie.htplay.entities.HomeItem
import com.htdev.movie.htplay.domain.entities.Country
import com.htdev.movie.htplay.domain.entities.Genre
import com.htdev.movie.htplay.domain.entities.PosterMovie
import com.htdev.movie.htplay.util.buildHomeSections
import com.htdev.movie.htplay.util.getGenreName
import com.htdev.movie.htplay.util.getCountryName
import com.htdev.movie.htplay.util.launchRepeatOnStarted
import com.htdev.movie.htplay.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch


@AndroidEntryPoint
class MainActivity : AuthenticatedActivity<ActivityMainBinding>() {

    private val viewModel: MainViewModel by viewModels()

    private val posterPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            updatePosterIndicator(position)
        }
    }

    private val posterHomeAdapter = PosterHomeAdapter(
        onMovieClick = { showMovieSelection(it.name) },
        onPlayClick = { showMovieSelection(it.name) },
    ).apply {
        stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    private val homeAdapter = HomeAdapter(
        onMovieClick = { showMovieSelection(it.nameFile.orEmpty()) },
        onContinueClick = {
            showMovieSelection(getString(R.string.playback_progress_spaced, formatTime(it.currentProgress ?: 0), formatTime(it.totalProgress ?: 0)))
        },
        onViewAllClick = { showAll(it) },
        onGenreClick = { showGenreMovies(it) },
        onCountryClick = { showCountryMovies(it) },
        onRetryClick = { viewModel.retryHomeSection(it.id) },
    )

    private val genreTabsController by lazy {
        HomeGenreTabsController(
            binding = binding.genreTabs,
            onSelect = { slug ->
                if (viewModel.selectedHomeGenre.value != slug) {
                    showHomeTab(slug)
                    viewModel.selectHomeGenre(slug)
                    binding.rcMain.scrollToPosition(0)
                }
            },
            onRetry = { viewModel.fetchGenres() },
        )
    }
    private var genreMovieAdapter: HomeGenreMovieAdapter? = null
    private var displayedHomeGenre: String? = null
    private val genreStatusAdapter = HomeGenreStatusAdapter { genreMovieAdapter?.retry() }
    private val mainAdapter = ConcatAdapter(homeAdapter)

    private val appBarOffsetListener = AppBarLayout.OnOffsetChangedListener { appBar, offset ->
        val progress = if (appBar.totalScrollRange > 0) {
            (-offset.toFloat() / appBar.totalScrollRange).coerceIn(0f, 1f)
        } else 0f
        // The remaining banner becomes the black background behind the search bar.
        binding.posterScrim.alpha = progress
        val searchAlpha = ((progress - 0.5f) / 0.4f).coerceIn(0f, 1f)
        binding.searchContainer.alpha = searchAlpha
        binding.searchContainer.isVisible = searchAlpha > 0f
    }

    override fun createViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun initView() {
        binding.appBar.addOnOffsetChangedListener(appBarOffsetListener)
        binding.vpPoster.adapter = posterHomeAdapter
        binding.vpPoster.registerOnPageChangeCallback(posterPageChangeCallback)
        binding.posterStatus.setOnClickListener {
            if (viewModel.moviesByPage.value is Result.Error) {
                viewModel.fetchMoviesByPage()
            }
        }
        binding.rcMain.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 3).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        var offset = 0
                        mainAdapter.adapters.forEach { child ->
                            if (position < offset + child.itemCount) {
                                return if (child === genreMovieAdapter) 1 else 3
                            }
                            offset += child.itemCount
                        }
                        return 3
                    }
                }
            }
            adapter = mainAdapter
        }
        binding.rcMain.itemAnimator = null
    }

    override fun observerData() {
        observePosterBanner()
        observeGenreTabs()
        launchRepeatOnStarted {
            combine(viewModel.movies, viewModel.homeData) { latest, home ->
                buildHomeSections(
                    latestMovies = latest,
                    home = home,
                    latestTitle = getString(R.string.home_latest_movies),
                    recommendedTitle = getString(R.string.home_selected_for_you),
                    chineseTitle = getString(R.string.home_chinese_movies),
                    koreanTitle = getString(R.string.home_korean_movies),
                    actionTitle = getString(R.string.GenreLocal_action),
                    romanceTitle = getString(R.string.GenreLocal_romance),
                    familyTitle = getString(R.string.GenreLocal_family),
                    localizeCountry = { country ->
                        val name = getCountryName(country.slug)
                        country.copy(name = name.takeUnless { it == country.slug } ?: country.name)
                    },
                )
            }.collectLatest { sections ->
                homeAdapter.submitList(sections)
            }
        }

    }

    private fun observeGenreTabs() {
        launchRepeatOnStarted {
            combine(viewModel.genres, viewModel.selectedHomeGenre) { genres, slug -> genres to slug }
                .collectLatest { (genres, slug) ->
                    val localized = if (genres is Result.Success) Result.Success(genres.data.map { genre ->
                        val name = getGenreName(genre.slug)
                        genre.copy(name = name.takeUnless { it == genre.slug } ?: genre.name)
                    }) else genres
                    genreTabsController.submit(localized, slug)
                    showHomeTab(slug)
                }
        }
        launchRepeatOnStarted {
            viewModel.homeGenreMovies.collectLatest { page ->
                val slug = page.genreSlug ?: return@collectLatest
                if (slug != viewModel.selectedHomeGenre.value) return@collectLatest
                showHomeTab(slug)
                val moviesAdapter = genreMovieAdapter ?: return@collectLatest
                coroutineScope {
                    launch {
                        moviesAdapter.loadStateFlow.collect { states ->
                            if (states != null && moviesAdapter === genreMovieAdapter &&
                                slug == viewModel.selectedHomeGenre.value) {
                                val refreshing = states.refresh is LoadState.Loading
                                if (states.refresh is LoadState.NotLoading && moviesAdapter.itemCount > 0 &&
                                    moviesAdapter !in mainAdapter.adapters) {
                                    mainAdapter.addAdapter(0, moviesAdapter)
                                }
                                val loading = refreshing || states.append is LoadState.Loading
                                val error = states.refresh is LoadState.Error || states.append is LoadState.Error
                                val empty = moviesAdapter.itemCount == 0 &&
                                        states.refresh is LoadState.NotLoading && states.append.endOfPaginationReached
                                genreStatusAdapter.submit(when {
                                    error -> R.string.home_load_error_retry
                                    loading -> R.string.home_genre_loading
                                    empty -> R.string.home_no_movies
                                    else -> null
                                }, canRetry = error)
                            }
                        }
                    }
                    moviesAdapter.submitData(page.movies)
                }
            }
        }
    }

    private fun showHomeTab(slug: String?) {
        if (displayedHomeGenre == slug) return
        displayedHomeGenre = slug
        // Detach old cards synchronously with the tap, before the new pager starts.
        genreMovieAdapter?.let { mainAdapter.removeAdapter(it) }
        genreMovieAdapter = null
        if (slug == null) {
            mainAdapter.removeAdapter(genreStatusAdapter)
            if (homeAdapter !in mainAdapter.adapters) mainAdapter.addAdapter(homeAdapter)
        } else {
            mainAdapter.removeAdapter(homeAdapter)
            genreMovieAdapter = HomeGenreMovieAdapter { showMovieSelection(it.name) }
            genreStatusAdapter.submit(R.string.home_genre_loading)
            if (genreStatusAdapter !in mainAdapter.adapters) mainAdapter.addAdapter(genreStatusAdapter)
        }
    }

    private fun observePosterBanner() {
        launchRepeatOnStarted {
            viewModel.moviesByPage.collectLatest { result ->
                binding.apply {
                    posterPlaceholder.isVisible = result is Result.Loading || result is Result.Idle
                    vpPoster.isVisible = result is Result.Success && result.data.isNotEmpty()
                    posterStatus.isVisible = result is Result.Error ||
                        (result is Result.Success && result.data.isEmpty())
                    posterStatus.isClickable = result is Result.Error
                    posterStatus.isFocusable = result is Result.Error

                    when (result) {
                        is Result.Success -> {
                            posterHomeAdapter.submitList(result.data) {
                                updatePosterIndicator()
                            }
                            posterStatus.setText(R.string.home_no_movies)
                        }
                        is Result.Error -> posterStatus.setText(R.string.home_load_error_retry)
                        else -> Unit
                    }
                    updatePosterIndicator()
                }
            }
        }
    }

    private fun updatePosterIndicator(position: Int = binding.vpPoster.currentItem) {
        val count = posterHomeAdapter.itemCount
        binding.dotIndicator.apply {
            isVisible = binding.vpPoster.isVisible && count > 1
            if (count > 0) {
                if (childCount != count) setDotCount(count)
                positionSelected(position.coerceIn(0, count - 1) + 1)
            }
        }
    }

    override fun onDestroy() {
        binding.appBar.removeOnOffsetChangedListener(appBarOffsetListener)
        binding.vpPoster.unregisterOnPageChangeCallback(posterPageChangeCallback)
        super.onDestroy()
    }

    private fun showMovieSelection(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showAll(section: HomeItem) {
        val (title, titles) = when (section) {
            is HomeItem.ContinueWatchingSector -> getString(R.string.continue_watching) to
                    section.movies.map { getString(R.string.playback_progress_spaced, formatTime(it.currentProgress ?: 0), formatTime(it.totalProgress ?: 0)) }

            is HomeItem.ComingSoonSector -> getString(R.string.coming_soon) to
                    section.movies.map { getString(R.string.movie_name_with_date, it.name.orEmpty(), it.date.orEmpty()) }

            is HomeItem.MovieSector -> section.Genre to section.movies.map { it.nameFile.orEmpty() }
            is HomeItem.GenreSector -> getString(R.string.home_GenreLocals) to section.Genres.map { it.name }
            is HomeItem.CountrySector -> getString(R.string.home_countries) to section.countries.map { it.name }
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(titles.toTypedArray()) { _, position ->
                when (section) {
                    is HomeItem.GenreSector -> showGenreMovies(section.Genres[position])
                    is HomeItem.CountrySector -> showCountryMovies(section.countries[position])
                    else -> showMovieSelection(titles[position])
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showGenreMovies(Genre: Genre) {
        showFilteredMovies(Genre.name, viewModel.moviesByGenre) {
            viewModel.fetchMoviesByGenre(Genre.slug)
        }
    }

    private fun showCountryMovies(country: Country) {
        showFilteredMovies(country.name, viewModel.moviesByCountry) {
            viewModel.fetchMoviesByCountry(country.slug)
        }
    }

    private fun showFilteredMovies(
        title: String,
        movies: StateFlow<Result<List<PosterMovie>>>,
        fetch: () -> Unit,
    ) {
        fetch()
        val rows = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mutableListOf())
        var current: Result<List<PosterMovie>> = Result.Loading
        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setAdapter(rows, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        // Dung listener cua ListView de bam thu lai khong dong dialog.
        dialog.show()
        dialog.listView.setOnItemClickListener { _, _, position, _ ->
            when (val result = current) {
                is Result.Success -> result.data.getOrNull(position)?.let { showMovieSelection(it.name) }
                is Result.Error -> fetch()
                else -> Unit
            }
        }
        val collection = launchRepeatOnStarted {
            movies.collectLatest { result ->
                current = result
                rows.clear()
                rows.addAll(when (result) {
                    is Result.Success -> result.data.map { it.name }.ifEmpty { listOf(getString(R.string.home_no_movies)) }
                    is Result.Error -> listOf(getString(R.string.home_load_error_retry))
                    else -> listOf(getString(R.string.loading))
                })
                rows.notifyDataSetChanged()
            }
        }
        dialog.setOnDismissListener { collection.cancel() }
    }

}
