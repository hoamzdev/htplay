package com.htdev.movie.htplay.detail

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.RectF
import android.os.Bundle
import android.text.TextUtils
import android.view.OrientationEventListener
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.BundleCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.adapter.MovieAdapter
import com.htdev.movie.htplay.base.BaseActivity
import com.htdev.movie.htplay.databinding.ActivityDetailBinding
import com.htdev.movie.htplay.databinding.ItemDetailHeaderBinding
import com.htdev.movie.htplay.databinding.ItemDetailRecommendationsBinding
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.entities.DataMovieDetail
import com.htdev.movie.htplay.domain.entities.MoviePlayer
import com.htdev.movie.htplay.domain.entities.PosterMovie
import com.htdev.movie.htplay.entities.ItemMovie
import com.htdev.movie.htplay.home.download.DownloadViewModel
import com.htdev.movie.htplay.home.download.DownloadMetadata
import com.htdev.movie.htplay.home.download.OfflineDownloads
import com.htdev.movie.htplay.player.InAppMiniPlayer
import com.htdev.movie.htplay.player.MiniPlayback
import com.htdev.movie.htplay.player.SystemPictureInPicture
import com.htdev.movie.htplay.util.SharePref
import com.htdev.movie.htplay.util.getCountryName
import com.htdev.movie.htplay.util.getGenreName
import com.htdev.movie.htplay.util.launchRepeatOnStarted
import com.htdev.movie.htplay.widget.onClickListener
import com.htdev.movie.htplay.widget.checkNetworkConnected
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
@androidx.annotation.OptIn(UnstableApi::class)
class DetailActivity : BaseActivity<ActivityDetailBinding>() {
    @Inject lateinit var miniPlayer: InAppMiniPlayer
    private val viewModel: DetailViewModel by viewModels()
    private val downloadViewModel : DownloadViewModel by viewModels()
    private val episodeAdapter = EpisodeAdapter()
    private lateinit var headerBinding: ItemDetailHeaderBinding
    private lateinit var recommendationsBinding: ItemDetailRecommendationsBinding
    private var isShowingEpisodes = false
    private val moreLikeThisAdapter = MovieAdapter(onClick = { movie ->
        openMovie(movie.slug)
    })
    private val countryMovieAdapter = MovieAdapter(onClick = { movie ->
        openMovie(movie.slug)
    })
    private var isContentExpanded = false
    private var renderedState: DetailUiState? = null
    private var player: ExoPlayer? = null
    private var playbackListener: Player.Listener? = null
    private var pendingExpandBounds: RectF? = null
    private var finishExpandAnimation: (() -> Unit)? = null
    private var progressSaveJob: Job? = null
    private var selectedEpisode: MoviePlayer? = null
    private var playbackPosition = 0L
    private var playWhenReady = true
    private val downloadId: String? by lazy { intent.getStringExtra(EXTRA_DOWNLOAD_ID) }
    private var downloadRequest: DownloadRequest? = null
    private var isOfflineDownload = false
    private var downloadResolved = false
    private var pendingDownloadEpisodeNumber: Int? = null
    private var isFullscreen = false
    private var portraitPlayerHeight = 0
    private var deviceOrientation = Configuration.ORIENTATION_UNDEFINED
    private var canExitFullscreenOnPortrait = false
    private val orientationListener by lazy {
        object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (isInPictureInPictureMode) return
                val physicalOrientation = when (orientation) {
                    in 0..30, in 150..210, in 330..359 -> Configuration.ORIENTATION_PORTRAIT
                    in 60..120, in 240..300 -> Configuration.ORIENTATION_LANDSCAPE
                    else -> return
                }
                deviceOrientation = physicalOrientation
                when {
                    isFullscreen && physicalOrientation == Configuration.ORIENTATION_LANDSCAPE ->
                        canExitFullscreenOnPortrait = true
                    isFullscreen && canExitFullscreenOnPortrait -> requestFullscreen(false)
                    !isFullscreen && physicalOrientation == Configuration.ORIENTATION_PORTRAIT ->
                        releasePortraitOrientationLock()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        savedInstanceState?.let { state ->
            selectedEpisode = BundleCompat.getParcelable(state, STATE_EPISODE, MoviePlayer::class.java)
            playbackPosition = state.getLong(STATE_POSITION)
            playWhenReady = state.getBoolean(STATE_PLAY_WHEN_READY, true)
        }
        super.onCreate(savedInstanceState)
    }

    override fun createViewBinding(): ActivityDetailBinding {
        return ActivityDetailBinding.inflate(layoutInflater)
    }

    override fun initView() {
        isOfflineDownload = downloadId != null && !checkNetworkConnected(this)
        // Inflating sections with this RecyclerView as parent requires its LayoutManager first.
        binding.scrollDetail.layoutManager = LinearLayoutManager(this)
        headerBinding = ItemDetailHeaderBinding.inflate(layoutInflater, binding.scrollDetail, false)
        recommendationsBinding = ItemDetailRecommendationsBinding.inflate(layoutInflater, binding.scrollDetail, false)
        binding.scrollDetail.apply {
            adapter = ConcatAdapter(
                DetailSectionAdapter(headerBinding.root),
                episodeAdapter,
                DetailSectionAdapter(recommendationsBinding.root),
            )
            itemAnimator = null
            setHasFixedSize(true)
        }
        recommendationsBinding.rcMoreLikeThis.adapter = moreLikeThisAdapter
        recommendationsBinding.rcMoreLikeThisCountry.adapter = countryMovieAdapter
        binding.tvNameMovie.text = ""
        headerBinding.tvStar.text = ""
        headerBinding.tvYear.text = ""
        headerBinding.tvCountry.text = ""
        headerBinding.tvGenre.text = ""
        recommendationsBinding.tvMore.isVisible = false
        recommendationsBinding.rcMoreLikeThis.isVisible = false
        recommendationsBinding.tvMoreCountry.isVisible = false
        recommendationsBinding.rcMoreLikeThisCountry.isVisible = false
        selectTab(showEpisodes = false)
        updateContentExpansion()
        portraitPlayerHeight = binding.mediaPlay.layoutParams.height
        applyFullscreen(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
        binding.mediaPlay.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateSystemPip()
        }
    }

    override fun initViewModel() {
        val restored = miniPlayer.take(intent.getStringExtra(EXTRA_MINI_PLAYER_ID))
        if (restored != null) {
            pendingExpandBounds = restored.expandBounds
            player = restored.player
            selectedEpisode = restored.episode
            playbackPosition = restored.player.currentPosition
            playWhenReady = restored.player.playWhenReady
            isOfflineDownload = restored.isOffline
            downloadRequest = restored.downloadRequest
            downloadResolved = true
            binding.tvNameMovie.text = restored.title
            applyFullscreen(isFullscreen)
            if (pendingExpandBounds != null) {
                binding.mediaPlay.alpha = 0f
                setDetailControlsAlpha(0f)
            }
            if (!isOfflineDownload) initializeMovie(restored.movieSlug, restored.state?.selectedServer)
            return
        }
        miniPlayer.close()
        val id = downloadId
        if (id == null) {
            initializeMovie(intent.getStringExtra(EXTRA_MOVIE_SLUG).orEmpty())
            return
        }
        lifecycleScope.launch {
            val download = withContext(Dispatchers.IO) {
                OfflineDownloads.manager(this@DetailActivity).downloadIndex.getDownload(id)
            }
            if (download == null) {
                showToastApp(getString(R.string.download_not_ready))
                finish()
                return@launch
            }
            val metadata = DownloadMetadata.fromByteArray(download.request.data)
            val connected = checkNetworkConnected(this@DetailActivity)
            val slug = if (connected) downloadViewModel.resolveMovieSlug(metadata) else null
            isOfflineDownload = !connected || slug.isNullOrBlank()
            downloadRequest = download.request
            downloadResolved = true
            if (selectedEpisode == null) {
                selectedEpisode = MoviePlayer(download.request.id, download.request.uri.toString())
                pendingDownloadEpisodeNumber = metadata.episodeNumber
            }
            binding.tvNameMovie.text = metadata.title
            applyFullscreen(isFullscreen)
            if (!isOfflineDownload) {
                initializeMovie(requireNotNull(slug))
            }
            if (download.state != Download.STATE_COMPLETED && isOfflineDownload) {
                selectedEpisode = null
            }
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) startPlayer()
            if (download.state != Download.STATE_COMPLETED && isOfflineDownload) {
                showPlaybackMessage(R.string.download_not_ready)
            }
        }
    }

