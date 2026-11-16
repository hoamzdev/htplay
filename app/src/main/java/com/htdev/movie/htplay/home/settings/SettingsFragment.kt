package com.htdev.movie.htplay.home.settings

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.viewModels
import com.htdev.movie.htplay.auth.login.LoginActivity
import com.htdev.movie.htplay.base.BaseFragment
import com.htdev.movie.htplay.databinding.FragmentSettingsBinding
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.language.LanguageActivity
import com.htdev.movie.htplay.player.InAppMiniPlayer
import com.htdev.movie.htplay.util.SharePref
import com.htdev.movie.htplay.util.launchRepeatOnStarted
import com.htdev.movie.htplay.widget.onClickListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest


@AndroidEntryPoint
class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {
    @Inject lateinit var miniPlayer: InAppMiniPlayer

    private val settingViewModel : SettingViewModel by viewModels()

    override fun createViewBinding(): FragmentSettingsBinding {
        return FragmentSettingsBinding.inflate(layoutInflater)
    }


    override fun initView() {
        binding.apply {
            swPip.isChecked = SharePref.init(requireContext()).isEnablePip()
            swNotification.isChecked = SharePref.init(requireContext()).isEnableNotification()
            swAutoNextEpisode.isChecked = SharePref.init(requireContext()).isNextEpisode()
        }
    }

    override fun bindViewModel() {
        launchRepeatOnStarted {
            settingViewModel.stateUser.collectLatest { state ->
                when(state){
                    is Result.Success -> {
                        val intent = Intent(requireActivity(),LoginActivity::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                    }
                    else -> Unit
                }
            }
        }
    }

    override fun handleClickEvent() {
        binding.apply {
            itemLanguage.onClickListener {
                val intent = Intent(requireActivity(), LanguageActivity::class.java).apply {
                    putExtra("LANGUAGE_SETTING", 1712)
                }
                startActivity(intent)
            }
            itemShare.onClickListener {
                //pending
                share()
            }
            itemLogout.onClickListener {
                //pending
                settingViewModel.handleLogoutUser()
            }
            swNotification.setOnCheckedChangeListener { _, isChecked ->
                SharePref.init(requireContext()).enableNotification(isChecked)
            }
            swAutoNextEpisode.setOnCheckedChangeListener { _, isChecked ->
                SharePref.init(requireContext()).setAutoNextEpisode(isChecked)
            }
            swPip.setOnCheckedChangeListener { _, isChecked ->
                SharePref.init(requireContext()).enablePictureInPicture(isChecked)
                if (!isChecked) miniPlayer.close()
            }
            itemDownload.onClickListener {
                //pending
            }
            itemPolicy.onClickListener {
                //pending
            }
        }
    }

    private fun share(){
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "HTPlay")
            putExtra(Intent.EXTRA_TEXT, "https://apkpure.com/htplay/com.htdev.movie.htplay")
        }
        startActivity(Intent.createChooser(intent, "Share via"))
    }
}
