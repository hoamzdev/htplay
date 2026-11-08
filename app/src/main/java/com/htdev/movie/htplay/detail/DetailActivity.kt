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
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.adapter.MovieAdapter
import com.htdev.movie.htplay.base.BaseActivity
import com.htdev.movie.htplay.databinding.ActivityDetailBinding
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.entities.DataMovieDetail
import com.htdev.movie.htplay.domain.entities.MoviePlayer
import com.htdev.movie.htplay.domain.entities.PosterMovie
import com.htdev.movie.htplay.entities.ItemMovie
import com.htdev.movie.htplay.util.getCountryName
import com.htdev.movie.htplay.util.getGenreName
import com.htdev.movie.htplay.util.launchRepeatOnStarted
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.UUID

@AndroidEntryPoint
@androidx.annotation.OptIn(UnstableApi::class)
class DetailActivity : BaseActivity<ActivityDetailBinding>() {
    private val viewModel: DetailViewModel by viewModels()
    private val episodeAdapter = EpisodeAdapter()
    private val moreLikeThisAdapter = MovieAdapter(onClick = { movie ->
        startActivity(createIntent(this, movie.slug))
    })
    private val countryMovieAdapter = MovieAdapter(onClick = { movie ->
        startActivity(createIntent(this, movie.slug))
    })
    private var isContentExpanded = false
    private var player: ExoPlayer? = null
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
        binding.rcEpisodes.adapter = episodeAdapter
        binding.rcMoreLikeThis.adapter = moreLikeThisAdapter
        binding.rcMoreLikeThisCountry.adapter = countryMovieAdapter
        binding.tvNameMovie.text = ""
        binding.tvStar.text = ""
        binding.tvYear.text = ""
        binding.tvCountry.text = ""
        binding.tvGenre.text = ""
        binding.tvMore.isVisible = false
        binding.rcMoreLikeThis.isVisible = false
        binding.tvMoreCountry.isVisible = false
        binding.rcMoreLikeThisCountry.isVisible = false
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
        tvAbout.setOnClickListener { selectTab(showEpisodes = false) }
        tvEpisodes.setOnClickListener { selectTab(showEpisodes = true) }
        tvContent.setOnClickListener {
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

    private fun selectTab(showEpisodes: Boolean) = with(binding) {
        tvContent.isVisible = !showEpisodes
        rcEpisodes.isVisible = showEpisodes
        tvAbout.isSelected = !showEpisodes
        tvEpisodes.isSelected = showEpisodes
        val aboutColor = getColor(if (showEpisodes) R.color.white else R.color.secondary_color)
        val episodesColor = getColor(if (showEpisodes) R.color.secondary_color else R.color.white)
        tvAbout.setTextColor(aboutColor)
        tvEpisodes.setTextColor(episodesColor)
        vLandAbout.setBackgroundColor(aboutColor)
        vLandEpisodes.setBackgroundColor(episodesColor)
    }

    private fun updateContentExpansion() = with(binding.tvContent) {
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
        if (isFullscreen) requestFullscreen(false) else super.backDevice()
    }

    override fun observerData() {
        launchRepeatOnStarted {
            launch {
                viewModel.detail.collect { result ->
                    when (result) {
                        is Result.Success -> bindDetail(result.data)
                        is Result.Error -> binding.tvContent.setText(R.string.detail_load_error)
                        Result.Idle, Result.Loading -> binding.tvContent.setText(R.string.loading)
                    }
                }
            }
            launch {
                viewModel.episodes.collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val episodes = result.data
                            episodeAdapter.submitList(episodes)
                            val episode = episodes.firstOrNull {
                                it.id == selectedEpisode?.id && it.linkM3u8.isNotBlank()
                            } ?: episodes.firstOrNull { it.linkM3u8.isNotBlank() }
                            if (episode == null) {
                                selectedEpisode = null
                                player?.stop()
                                player?.clearMediaItems()
                                showPlaybackMessage(R.string.detail_no_playable_episodes)
                            } else if (episode != selectedEpisode) {
                                playEpisode(episode)
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
            }
            launch {
                viewModel.moreLikeThis.collect { result ->
                    val movies = (result as? Result.Success)?.data.orEmpty()
                    moreLikeThisAdapter.submitList(movies.toMovieItems())
                    binding.tvMore.isVisible = movies.isNotEmpty()
                    binding.rcMoreLikeThis.isVisible = movies.isNotEmpty()
                }
            }
            launch {
                viewModel.countryMovies.collect { result ->
                    val movies = (result as? Result.Success)?.data.orEmpty()
                    countryMovieAdapter.submitList(movies.toMovieItems())
                    binding.tvMoreCountry.isVisible = movies.isNotEmpty()
                    binding.rcMoreLikeThisCountry.isVisible = movies.isNotEmpty()
                }
            }
        }
    }

    private fun bindDetail(movie: DataMovieDetail) = with(binding) {
        tvNameMovie.text = movie.name
        tvStar.text = movie.vote
        tvYear.text = movie.year.takeIf { it > 0 }?.toString().orEmpty()
        tvCountry.text = getCountryName(movie.countrySlug)
        tvMoreCountry.text = getString(R.string.detail_more_from_country, getCountryName(movie.countrySlug))
        tvGenre.text = getGenreName(movie.genreSlug.ifBlank { movie.genre })
        tvContent.text = HtmlCompat.fromHtml(movie.content, HtmlCompat.FROM_HTML_MODE_COMPACT).trim()
    }

    private fun List<PosterMovie>.toMovieItems(): List<ItemMovie> = map { movie ->
        ItemMovie(
            id = UUID.nameUUIDFromBytes(movie.id.ifBlank { movie.slug }.toByteArray(Charsets.UTF_8)),
            urlPoster = movie.posterUrl.ifBlank { movie.thumbUrl },
            nameFile = movie.name,
            slug = movie.slug,
        )
    }

    private fun playEpisode(episode: MoviePlayer) {
        if (episode.linkM3u8.isBlank()) return
        val currentPlayer = player
        if (selectedEpisode == episode && currentPlayer?.currentMediaItem?.mediaId == episode.id &&
            currentPlayer.playerError == null) {
            currentPlayer.play()
            return
        }
        selectedEpisode = episode
        episodeAdapter.selectedEpisodeId = episode.id
        playbackPosition = 0L
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
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    binding.progressBar.isVisible = playbackState == Player.STATE_BUFFERING
                    if (playbackState == Player.STATE_READY) binding.mediaPlay.setCustomErrorMessage(null)
                }

                override fun onPlayerError(error: PlaybackException) {
                    showPlaybackMessage(R.string.detail_playback_error)
                }
            })
            binding.mediaPlay.player = currentPlayer
        }
        prepareSelectedEpisode()
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

    override fun onSaveInstanceState(outState: Bundle) {
        savePlaybackState()
        outState.putParcelable(STATE_EPISODE, selectedEpisode)
        outState.putLong(STATE_POSITION, playbackPosition)
        outState.putBoolean(STATE_PLAY_WHEN_READY, playWhenReady)
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        orientationListener.disable()
        savePlaybackState()
        binding.mediaPlay.player = null
        player?.release()
        player = null
        binding.mediaPlay.keepScreenOn = false
        super.onStop()
    }

    override fun onDestroy() {
        binding.rcEpisodes.adapter = null
        binding.rcMoreLikeThis.adapter = null
        binding.rcMoreLikeThisCountry.adapter = null
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