    private fun initializeMovie(slug: String, server: EpisodeServer? = null) {
        viewModel.initialize(slug, resources.configuration.locales[0].language, server)
    }

    override fun handleClickEvent(): Unit = with(binding) {
        ivDrop.onClickListener(requiresNetwork = false) { backDevice() }
        mediaPlay.setFullscreenButtonClickListener { fullscreen ->
            if (fullscreen != isFullscreen) requestFullscreen(fullscreen)
        }
        headerBinding.tvAbout.onClickListener { selectTab(showEpisodes = false) }
        headerBinding.tvEpisodes.onClickListener { selectTab(showEpisodes = true) }
        headerBinding.serverTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val server = if (tab.position == 1) EpisodeServer.DUBBED else EpisodeServer.ORIGINAL
                if (renderedState?.selectedServer == server) return
                persistContinueWatching()
                savePlaybackState()
                viewModel.selectEpisodeServer(server)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        headerBinding.tvContent.onClickListener {
            isContentExpanded = !isContentExpanded
            updateContentExpansion()
        }
        headerBinding.ivDownload.onClickListener {
            onDownloadMovie()
        }
        headerBinding.ivFavorite.onClickListener {
            onFavoriteMovie()
        }
        episodeAdapter.onEpisodeClick = { episode -> playEpisode(episode) }
        mediaPlay.findViewById<View>(androidx.media3.ui.R.id.exo_error_message)?.onClickListener(requiresNetwork = false) {
            player?.takeIf { it.playerError != null }?.let {
                mediaPlay.setCustomErrorMessage(null)
                progressBar.visibility = if (isOfflineDownload) View.INVISIBLE else View.VISIBLE
                it.prepare()
                it.play()
            }
        }
    }

