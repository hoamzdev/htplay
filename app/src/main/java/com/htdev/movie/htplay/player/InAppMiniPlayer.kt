package com.htdev.movie.htplay.player

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.ui.PlayerView
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.databinding.MiniPlayerOverlayBinding
import com.htdev.movie.htplay.detail.DetailActivity
import com.htdev.movie.htplay.detail.DetailUiState
import com.htdev.movie.htplay.di.ApplicationScope
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.entities.ContinueWatching
import com.htdev.movie.htplay.domain.entities.MoviePlayer
import com.htdev.movie.htplay.domain.usecase.movie.SaveContinueWatching
import com.htdev.movie.htplay.home.HomeRouterActivity
import com.htdev.movie.htplay.home.home.ShowAllMovieActivity
import com.htdev.movie.htplay.language.LanguageActivity
import com.htdev.movie.htplay.search.SearchActivity
import com.htdev.movie.htplay.util.SharePref
import com.htdev.movie.htplay.watching.WatchingActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Owns the live player only while the detail screen is minimized. */
@androidx.annotation.OptIn(UnstableApi::class)
data class MiniPlayback(
    val player: ExoPlayer,
    val movieSlug: String,
    val title: String,
    val episode: MoviePlayer,
    val state: DetailUiState?,
    val downloadId: String?,
    val downloadRequest: DownloadRequest?,
    val isOffline: Boolean,
    val id: String = UUID.randomUUID().toString(),
    val expandBounds: RectF? = null,
)

