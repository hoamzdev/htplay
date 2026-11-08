package com.htdev.movie.htplay.home.home


import android.content.Intent
import androidx.fragment.app.viewModels
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
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.base.BaseFragment
import com.htdev.movie.htplay.databinding.FragmentHomeBinding
import com.htdev.movie.htplay.detail.DetailActivity
import com.htdev.movie.htplay.entities.HomeViewAllTarget
import com.htdev.movie.htplay.util.buildHomeSections
import com.htdev.movie.htplay.util.getGenreName
import com.htdev.movie.htplay.util.getCountryName
import com.htdev.movie.htplay.util.launchRepeatOnStarted
import com.htdev.movie.htplay.home.home.viewmodel.HomeViewModel
import com.htdev.movie.htplay.search.SearchActivity
import com.htdev.movie.htplay.widget.onClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch


@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private val viewModel: HomeViewModel by viewModels()

    private val posterPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            updatePosterIndicator(position)
        }
    }

    private val posterHomeAdapter = PosterHomeAdapter(
        onMovieClick = { movie ->
            //nav to detail movie
            navToMovieDetail(movie.slug)
        },
    ).apply {
        stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    private fun navToMovieDetail(movieSlug : String){
        val intent = DetailActivity.createIntent(requireActivity(), movieSlug)
        startActivity(intent)
    }

    private val homeAdapter = HomeAdapter(
        onMovieClick = { movie ->
            navToMovieDetail(movie.slug)
        },
        onContinueClick = { continueWatching ->
            val movieSlug : String = continueWatching.slugMovie
            navToMovieDetail(movieSlug)
            //nav to detail movie
        },
        onViewAllClick = { source ->
            //nav to show all activity
            handleNavToShowAllSector(source)
        },
        onCountryClick = { country ->
            startActivity(ShowAllMovieActivity.countryIntent(requireContext(), country))
        },
        onRetryClick = { viewModel.retryHomeSection(it.id) },
    )

    private var genreTabsController: HomeGenreTabsController? = null
    private var genreMovieAdapter: HomeGenreMovieAdapter? = null
    private var displayedHomeGenre: String? = null
    private val genreStatusAdapter = HomeGenreStatusAdapter(onRetry = { genreMovieAdapter?.retry() })
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

    override fun createViewBinding(): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(layoutInflater)
    }

    override fun initView() {
        genreTabsController = HomeGenreTabsController(
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
        binding.appBar.addOnOffsetChangedListener(appBarOffsetListener)
        binding.vpPoster.adapter = posterHomeAdapter
        binding.vpPoster.registerOnPageChangeCallback(posterPageChangeCallback)
        binding.posterStatus.setOnClickListener {
            if (viewModel.moviesByPage.value is Result.Error) {
                viewModel.fetchMoviesByPage()
            }
        }
        binding.rcMain.apply {
            layoutManager = GridLayoutManager(requireContext(), 3).apply {
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

    private fun handleNavToShowAllSector(source: HomeViewAllTarget){
        when(source){
            is HomeViewAllTarget.ContinueWatching -> {
                val intent = Intent(requireActivity(), ShowAllMovieActivity::class.java).apply {
                    putExtra(ShowAllMovieActivity.KEY_SECTOR, ShowAllMovieActivity.SECTOR_CONTINUE)
                }
                startActivity(intent)
            }
            is HomeViewAllTarget.Latest -> {
                val intent = Intent(requireActivity(), ShowAllMovieActivity::class.java).apply {
                    putExtra(ShowAllMovieActivity.KEY_SECTOR, ShowAllMovieActivity.SECTOR_MOVIE_LATEST)
                }
                startActivity(intent)
            }
            is HomeViewAllTarget.Recommended -> {
                val intent = Intent(requireActivity(), ShowAllMovieActivity::class.java).apply {
                    putExtra(ShowAllMovieActivity.KEY_SECTOR, ShowAllMovieActivity.SECTOR_MOVIE_RECOMMENDED)
                    putStringArrayListExtra(ShowAllMovieActivity.KEY_DATA, ArrayList(source.genreSlugs))
                }
                startActivity(intent)
            }
            is HomeViewAllTarget.ByGenre -> {
                when(source.slug){
                    "hanh-dong" -> {
                        val intent = Intent(requireActivity(), ShowAllMovieActivity::class.java).apply {
                            putExtra(ShowAllMovieActivity.KEY_SECTOR, ShowAllMovieActivity.SECTOR_MOVIE_ACTION)
                        }
                        startActivity(intent)
                    }
                    "tinh-cam" -> {
                        val intent = Intent(requireActivity(), ShowAllMovieActivity::class.java).apply {
                            putExtra(ShowAllMovieActivity.KEY_SECTOR, ShowAllMovieActivity.SECTOR_MOVIE_ROMANCE)
                        }
                        startActivity(intent)
                    }
                    "gia-dinh" -> {
                        val intent = Intent(requireActivity(), ShowAllMovieActivity::class.java).apply {
                            putExtra(ShowAllMovieActivity.KEY_SECTOR, ShowAllMovieActivity.SECTOR_MOVIE_FAMILY)
                        }
                        startActivity(intent)
                    }
                }
            }
            is HomeViewAllTarget.ByCountry -> {
                when (source.slug) {
                    "trung-quoc" -> {
                        val intent = Intent(requireActivity(), ShowAllMovieActivity::class.java).apply {
                            putExtra(ShowAllMovieActivity.KEY_SECTOR, ShowAllMovieActivity.SECTOR_MOVIE_CHINESE)
                        }
                        startActivity(intent)
                    }
                    "han-quoc" -> {
                        val intent = Intent(requireActivity(), ShowAllMovieActivity::class.java).apply {
                            putExtra(ShowAllMovieActivity.KEY_SECTOR, ShowAllMovieActivity.SECTOR_MOVIE_KOREAN)
                        }
                        startActivity(intent)
                    }
                }
            }
            is HomeViewAllTarget.Countries -> {
                val intent = Intent(requireActivity(), ShowAllMovieActivity::class.java).apply {
                    putExtra(ShowAllMovieActivity.KEY_SECTOR, ShowAllMovieActivity.SECTOR_MOVIE_COUNTRIES)
                }
                startActivity(intent)
            }
        }
    }

    override fun handleClickEvent() {
        binding.apply {
            edtSearch.onClickListener {
                val intent = Intent(requireActivity(), SearchActivity::class.java)
                startActivity(intent)
            }
        }
    }

    override fun bindViewModel() {
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
                        val name = requireContext().getCountryName(country.slug)
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
                        val name = requireContext().getGenreName(genre.slug)
                        genre.copy(name = name.takeUnless { it == genre.slug } ?: genre.name)
                    }) else genres
                    genreTabsController?.submit(localized, slug)
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
        // Detach old cards before displaying the new genre.
        genreMovieAdapter?.let { mainAdapter.removeAdapter(it) }
        genreMovieAdapter = null
        if (slug == null) {
            mainAdapter.removeAdapter(genreStatusAdapter)
            if (homeAdapter !in mainAdapter.adapters) mainAdapter.addAdapter(homeAdapter)
        } else {
            mainAdapter.removeAdapter(homeAdapter)
            genreMovieAdapter = HomeGenreMovieAdapter(onClick = { movie ->
                navToMovieDetail(movie.slug)
            })
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
                                if (view != null) updatePosterIndicator()
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

    override fun onDestroyView() {
        binding.appBar.removeOnOffsetChangedListener(appBarOffsetListener)
        binding.vpPoster.unregisterOnPageChangeCallback(posterPageChangeCallback)
        binding.vpPoster.adapter = null
        binding.rcMain.adapter = null
        genreTabsController = null
        genreMovieAdapter?.let { mainAdapter.removeAdapter(it) }
        genreMovieAdapter = null
        mainAdapter.removeAdapter(genreStatusAdapter)
        if (homeAdapter !in mainAdapter.adapters) mainAdapter.addAdapter(homeAdapter)
        displayedHomeGenre = null
        super.onDestroyView()
    }

}