    private fun onDownloadMovie() {
        val episode = selectedEpisode ?: return
        if (episode.linkM3u8.isBlank()) return
        val episodes = (renderedState?.episodes as? Result.Success)?.data.orEmpty()
        val episodeIndex = episodes.indexOfFirst { it.id == episode.id }
        if (episodeIndex < 0) return
        val movie = (renderedState?.detail as? Result.Success)?.data ?: return
        runCatching {
            downloadViewModel.downloadEpisode(
                episode.linkM3u8.trim(), binding.tvNameMovie.text.toString(), episodeIndex + 1,
                renderedState?.movieSlug.orEmpty(),
                movie.posterUrl.ifBlank { movie.thumbUrl },
            )
        }.onSuccess {
            showToastApp(getString(R.string.download_added))
        }.onFailure {
            showToastApp(getString(R.string.download_start_error))
        }
    }

    private fun onFavoriteMovie(){
        viewModel.toggleFavorite()
    }

    private fun selectTab(showEpisodes: Boolean) = with(headerBinding) {
        isShowingEpisodes = showEpisodes
        tvContent.isVisible = !showEpisodes
        renderServerTabs()
        renderEpisodes()
        tvAbout.isSelected = !showEpisodes
        tvEpisodes.isSelected = showEpisodes
        val aboutColor = getColor(if (showEpisodes) R.color.white else R.color.secondary_color)
        val episodesColor = getColor(if (showEpisodes) R.color.secondary_color else R.color.white)
        tvAbout.setTextColor(aboutColor)
        tvEpisodes.setTextColor(episodesColor)
        vLandAbout.setBackgroundColor(aboutColor)
        vLandEpisodes.setBackgroundColor(episodesColor)
    }

    private fun renderEpisodes() {
        // Recycle complete six-button rows without changing the list during a fling.
        val episodes = (renderedState?.episodes as? Result.Success)?.data.orEmpty()
        episodeAdapter.submitEpisodes(if (isShowingEpisodes) episodes else emptyList())
    }

    private fun renderServerTabs() = with(headerBinding.serverTabs) {
        val state = renderedState
        isVisible = isShowingEpisodes && state?.showServerTabs == true
        if (state?.showServerTabs != true) return@with
        getTabAt(0)?.view?.apply {
            isEnabled = state.episodeServers.vietsub.any { it.linkM3u8.isNotBlank() }
            alpha = if (isEnabled) 1f else 0.5f
        }
        val selectedIndex = if (state.selectedServer == EpisodeServer.DUBBED) 1 else 0
        if (selectedTabPosition != selectedIndex) selectTab(getTabAt(selectedIndex))
    }