@Singleton
@androidx.annotation.OptIn(UnstableApi::class)
class InAppMiniPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val saveContinueWatching: SaveContinueWatching,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    private var playback: MiniPlayback? = null
    private var overlay: MiniPlayerOverlayBinding? = null
    private var host: Activity? = null
    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var progressJob: Job? = null
    private var pendingSave: Job? = null
    private var lastSavedProgress: ContinueWatching? = null
    private var resumeAfterBackground = false
    private var horizontalPosition = 1f
    private var verticalPosition = 1f
    private var pendingEnterBounds: RectF? = null
    private var systemPipActive = false
    private var systemPipWasActive = false
    private var normalLayoutParams: FrameLayout.LayoutParams? = null
    private var stopEnterAnimation: (() -> Unit)? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            updateControls()
            updateSystemPip()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying) persistProgress()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState != Player.STATE_ENDED) return
            persistProgress()
            val current = playback ?: return
            if (current.isOffline || !SharePref.init(context).isNextEpisode()) return
            val episodes = (current.state?.episodes as? Result.Success)?.data ?: return
            val index = episodes.indexOf(current.episode)
            if (index < 0) return
            val next = episodes.drop(index + 1).firstOrNull { it.linkM3u8.isNotBlank() } ?: return
            playback = current.copy(episode = next)
            current.player.setMediaItem(MediaItem.Builder()
                .setMediaId(next.id)
                .setUri(next.linkM3u8.trim())
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .build())
            current.player.prepare()
            current.player.play()
        }
    }

    fun show(session: MiniPlayback, source: PlayerView) {
        close()
        playback = session
        if (source.width > 0 && source.height > 0) {
            val location = IntArray(2)
            source.getLocationOnScreen(location)
            pendingEnterBounds = RectF(
                location[0].toFloat(), location[1].toFloat(),
                (location[0] + source.width).toFloat(), (location[1] + source.height).toFloat(),
            )
        }
        PlayerView.switchTargetView(session.player, source, null)
        session.player.addListener(listener)
        progressJob = uiScope.launch {
            while (isActive) {
                delay(5_000)
                if (playback?.player?.isPlaying == true) persistProgress()
            }
        }
    }

    /** Transfer ownership back to DetailActivity without preparing or seeking again. */
    fun take(id: String?): MiniPlayback? {
        val current = playback?.takeIf { id != null && it.id == id } ?: return null
        persistProgress()
        detach()
        progressJob?.cancel()
        progressJob = null
        current.player.removeListener(listener)
        playback = null
        pendingEnterBounds = null
        resumeAfterBackground = false
        lastSavedProgress = null
        return current
    }

    fun attach(activity: Activity) {
        val current = playback ?: return
        if (activity is DetailActivity) return
        val supported = activity is HomeRouterActivity || activity is ShowAllMovieActivity ||
            activity is SearchActivity || activity is WatchingActivity || activity is LanguageActivity
        if (!supported || !SharePref.init(context).isEnablePip()) {
            close()
            return
        }
        if (host === activity) {
            if (!activity.isInPictureInPictureMode) systemPipWasActive = false
            updateSystemPip()
            return
        }
        detach()
        val parent = activity.findViewById<FrameLayout>(android.R.id.content) ?: return
        val view = MiniPlayerOverlayBinding.inflate(LayoutInflater.from(activity), parent, false)
        val enterBounds = pendingEnterBounds
        pendingEnterBounds = null
        overlay = view
        host = activity
        val width = minOf(dp(224), activity.resources.displayMetrics.widthPixels - dp(24))
        val bottomMargin = dp(if (activity is HomeRouterActivity) 76 else 16)
        view.root.background = GradientDrawable().apply {
            setColor(Color.BLACK)
            cornerRadius = dp(10).toFloat()
        }
        view.root.clipToOutline = true
        view.root.elevation = dp(8).toFloat()
        view.root.alpha = if (enterBounds != null) 0f else 1f
        parent.addView(view.root, FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.END).apply {
            marginEnd = dp(12)
            this.bottomMargin = bottomMargin
        })
        normalLayoutParams = FrameLayout.LayoutParams(view.root.layoutParams as FrameLayout.LayoutParams)
        enableDragging(view, parent, bottomMargin, enterBounds)
        view.miniVideo.player = current.player
        view.miniTitle.text = current.title
        view.miniVideo.setOnClickListener { expand(activity) }
        view.miniTitle.setOnClickListener { expand(activity) }
        view.miniClose.setOnClickListener { close() }
        view.miniPlayPause.setOnClickListener {
            val player = playback?.player ?: return@setOnClickListener
            if (player.playWhenReady && player.playerError == null &&
                player.playbackState != Player.STATE_ENDED) player.pause() else {
                if (player.playerError != null) player.prepare()
                if (player.playbackState == Player.STATE_ENDED) player.seekTo(0L)
                player.play()
            }
        }
        updateControls()
        updateSystemPip()
    }

    fun onUserLeaveHint(activity: Activity) {
        if (host !== activity) return
        SystemPictureInPicture.onUserLeaveHint(activity, playback?.player, overlay?.miniVideo)
    }

    fun enterSystemPip(activity: Activity): Boolean =
        host === activity && SystemPictureInPicture.enter(activity, playback?.player, overlay?.miniVideo)

    fun onSystemPipChanged(activity: Activity, inPip: Boolean) {
        if (host !== activity) return
        val view = overlay ?: return
        stopEnterAnimation?.invoke()
        systemPipActive = inPip
        if (inPip) systemPipWasActive = true
        view.miniControls.isVisible = !inPip
        view.root.clipToOutline = !inPip
        (view.root.background as? GradientDrawable)?.cornerRadius = if (inPip) 0f else dp(10).toFloat()
        view.root.translationX = 0f
        view.root.translationY = 0f
        view.root.layoutParams = if (inPip) {
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.TOP or Gravity.START)
        } else normalLayoutParams ?: return
        view.miniVideoContainer.layoutParams = view.miniVideoContainer.layoutParams.apply {
            height = if (inPip) ViewGroup.LayoutParams.MATCH_PARENT else dp(126)
        }
    }

    fun onActivityStopped(activity: Activity) {
        if (host !== activity) return
        if (systemPipActive || systemPipWasActive || activity.isInPictureInPictureMode) close() else detach(activity)
    }

    private fun updateSystemPip() {
        val activity = host ?: return
        SystemPictureInPicture.update(activity, playback?.player, overlay?.miniVideo)
    }

    private fun enableDragging(
        view: MiniPlayerOverlayBinding,
        parent: FrameLayout,
        bottomMargin: Int,
        enterBounds: RectF?,
    ) {
        val root = view.root
        val touchSlop = ViewConfiguration.get(root.context).scaledTouchSlop
        var downX = 0f
        var downY = 0f
        var startX = 0f
        var startY = 0f
        var dragging = false
        var animationPending = enterBounds != null
        var entering = false

        fun bounds(): RectF {
            val insets = ViewCompat.getRootWindowInsets(root)
            val bars = insets?.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val keyboard = insets?.getInsets(WindowInsetsCompat.Type.ime())
            val left = (bars?.left ?: 0) + dp(12).toFloat()
            val top = (bars?.top ?: 0) + dp(12).toFloat()
            return RectF(
                left, top,
                maxOf(left, parent.width - root.width - (bars?.right ?: 0) - dp(12).toFloat()),
                maxOf(top, parent.height - root.height - bottomMargin -
                    maxOf(bars?.bottom ?: 0, keyboard?.bottom ?: 0).toFloat()),
            )
        }

        fun restorePosition() {
            if (dragging || entering || systemPipActive || overlay !== view || root.width == 0 || parent.height == 0) return
            val area = bounds()
            root.x = area.left + area.width() * horizontalPosition
            root.y = area.top + area.height() * verticalPosition
            if (!animationPending) root.alpha = 1f
        }

        fun finishEnterAnimation() {
            animationPending = false
            entering = false
            root.animate().withEndAction(null).cancel()
            view.miniControls.animate().cancel()
            root.scaleX = 1f
            root.scaleY = 1f
            view.miniControls.alpha = 1f
            restorePosition()
        }
        stopEnterAnimation = ::finishEnterAnimation

        fun shouldDismiss(): Boolean {
            val insets = ViewCompat.getRootWindowInsets(root)
            val bottom = maxOf(
                insets?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom ?: 0,
                insets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0,
            )
            return root.y - startY >= dp(48) &&
                root.y + root.height / 2f >= parent.height - bottom - dp(48)
        }

        val touch = View.OnTouchListener { touched, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (systemPipActive) return@OnTouchListener false
                    if (entering || animationPending) finishEnterAnimation()
                    downX = event.rawX
                    downY = event.rawY
                    startX = root.x
                    startY = root.y
                    dragging = false
                    parent.requestDisallowInterceptTouchEvent(true)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (abs(dx) > touchSlop || abs(dy) > touchSlop) dragging = true
                    if (dragging) {
                        val area = bounds()
                        root.x = (startX + dx).coerceIn(area.left, area.right)
                        // Allow the card past its resting bounds so dragging to the bottom dismisses it.
                        root.y = (startY + dy).coerceIn(area.top, maxOf(area.top, parent.height - dp(24).toFloat()))
                        root.alpha = if (shouldDismiss()) 0.5f else 1f
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    parent.requestDisallowInterceptTouchEvent(false)
                    val wasDragging = dragging
                    dragging = false
                    if (wasDragging && shouldDismiss()) {
                        close()
                    } else if (wasDragging) {
                        val area = bounds()
                        horizontalPosition = ((root.x - area.left) / area.width().coerceAtLeast(1f)).coerceIn(0f, 1f)
                        verticalPosition = ((root.y - area.top) / area.height().coerceAtLeast(1f)).coerceIn(0f, 1f)
                        restorePosition()
                        updateSystemPip()
                    } else {
                        touched.performClick()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    parent.requestDisallowInterceptTouchEvent(false)
                    dragging = false
                    restorePosition()
                    true
                }
                else -> false
            }
        }
        view.miniVideo.setOnTouchListener(touch)
        view.miniTitle.setOnTouchListener(touch)
        root.setOnTouchListener(touch)
        root.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            restorePosition()
            if (!systemPipActive) updateSystemPip()
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            root.post { restorePosition() }
            insets
        }
        ViewCompat.requestApplyInsets(root)
        root.doOnLayout {
            if (overlay !== view) return@doOnLayout
            if (systemPipActive) {
                finishEnterAnimation()
                return@doOnLayout
            }
            restorePosition()
            if (!animationPending || enterBounds == null || root.width == 0 || view.miniVideo.height == 0) {
                finishEnterAnimation()
                return@doOnLayout
            }
            val targetX = root.x
            val targetY = root.y
            val parentLocation = IntArray(2)
            parent.getLocationOnScreen(parentLocation)
            entering = true
            animationPending = false
            root.pivotX = 0f
            root.pivotY = 0f
            root.scaleX = enterBounds.width() / root.width
            root.scaleY = enterBounds.height() / view.miniVideo.height
            root.x = enterBounds.left - parentLocation[0]
            root.y = enterBounds.top - parentLocation[1]
            root.alpha = 1f
            view.miniControls.alpha = 0f
            root.animate()
                .x(targetX).y(targetY)
                .scaleX(1f).scaleY(1f)
                .setDuration(280L)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    entering = false
                    restorePosition()
                    updateSystemPip()
                }
                .start()
            view.miniControls.animate().alpha(1f).setStartDelay(160L).setDuration(120L).start()
        }
    }

    fun detach(activity: Activity? = null) {
        if (activity != null && host !== activity) return
        overlay?.let {
            it.root.animate().withEndAction(null).cancel()
            it.miniControls.animate().cancel()
            it.miniVideo.player = null
            it.root.keepScreenOn = false
            (it.root.parent as? ViewGroup)?.removeView(it.root)
        }
        host?.takeUnless { it.isDestroyed }?.let { SystemPictureInPicture.update(it, null, null, enabled = false) }
        stopEnterAnimation = null
        normalLayoutParams = null
        systemPipActive = false
        systemPipWasActive = false
        overlay = null
        host = null
    }

    fun onAppBackground() {
        if (systemPipActive || host?.isInPictureInPictureMode == true) return
        val current = playback ?: return
        resumeAfterBackground = current.player.playWhenReady
        persistProgress()
        current.player.pause()
    }

    fun onAppForeground() {
        if (resumeAfterBackground) playback?.player?.play()
        resumeAfterBackground = false
    }

    fun close() {
        val current = playback ?: return
        persistProgress()
        detach()
        progressJob?.cancel()
        progressJob = null
        current.player.removeListener(listener)
        playback = null
        pendingEnterBounds = null
        current.player.release()
        resumeAfterBackground = false
        lastSavedProgress = null
    }

    @Suppress("DEPRECATION")
    private fun expand(activity: Activity) {
        val current = playback ?: return
        stopEnterAnimation?.invoke()
        val video = overlay?.miniVideo
        val location = IntArray(2)
        video?.getLocationOnScreen(location)
        val bounds = video?.takeIf { it.width > 0 && it.height > 0 }?.let {
            RectF(location[0].toFloat(), location[1].toFloat(),
                (location[0] + it.width).toFloat(), (location[1] + it.height).toFloat())
        }
        playback = current.copy(expandBounds = bounds)
        SystemPictureInPicture.update(activity, null, null, enabled = false)
        val intent = current.downloadId?.let { DetailActivity.createDownloadIntent(activity, it) }
            ?: DetailActivity.createIntent(activity, current.movieSlug)
        activity.startActivity(intent.putExtra(DetailActivity.EXTRA_MINI_PLAYER_ID, current.id))
        activity.overridePendingTransition(0, 0)
    }

    private fun updateControls() {
        val current = playback ?: return
        overlay?.apply {
            val playing = current.player.playWhenReady && current.player.playerError == null &&
                current.player.playbackState != Player.STATE_ENDED
            miniPlayPause.setImageResource(if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
            miniPlayPause.contentDescription = root.context.getString(if (playing) R.string.mini_player_pause else R.string.mini_player_play)
            root.keepScreenOn = current.player.isPlaying
        }
    }

    private fun persistProgress() {
        val current = playback ?: return
        if (current.isOffline) return
        val movie = (current.state?.detail as? Result.Success)?.data ?: return
        val episodes = (current.state?.episodes as? Result.Success)?.data ?: return
        val index = episodes.indexOf(current.episode)
        val position = current.player.currentPosition
        val duration = current.player.duration
        if (index < 0 || position < 1_000 || duration <= 0) return
        val progress = ContinueWatching(
            name = movie.name,
            slugMovie = current.movieSlug,
            thumbnail = movie.thumbUrl.ifBlank { movie.posterUrl },
            genre = movie.genreSlug.ifBlank { movie.genre },
            currentEpisode = index,
            lengthCurrentEpisode = duration,
            positionContinue = position.coerceAtMost(duration),
        )
        if (progress == lastSavedProgress) return
        lastSavedProgress = progress
        val previous = pendingSave
        pendingSave = applicationScope.launch {
            previous?.join()
            try {
                saveContinueWatching(progress)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("MiniPlayer", "Could not save playback progress", error)
            }
        }
    }

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()
}
