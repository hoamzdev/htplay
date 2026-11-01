package com.htdev.movie.htplay.intro

import android.view.ViewOutlineProvider
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.htdev.movie.htplay.MainActivity
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.auth.login.LoginActivity
import com.htdev.movie.htplay.base.BaseActivity
import com.htdev.movie.htplay.databinding.ActivityIntroBinding
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.common.SessionExpiredException
import com.htdev.movie.htplay.widget.onClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class IntroActivity : BaseActivity<ActivityIntroBinding>() {

    override fun createViewBinding(): ActivityIntroBinding {
        return ActivityIntroBinding.inflate(layoutInflater)
    }
    private val introAdapter: IntroAdapter by lazy { IntroAdapter() }
    private var currentPosition: Int = 0
    private val introViewModel: IntroViewModel by viewModels()

    override fun initView() {
        setupViewPager()
        val windowBackground = window.decorView.background
        binding.apply {
            blurBottom.post {
                blurBottom.setupWith(blurTarget).setFrameClearDrawable(windowBackground)
                    .setBlurRadius(16f)
            }
            blurBottom.outlineProvider = ViewOutlineProvider.BACKGROUND
            blurBottom.clipToOutline = true
        }
    }

    private fun setupViewPager() {
        introAdapter.setListData(listIntro)
        binding.vpIntro.apply {
            adapter = introAdapter
            isUserInputEnabled = true
        }

        binding.vpIntro.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    currentPosition = binding.vpIntro.currentItem
                    handlePositonIntro(currentPosition)
                }
            }
        })

        currentPosition = binding.vpIntro.currentItem
        handlePositonIntro(currentPosition)
    }

    override fun handleClickEvent() {
        binding.btnStarted.onClickListener {
            if (currentPosition < listIntro.size - 1) {
                handlePositonIntro(++currentPosition)
            } else {
                lifecycleScope.launch {
                    binding.btnStarted.isEnabled = false
                    try {
                        when (val session = introViewModel.checkSessionLogin()) {
                            is Result.Success -> {
                                newActivity(dist = MainActivity::class.java)
                                finishAffinity()
                            }
                            is Result.Error -> {
                                if (session.throwable is SessionExpiredException) {
                                    newActivity(dist = LoginActivity::class.java)
                                    finishAffinity()
                                } else {
                                    showToastApp(session.throwable.message.orEmpty())
                                }
                            }
                            else -> Unit
                        }
                    } finally {
                        binding.btnStarted.isEnabled = true
                    }
                }
            }
        }
    }

    private fun handlePositonIntro(position: Int) {
        binding.apply {
            dots.positionSelected(position + 1)
            tvIntro1.setText(listIntro[position].intro1)
            tvIntro2.setText(listIntro[position].intro2)
            btnStarted.setText(if (position < listIntro.size - 1) R.string.next else R.string.let_started)
        }
    }

    private val listIntro = listOf(
        IntroItem(
            R.drawable.img_intro_1,
            R.string.intro_title_1,
            R.string.intro_description_1
        ),
        IntroItem(
            R.drawable.img_intro_2,
            R.string.intro_title_2,
            R.string.intro_description_2
        ),
        IntroItem(
            R.drawable.img_intro_3,
            R.string.intro_title_3,
            R.string.intro_description_3
        ),
        IntroItem(
            R.drawable.img_intro_4,
            R.string.intro_title_4,
            R.string.intro_description_4
        )
    )

}