    private fun updateContentExpansion() = with(headerBinding.tvContent) {
        maxLines = if (isContentExpanded) Int.MAX_VALUE else 3
        ellipsize = if (isContentExpanded) null else TextUtils.TruncateAt.END
    }

    private fun requestFullscreen(fullscreen: Boolean) {
        // A button can enter landscape while the phone is still upright. Wait until it has
        // physically reached landscape before treating a portrait sensor reading as an exit.
        canExitFullscreenOnPortrait = fullscreen && deviceOrientation == Configuration.ORIENTATION_LANDSCAPE
        applyFullscreen(fullscreen)
        requestedOrientation = if (fullscreen) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            else ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    }

    private fun applyFullscreen(fullscreen: Boolean) = with(binding) {
        isFullscreen = fullscreen
        val inPip = isInPictureInPictureMode
        tvNameMovie.isVisible = !fullscreen && !inPip
        updateCurrentEpisodeLabel()
        scrollDetail.visibility = if (inPip || fullscreen) View.GONE else if (isOfflineDownload) View.INVISIBLE else View.VISIBLE
        ivDrop.visibility = if (isOfflineDownload || inPip) View.INVISIBLE else View.VISIBLE
        if (isOfflineDownload) progressBar.visibility = View.INVISIBLE
        mediaPlay.updateLayoutParams<ConstraintLayout.LayoutParams> {
            height = if (fullscreen || inPip) ConstraintLayout.LayoutParams.MATCH_CONSTRAINT else portraitPlayerHeight
            bottomToBottom = if (fullscreen || inPip) ConstraintLayout.LayoutParams.PARENT_ID
                else ConstraintLayout.LayoutParams.UNSET
        }
        mediaPlay.setFullscreenButtonState(fullscreen)
        mediaPlay.useController = !inPip && finishExpandAnimation == null && pendingExpandBounds == null
    }

    private fun releasePortraitOrientationLock() {
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT &&
            requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        finishExpandAnimation?.invoke()
        if (isInPictureInPictureMode) return
        applyFullscreen(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
        if (isFullscreen && deviceOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            canExitFullscreenOnPortrait = true
        } else if (!isFullscreen && deviceOrientation == Configuration.ORIENTATION_PORTRAIT) {
            releasePortraitOrientationLock()
        }
    }

    private fun updateSystemPip() {
        SystemPictureInPicture.update(this, player, binding.mediaPlay)
    }

    override fun onResume() {
        super.onResume()
        updateSystemPip()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        SystemPictureInPicture.onUserLeaveHint(this, player, binding.mediaPlay)
    }

    override fun onPictureInPictureModeChanged(inPip: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(inPip, newConfig)
        finishExpandAnimation?.invoke()
        if (inPip) {
            orientationListener.disable()
            binding.mediaPlay.hideController()
        } else if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) && orientationListener.canDetectOrientation()) {
            orientationListener.enable()
        }
        applyFullscreen(if (inPip) isFullscreen else newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
    }

    private fun setDetailControlsAlpha(alpha: Float) = with(binding) {
        scrollDetail.alpha = alpha
        tvNameMovie.alpha = alpha
        tvCurrentEpisode.alpha = alpha
        ivDrop.alpha = alpha
        progressBar.alpha = alpha
    }

    private fun animatePlayerExpansion() {
        val source = pendingExpandBounds ?: return
        pendingExpandBounds = null
        val video = binding.mediaPlay
        val originalElevation = video.elevation
        val finish = {
            video.animate().withEndAction(null).cancel()
            video.scaleX = 1f
            video.scaleY = 1f
            video.translationX = 0f
            video.translationY = 0f
            video.alpha = 1f
            video.elevation = originalElevation
            video.useController = !isInPictureInPictureMode
            setDetailControlsAlpha(1f)
            finishExpandAnimation = null
            updateSystemPip()
        }
        finishExpandAnimation = finish
        video.useController = false
        video.doOnLayout {
            if (finishExpandAnimation !== finish) return@doOnLayout
            if (isFinishing || isDestroyed || video.width == 0 || video.height == 0) {
                finish()
                return@doOnLayout
            }
            val location = IntArray(2)
            video.getLocationOnScreen(location)
            video.pivotX = 0f
            video.pivotY = 0f
            video.scaleX = source.width() / video.width
            video.scaleY = source.height() / video.height
            video.translationX = source.left - location[0]
            video.translationY = source.top - location[1]
            video.elevation = 16f * resources.displayMetrics.density
            video.alpha = 1f
            video.animate().scaleX(1f).scaleY(1f).translationX(0f).translationY(0f)
                .setDuration(280L).setInterpolator(DecelerateInterpolator())
                .withEndAction { finish() }.start()
        }
    }

