package com.htdev.movie.htplay.home.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.htdev.movie.htplay.auth.login.LoginActivity
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.base.BaseFragment
import com.htdev.movie.htplay.databinding.FragmentSettingsBinding
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.language.LanguageActivity
import com.htdev.movie.htplay.player.InAppMiniPlayer
import com.htdev.movie.htplay.service.DownloadNotifications
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.htdev.movie.htplay.util.SharePref
import com.htdev.movie.htplay.util.launchRepeatOnStarted
import com.htdev.movie.htplay.widget.onClickListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import androidx.core.net.toUri


@AndroidEntryPoint
class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {
    @Inject lateinit var miniPlayer: InAppMiniPlayer

    private val settingViewModel : SettingViewModel by viewModels()
    private var updatingNotificationSwitch = false
    private val notificationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        setNotificationEnabled(DownloadNotifications.areAllowed(requireContext()))
    }
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val enabled = granted && DownloadNotifications.areAllowed(requireContext())
        setNotificationEnabled(enabled)
        if (!enabled && view != null) showNotificationSettings()
    }

    override fun createViewBinding(): FragmentSettingsBinding {
        return FragmentSettingsBinding.inflate(layoutInflater)
    }


    override fun initView() {
        binding.apply {
            swPip.isChecked = SharePref.init(requireContext()).isEnablePip()
            DownloadNotifications.createChannel(requireContext())
            swNotification.isChecked = SharePref.init(requireContext()).isEnableNotification() &&
                DownloadNotifications.areAllowed(requireContext())
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
                if (!updatingNotificationSwitch) {
                    if (isChecked) checkNotificationPermission() else setNotificationEnabled(false)
                }
            }
            swAutoNextEpisode.setOnCheckedChangeListener { _, isChecked ->
                SharePref.init(requireContext()).setAutoNextEpisode(isChecked)
            }
            swPip.setOnCheckedChangeListener { _, isChecked ->
                SharePref.init(requireContext()).enablePictureInPicture(isChecked)
                if (!isChecked) miniPlayer.close()
            }
            itemPolicy.onClickListener {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    "https://hoamzdev.github.io/htplay-privacy/privacy-policy.html?v=2".toUri()
                )
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (SharePref.init(requireContext()).isEnableNotification() &&
            !DownloadNotifications.areAllowed(requireContext())) {
            setNotificationEnabled(false)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            setNotificationEnabled(false)
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (!DownloadNotifications.areAllowed(requireContext())) {
            setNotificationEnabled(false)
            showNotificationSettings()
        } else {
            setNotificationEnabled(true)
        }
    }

    private fun setNotificationEnabled(enabled: Boolean) {
        SharePref.init(requireContext()).enableNotification(enabled)
        if (view != null) {
            updatingNotificationSwitch = true
            binding.swNotification.isChecked = enabled
            updatingNotificationSwitch = false
        }
    }

    private fun showNotificationSettings() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.notification_permission_title)
            .setMessage(R.string.notification_permission_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.notification_open_settings) { _, _ ->
                notificationSettingsLauncher.launch(DownloadNotifications.settingsIntent(requireContext()))
            }.show()
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
