package com.htdev.movie.htplay.detail

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.text.TextUtils
import android.view.OrientationEventListener
import android.view.View
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.BundleCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.htdev.movie.htplay.util.getCountryName
import com.htdev.movie.htplay.util.getGenreName
import com.htdev.movie.htplay.util.launchRepeatOnStarted
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
@androidx.annotation.OptIn(UnstableApi::class)
class DetailActivity : BaseActivity<ActivityDetailBinding>() {
    private val viewModel: DetailViewModel by viewModels()
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
    private var progressSaveJob: Job? = null
    private var selectedEpisode: MoviePlayer? = null
    private var playbackPosition = 0L
    private var playWhenReady = true
    private var isFullscreen = false
    private var portraitPlayerHeight = 0
    private var deviceOrientation = Configuration.ORIENTATION_UNDEFINED
    private var canExitFullscreenOnPortrait = false
    private val orientationListener by lazy {
        object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
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
    }

    override fun initViewModel() {
        viewModel.initialize(intent.getStringExtra(EXTRA_MOVIE_SLUG).orEmpty())
    }

    override fun handleClickEvent(): Unit = with(binding) {
        ivDrop.setOnClickListener { backDevice() }
        mediaPlay.setFullscreenButtonClickListener { fullscreen ->
            if (fullscreen != isFullscreen) requestFullscreen(fullscreen)
        }
        headerBinding.tvAbout.setOnClickListener { selectTab(showEpisodes = false) }
        headerBinding.tvEpisodes.setOnClickListener { selectTab(showEpisodes = true) }
        headerBinding.tvContent.setOnClickListener {
            isContentExpanded = !isContentExpanded
            updateContentExpansion()
        }
        episodeAdapter.onEpisodeClick = { episode -> playEpisode(episode) }
        mediaPlay.findViewById<View>(androidx.media3.ui.R.id.exo_error_message)?.setOnClickListener {
            player?.takeIf { it.playerError != null }?.let {
                mediaPlay.setCustomErrorMessage(null)
                progressBar.isVisible = true
                it.prepare()
                it.play()
            }
        }
    }

    private fun selectTab(showEpisodes: Boolean) = with(headerBinding) {
        isShowingEpisodes = showEpisodes
        tvContent.isVisible = !showEpisodes
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
        tvNameMovie.isVisible = !fullscreen
        updateCurrentEpisodeLabel()
        scrollDetail.isVisible = !fullscreen
        mediaPlay.updateLayoutParams<ConstraintLayout.LayoutParams> {
            height = if (fullscreen) ConstraintLayout.LayoutParams.MATCH_CONSTRAINT else portraitPlayerHeight
            bottomToBottom = if (fullscreen) ConstraintLayout.LayoutParams.PARENT_ID
                else ConstraintLayout.LayoutParams.UNSET
        }
        mediaPlay.setFullscreenButtonState(fullscreen)
    }

    private fun releasePortraitOrientationLock() {
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT &&
            requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyFullscreen(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
        if (isFullscreen && deviceOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            canExitFullscreenOnPortrait = true
        } else if (!isFullscreen && deviceOrientation == Configuration.ORIENTATION_PORTRAIT) {
            releasePortraitOrientationLock()
        }
    }

    override fun backDevice() {
        persistContinueWatching()
        when {
            isFullscreen -> requestFullscreen(false)
            viewModel.goBack() -> Unit
            else -> super.backDevice()
        }
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
        val previous = renderedState
        val movieChanged = previous?.movieSlug != state.movieSlug
        // Reset and bind from the same snapshot, even when loading finishes before it is drawn.
        if (previous != null && movieChanged) resetMovieContent()
        renderedState = state

        if (movieChanged || previous.detail != state.detail) {
            when (val result = state.detail) {
                is Result.Success -> bindDetail(result.data)
                is Result.Error -> headerBinding.tvContent.setText(R.string.detail_load_error)
                Result.Idle, Result.Loading -> headerBinding.tvContent.setText(R.string.loading)
            }
        }
        if (movieChanged || previous.episodes != state.episodes) {
            when (val result = state.episodes) {
                is Result.Success -> {
                    val episodes = result.data
                    renderEpisodes()
                    val episode = episodes.firstOrNull {
                        it.id == selectedEpisode?.id && it.linkM3u8.isNotBlank()
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
                        val resumePosition = if (selectedEpisode == null && saved != null &&
                            episodes.getOrNull(saved.currentEpisode) == episode) saved.positionContinue else 0L
                        playEpisode(episode, resumePosition)
                    }
                    episodeAdapter.selectedEpisodeId = selectedEpisode?.id
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

    private fun prepareSelectedEpisode() {
        val episode = selectedEpisode ?: return
        val currentPlayer = player ?: return
        binding.mediaPlay.setCustomErrorMessage(null)
        binding.progressBar.isVisible = true
        currentPlayer.setMediaItem(
            MediaItem.Builder()
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
        player = ExoPlayer.Builder(this).build().also { currentPlayer ->
            currentPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true,
            )
            currentPlayer.setHandleAudioBecomingNoisy(true)
            currentPlayer.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    binding.mediaPlay.keepScreenOn = isPlaying
                    if (!isPlaying) persistContinueWatching()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    binding.progressBar.isVisible = playbackState == Player.STATE_BUFFERING
                    if (playbackState == Player.STATE_READY) binding.mediaPlay.setCustomErrorMessage(null)
                    if (playbackState == Player.STATE_ENDED) persistContinueWatching()
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
            })
            binding.mediaPlay.player = currentPlayer
        }
        prepareSelectedEpisode()
        progressSaveJob = lifecycleScope.launch {
            while (isActive) {
                delay(5_000)
                if (player?.isPlaying == true) persistContinueWatching()
            }
        }
        if (orientationListener.canDetectOrientation()) orientationListener.enable()
    }

    private fun showPlaybackMessage(@androidx.annotation.StringRes message: Int) {
        binding.progressBar.isVisible = false
        binding.mediaPlay.setCustomErrorMessage(getString(message))
    }

    private fun savePlaybackState() {
        player?.let {
            playbackPosition = it.currentPosition.coerceAtLeast(0L)
            playWhenReady = it.playWhenReady
        }
    }

    private fun persistContinueWatching() {
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
        progressSaveJob?.cancel()
        progressSaveJob = null
        persistContinueWatching()
        orientationListener.disable()
        savePlaybackState()
        binding.mediaPlay.player = null
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
        private const val STATE_POSITION = "detail_position"
        private const val STATE_PLAY_WHEN_READY = "detail_play_when_ready"

        fun createIntent(context: Context, movieSlug: String): Intent =
            Intent(context, DetailActivity::class.java).putExtra(EXTRA_MOVIE_SLUG, movieSlug)
    }
}