    override fun backDevice() {
        persistContinueWatching()
        when {
            minimizePlayer() -> Unit
            isFullscreen -> requestFullscreen(false)
            !isOfflineDownload && viewModel.goBack() -> Unit
            else -> super.backDevice()
        }
    }

    @Suppress("DEPRECATION")
    private fun minimizePlayer(): Boolean {
        if (!SharePref.init(this).isEnablePip()) return false
        val currentPlayer = player ?: return false
        val episode = selectedEpisode ?: return false
        if (currentPlayer.currentMediaItem == null || currentPlayer.playerError != null ||
            currentPlayer.playbackState == Player.STATE_ENDED ||
            currentPlayer.playbackState == Player.STATE_IDLE) return false
        finishExpandAnimation?.invoke()
        SystemPictureInPicture.update(this, null, null, enabled = false)
        progressSaveJob?.cancel()
        progressSaveJob = null
        playbackListener?.let(currentPlayer::removeListener)
        playbackListener = null
        miniPlayer.show(
            MiniPlayback(
                player = currentPlayer,
                movieSlug = renderedState?.movieSlug.orEmpty(),
                title = binding.tvNameMovie.text.toString(),
                episode = episode,
                state = renderedState,
                downloadId = downloadId,
                downloadRequest = downloadRequest,
                isOffline = isOfflineDownload,
            ),
            binding.mediaPlay,
        )
        player = null
        binding.mediaPlay.keepScreenOn = false
        if (isTaskRoot) startActivity(Intent(this, com.htdev.movie.htplay.home.HomeRouterActivity::class.java))
        finish()
        overridePendingTransition(0, 0)
        return true
    }

    private fun openMovie(slug: String) {
        persistContinueWatching()
        viewModel.openMovie(slug)
    }

    private fun resetMovieContent() = with(binding) {
        selectedEpisode = null
        playbackPosition = 0L
        playWhenReady = true
        player?.stop()
        player?.clearMediaItems()
        mediaPlay.setCustomErrorMessage(null)
        mediaPlay.keepScreenOn = false
        progressBar.isVisible = true
        episodeAdapter.selectedEpisodeId = null
        episodeAdapter.submitList(emptyList())
        moreLikeThisAdapter.submitList(emptyList())
        countryMovieAdapter.submitList(emptyList())
        tvNameMovie.text = ""
        tvCurrentEpisode.text = ""
        tvCurrentEpisode.isVisible = false
        headerBinding.tvStar.text = ""
        headerBinding.tvYear.text = ""
        headerBinding.tvCountry.text = ""
        headerBinding.tvGenre.text = ""
        headerBinding.tvContent.setText(R.string.loading)
        recommendationsBinding.tvMore.isVisible = false
        recommendationsBinding.rcMoreLikeThis.isVisible = false
        recommendationsBinding.tvMoreCountry.isVisible = false
        recommendationsBinding.rcMoreLikeThisCountry.isVisible = false
        isContentExpanded = false
        updateContentExpansion()
        selectTab(showEpisodes = false)
        scrollDetail.scrollToPosition(0)
        recommendationsBinding.rcMoreLikeThis.scrollToPosition(0)
        recommendationsBinding.rcMoreLikeThisCountry.scrollToPosition(0)
    }

    override fun observerData() {
        launchRepeatOnStarted {
            viewModel.uiState.collect { state -> bindState(state) }
        }
    }

