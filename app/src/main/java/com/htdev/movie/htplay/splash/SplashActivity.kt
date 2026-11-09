package com.htdev.movie.htplay.splash

import android.annotation.SuppressLint
import android.view.Choreographer
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import com.htdev.movie.htplay.base.BaseActivity
import com.htdev.movie.htplay.databinding.ActivitySplashBinding
import com.htdev.movie.htplay.intro.IntroActivity
import com.htdev.movie.htplay.language.LanguageActivity
import com.htdev.movie.htplay.util.launchRepeatOnStarted
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.time.Duration.Companion.milliseconds


@AndroidEntryPoint
@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity<ActivitySplashBinding>() {

    override fun createViewBinding(): ActivitySplashBinding {
        return ActivitySplashBinding.inflate(layoutInflater)
    }

    private val splashViewModel: SplashViewModel by viewModels()
    private val splashAdapter: AnimPosterSplashAdapter by lazy { AnimPosterSplashAdapter() }
    private val splashAdapter1: AnimPosterSplashAdapter by lazy { AnimPosterSplashAdapter() }
    private val splashAdapter2: AnimPosterSplashAdapter by lazy { AnimPosterSplashAdapter() }

    private var lastFrameNanos = 0L
    private var pendingScrollPx = 0f
    private val scrollFrame = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (lastFrameNanos != 0L) {
                // Keep a constant dp/second speed across screen refresh rates.
                val seconds = ((frameTimeNanos - lastFrameNanos) / 1_000_000_000f)
                    .coerceAtMost(0.05f)
                pendingScrollPx += SCROLL_SPEED_DP_PER_SECOND * resources.displayMetrics.density * seconds
                val distance = pendingScrollPx.toInt()
                pendingScrollPx -= distance
                binding.rcDemo1.scrollBy(distance, 0)
                binding.rcDemo2.scrollBy(-distance, 0)
                binding.rcDemo3.scrollBy(distance, 0)
            }
            lastFrameNanos = frameTimeNanos
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun onResume() {
        super.onResume()
        lastFrameNanos = 0L
        Choreographer.getInstance().postFrameCallback(scrollFrame)
        navToIntro()
    }

    private fun navToIntro(){
        launchRepeatOnStarted {
            delay(5000.milliseconds)
            withContext(Dispatchers.Main){
                newActivity(dist = LanguageActivity::class.java)
                finishAffinity()
            }
        }
    }

    override fun onPause() {
        Choreographer.getInstance().removeFrameCallback(scrollFrame)
        lastFrameNanos = 0L
        super.onPause()
    }

    override fun initView() {
        binding.root.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            // A screen diagonal keeps both ends outside the viewport after rotation.
            val rowWidth = ceil(hypot((right - left).toDouble(), (bottom - top).toDouble())).toInt()
            if (rowWidth > 0 && binding.posterRows.layoutParams.width != rowWidth) {
                binding.posterRows.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    width = rowWidth
                }
            }
        }
        setupAnimLogo()
        binding.rcDemo1.apply {
            adapter = splashAdapter
            setItemViewCacheSize(3)
            layoutManager =
                LinearLayoutManager(this@SplashActivity, LinearLayoutManager.HORIZONTAL, false)
        }
        binding.rcDemo2.apply {
            adapter = splashAdapter1
            setItemViewCacheSize(3)
            layoutManager =
                LinearLayoutManager(this@SplashActivity, LinearLayoutManager.HORIZONTAL, false)
        }
        binding.rcDemo3.apply {
            adapter = splashAdapter2
            setItemViewCacheSize(3)
            layoutManager =
                LinearLayoutManager(this@SplashActivity, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun setupAnimLogo() {
        binding.apply {
            logoApp.alpha = 0f
            nameLogo.alpha = 0f
            root.doOnPreDraw {
                logoApp.animate().alpha(1f).setDuration(1000).start()
                nameLogo.animate().alpha(1f).setDuration(1500).start()
            }
        }
    }

    override fun initViewModel() {
        splashViewModel.getListSplashAnim().let {
            splashAdapter.submitList(it)
            splashAdapter1.submitList(it.shuffled())
            splashAdapter2.submitList(it.shuffled())
            binding.rcDemo1.scrollToPosition(splashAdapter.loopStartPosition)
            binding.rcDemo2.scrollToPosition(splashAdapter1.loopStartPosition)
            binding.rcDemo3.scrollToPosition(splashAdapter2.loopStartPosition)
        }
    }

    companion object {
        private const val SCROLL_SPEED_DP_PER_SECOND = 30f
    }
}
