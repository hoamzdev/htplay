package com.htdev.movie.htplay.home.settings

import com.htdev.movie.htplay.base.BaseFragment
import com.htdev.movie.htplay.databinding.FragmentSettingsBinding


class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {

    override fun createViewBinding(): FragmentSettingsBinding {
        return FragmentSettingsBinding.inflate(layoutInflater)
    }



}