    private fun bindState(state: DetailUiState) {
        if (downloadId != null && (!downloadResolved || isOfflineDownload)) return
        val previous = renderedState
        val movieChanged = previous?.movieSlug != state.movieSlug
        // Reset and bind from the same snapshot, even when loading finishes before it is drawn.
        if (previous != null && movieChanged) resetMovieContent()
        renderedState = state
        renderServerTabs()

        headerBinding.ivFavorite.apply {
            setImageResource(if (state.isMovieFavorite == true) R.drawable.ic_checked_favorite else R.drawable.iv_favo)
            isEnabled = state.detail is Result.Success && state.isMovieFavorite != null && !state.isUpdatingFavorite
        }
        when (val change = state.favoriteChange) {
            is Result.Success -> {
                showToastApp(getString(if (change.data) R.string.detail_favorite_added else R.string.detail_favorite_removed))
                viewModel.clearFavoriteChange()
            }
            is Result.Error -> {
                showToastApp(getString(R.string.detail_favorite_error))
                viewModel.clearFavoriteChange()
            }
            else -> Unit
        }

        if (movieChanged || previous.detail != state.detail) {
            when (val result = state.detail) {
                is Result.Success -> bindDetail(result.data)
                is Result.Error -> headerBinding.tvContent.setText(R.string.detail_load_error)
                Result.Idle, Result.Loading -> headerBinding.tvContent.setText(R.string.loading)
            }
        }
        val serverChanged = !movieChanged && previous?.selectedServer != state.selectedServer
        if (movieChanged || serverChanged || previous.episodes != state.episodes) {
            when (val result = state.episodes) {
                is Result.Success -> {
                    val episodes = result.data
                    if (pendingDownloadEpisodeNumber != null && state.showServerTabs) {
                        val url = downloadRequest?.uri?.toString()
                        val server = when {
                            state.episodeServers.vietsub.any { it.linkM3u8.trim() == url } -> EpisodeServer.ORIGINAL
                            state.episodeServers.longtieng.any { it.linkM3u8.trim() == url } -> EpisodeServer.DUBBED
                            else -> state.selectedServer
                        }
                        if (server != state.selectedServer) {
                            viewModel.selectEpisodeServer(server)
                            return
                        }
                    }
                    renderEpisodes()
                    val episode = episodes.firstOrNull {
                        it.linkM3u8.isNotBlank() && it.linkM3u8.trim() == selectedEpisode?.linkM3u8?.trim()
                    } ?: episodes.firstOrNull {
                        it.id == selectedEpisode?.id && it.linkM3u8.isNotBlank()
                    } ?: pendingDownloadEpisodeNumber?.let { number ->
                        episodes.getOrNull(number - 1)?.takeIf { it.linkM3u8.isNotBlank() }
                    } ?: state.continueWatching?.let { saved ->
                        episodes.getOrNull(saved.currentEpisode)?.takeIf { it.linkM3u8.isNotBlank() }
                    } ?: episodes.firstOrNull { it.linkM3u8.isNotBlank() }
                    if (episode == null) {
                        selectedEpisode = null
                        player?.stop()
                        player?.clearMediaItems()
                        showPlaybackMessage(R.string.detail_no_playable_episodes)
                    } else if (episode != selectedEpisode) {
                        val saved = state.continueWatching
                        val resumePosition = when {
                            episode.linkM3u8.trim() == selectedEpisode?.linkM3u8?.trim() -> playbackPosition
                            serverChanged && episode.id == selectedEpisode?.id -> playbackPosition
                            selectedEpisode == null && saved != null &&
                                episodes.getOrNull(saved.currentEpisode) == episode -> saved.positionContinue
                            else -> 0L
                        }
                        playEpisode(episode, resumePosition)
                    }
                    episodeAdapter.selectedEpisodeId = selectedEpisode?.id
                    pendingDownloadEpisodeNumber = null
                }
                is Result.Error -> if (selectedEpisode == null) {
                    showPlaybackMessage(R.string.detail_episodes_error)
                }
                Result.Idle, Result.Loading -> if (selectedEpisode == null) {
                    binding.mediaPlay.setCustomErrorMessage(null)
                    binding.progressBar.isVisible = true
                }
            }
        }
        updateCurrentEpisodeLabel()
        if (movieChanged || previous.moreLikeThis != state.moreLikeThis) {
            val movies = (state.moreLikeThis as? Result.Success)?.data.orEmpty()
            moreLikeThisAdapter.submitList(movies.toMovieItems())
            recommendationsBinding.tvMore.isVisible = movies.isNotEmpty()
            recommendationsBinding.rcMoreLikeThis.isVisible = movies.isNotEmpty()
        }
        if (movieChanged || previous.countryMovies != state.countryMovies) {
            val movies = (state.countryMovies as? Result.Success)?.data.orEmpty()
            countryMovieAdapter.submitList(movies.toMovieItems())
            recommendationsBinding.tvMoreCountry.isVisible = movies.isNotEmpty()
            recommendationsBinding.rcMoreLikeThisCountry.isVisible = movies.isNotEmpty()
        }
    }

    private fun updateCurrentEpisodeLabel() = with(binding.tvCurrentEpisode) {
        if (isOfflineDownload || isInPictureInPictureMode) {
            visibility = View.INVISIBLE
            return@with
        }
        val episodes = (renderedState?.episodes as? Result.Success)?.data.orEmpty()
        val episodeIndex = episodes.indexOfFirst { it.id == selectedEpisode?.id }
        text = if (episodeIndex >= 0) getString(R.string.detail_episode_number, episodeIndex + 1) else ""
        isVisible = !isFullscreen && episodeIndex >= 0
    }

    private fun bindDetail(movie: DataMovieDetail) = with(binding) {
        tvNameMovie.text = movie.name
        headerBinding.tvStar.text = movie.vote
        headerBinding.tvYear.text = movie.year.takeIf { it > 0 }?.toString().orEmpty()
        headerBinding.tvCountry.text = getCountryName(movie.countrySlug)
        recommendationsBinding.tvMoreCountry.text = getString(R.string.detail_more_from_country, getCountryName(movie.countrySlug))
        headerBinding.tvGenre.text = getGenreName(movie.genreSlug.ifBlank { movie.genre })
        headerBinding.tvContent.text = HtmlCompat.fromHtml(movie.content, HtmlCompat.FROM_HTML_MODE_COMPACT).trim()
    }

    private fun List<PosterMovie>.toMovieItems(): List<ItemMovie> = map { movie ->
        ItemMovie(
            id = UUID.nameUUIDFromBytes(movie.id.ifBlank { movie.slug }.toByteArray(Charsets.UTF_8)),
            urlPoster = movie.posterUrl.ifBlank { movie.thumbUrl },
            nameFile = movie.name,
            slug = movie.slug,
        )
    }

    private fun playEpisode(episode: MoviePlayer, startPosition: Long = 0L) {
        if (episode.linkM3u8.isBlank()) return
        val currentPlayer = player
        if (selectedEpisode == episode && currentPlayer?.currentMediaItem?.mediaId == episode.id &&
            currentPlayer.playerError == null) {
            currentPlayer.play()
            return
        }
        persistContinueWatching()
        selectedEpisode = episode
        episodeAdapter.selectedEpisodeId = episode.id
        updateCurrentEpisodeLabel()
        playbackPosition = startPosition.coerceAtLeast(0L)
        playWhenReady = true
        prepareSelectedEpisode()
    }

    private fun playNextEpisodeIfEnabled() {
        if (isOfflineDownload || !SharePref.init(this).isNextEpisode()) return
        val episode = selectedEpisode ?: return
        val episodes = (renderedState?.episodes as? Result.Success)?.data ?: return
        val currentIndex = episodes.indexOf(episode)
        if (currentIndex < 0) return
        val nextEpisode = episodes.drop(currentIndex + 1)
            .firstOrNull { it.linkM3u8.isNotBlank() } ?: return
        playEpisode(nextEpisode)
    }

    private fun prepareSelectedEpisode() {
        val episode = selectedEpisode ?: return
        val currentPlayer = player ?: return
        binding.mediaPlay.setCustomErrorMessage(null)
        binding.progressBar.visibility = if (isOfflineDownload) View.INVISIBLE else View.VISIBLE
        currentPlayer.setMediaItem(
            if (isOfflineDownload) downloadRequest?.toMediaItem() ?: return else MediaItem.Builder()
                .setMediaId(episode.id)
                .setUri(episode.linkM3u8.trim())
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .build(),
            playbackPosition,
        )
        currentPlayer.prepare()
        currentPlayer.playWhenReady = playWhenReady
    }

    override fun onStart() {
        super.onStart()
        if (downloadId == null || downloadResolved) startPlayer()
        if (orientationListener.canDetectOrientation()) orientationListener.enable()
    }

    private fun startPlayer() {
        if (player != null && binding.mediaPlay.player === player) return
        player = (player ?: ExoPlayer.Builder(applicationContext).setMediaSourceFactory(
            androidx.media3.exoplayer.source.DefaultMediaSourceFactory(
                OfflineDownloads.dataSource(applicationContext, offlineOnly = isOfflineDownload)
            )
        ).build()).also { currentPlayer ->
            currentPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true,
            )
            currentPlayer.setHandleAudioBecomingNoisy(true)
            val listener = object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    updateSystemPip()
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    binding.mediaPlay.keepScreenOn = isPlaying
                    if (!isPlaying) persistContinueWatching()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    binding.progressBar.visibility = if (isOfflineDownload) View.INVISIBLE
                        else if (playbackState == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
                    if (playbackState == Player.STATE_READY) binding.mediaPlay.setCustomErrorMessage(null)
                    if (playbackState == Player.STATE_ENDED) {
                        persistContinueWatching()
                        playNextEpisodeIfEnabled()
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    showPlaybackMessage(R.string.detail_playback_error)
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int,
                ) {
                    if (reason == Player.DISCONTINUITY_REASON_SEEK) persistContinueWatching()
                }
            }
            playbackListener = listener
            currentPlayer.addListener(listener)
            binding.mediaPlay.player = currentPlayer
            binding.mediaPlay.keepScreenOn = currentPlayer.isPlaying
            binding.progressBar.visibility = if (isOfflineDownload) View.INVISIBLE
                else if (currentPlayer.playbackState == Player.STATE_BUFFERING ||
                    currentPlayer.playbackState == Player.STATE_IDLE) View.VISIBLE else View.GONE
            if (currentPlayer.playerError != null) showPlaybackMessage(R.string.detail_playback_error)
        }
        if (player?.currentMediaItem == null) prepareSelectedEpisode()
        animatePlayerExpansion()
        updateSystemPip()
        if (!isOfflineDownload) progressSaveJob = lifecycleScope.launch {
            while (isActive) {
                delay(5_000.milliseconds)
                if (player?.isPlaying == true) persistContinueWatching()
            }
        }
    }

    private fun showPlaybackMessage(@androidx.annotation.StringRes message: Int) {
        binding.progressBar.visibility = if (isOfflineDownload) View.INVISIBLE else View.GONE
        binding.mediaPlay.setCustomErrorMessage(getString(message))
    }

    private fun savePlaybackState() {
        player?.let {
            playbackPosition = it.currentPosition.coerceAtLeast(0L)
            playWhenReady = it.playWhenReady
        }
    }

    private fun persistContinueWatching() {
        if (isOfflineDownload) return
        val currentPlayer = player ?: return
        val episode = selectedEpisode ?: return
        if (currentPlayer.currentMediaItem?.localConfiguration?.uri?.toString() != episode.linkM3u8.trim()) return
        viewModel.savePlaybackProgress(episode, currentPlayer.currentPosition, currentPlayer.duration)
    }

    override fun onPause() {
        persistContinueWatching()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        savePlaybackState()
        outState.putParcelable(STATE_EPISODE, selectedEpisode)
        outState.putLong(STATE_POSITION, playbackPosition)
        outState.putBoolean(STATE_PLAY_WHEN_READY, playWhenReady)
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        finishExpandAnimation?.invoke()
        progressSaveJob?.cancel()
        progressSaveJob = null
        persistContinueWatching()
        orientationListener.disable()
        savePlaybackState()
        binding.mediaPlay.player = null
        playbackListener?.let { player?.removeListener(it) }
        playbackListener = null
        player?.release()
        player = null
        binding.mediaPlay.keepScreenOn = false
        super.onStop()
    }

    override fun onDestroy() {
        binding.scrollDetail.adapter = null
        recommendationsBinding.rcMoreLikeThis.adapter = null
        recommendationsBinding.rcMoreLikeThisCountry.adapter = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_MOVIE_SLUG = "movie_slug"
        private const val STATE_EPISODE = "detail_episode"
        private const val EXTRA_DOWNLOAD_ID = "download_id"
        const val EXTRA_MINI_PLAYER_ID = "mini_player_id"
        private const val STATE_POSITION = "detail_position"
        private const val STATE_PLAY_WHEN_READY = "detail_play_when_ready"

        fun createIntent(context: Context, movieSlug: String): Intent =
            Intent(context, DetailActivity::class.java).putExtra(EXTRA_MOVIE_SLUG, movieSlug)

        fun createDownloadIntent(context: Context, downloadId: String): Intent =
            Intent(context, DetailActivity::class.java).putExtra(EXTRA_DOWNLOAD_ID, downloadId)
    }
}
