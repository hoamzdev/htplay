package com.htdev.movie.htplay.language

import com.htdev.movie.htplay.widget.onClickListener

import android.app.Dialog
import android.graphics.Color
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.activity.viewModels
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.base.BaseActivity
import com.htdev.movie.htplay.databinding.ActivityLanguageBinding
import com.htdev.movie.htplay.intro.IntroActivity
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class LanguageActivity : BaseActivity<ActivityLanguageBinding>(){
    private val languageViewModel : LanguageViewModel by viewModels()
    private val languageAdapter : LanguageAdapter by lazy { LanguageAdapter{ code ->
        handleSelectedLanguage(code)
    } }

    private var translateDialog : Dialog? = null
    private var changeLanguageJob: Job? = null

    override fun createViewBinding(): ActivityLanguageBinding {
        return ActivityLanguageBinding.inflate(layoutInflater)
    }

    override fun initView() {
        binding.apply {
            rcLanguages.apply {
                adapter = languageAdapter
                setHasFixedSize(true)
                itemAnimator = null
            }
        }
        LanguageHelper.applyLocaleTemporarily(languageViewModel.selectedLanguageCode)
        updateLanguageUi()
    }

    private fun showTranslating(){
        if (isFinishing || isDestroyed || translateDialog?.isShowing == true) return

        translateDialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.layout_dialog_translating)
            val localized = LanguageHelper.localizedContext(
                this@LanguageActivity, languageViewModel.selectedLanguageCode
            )
            findViewById<TextView>(R.id.tv_translating).text = localized.getString(R.string.language_translating)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            setOnDismissListener { translateDialog = null }
            show()
            window?.apply {
                setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                setLayout(
                    (resources.displayMetrics.widthPixels * 0.85f).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        }
        handleChangeLanguage()
    }

    private fun handleChangeLanguage(){
        binding.btnSaveLanguage.isEnabled = false
        changeLanguageJob = lifecycleScope.launch {
            try {
                delay(1_500.milliseconds)
                LanguageHelper.applyLocaleTemporarily(languageViewModel.selectedLanguageCode)
                updateLanguageUi()
            } finally {
                translateDialog?.dismiss()
                binding.btnSaveLanguage.isEnabled = true
                changeLanguageJob = null
            }
        }
    }

    private fun updateLanguageUi() {
        val localized = LanguageHelper.localizedContext(this, languageViewModel.selectedLanguageCode)
        binding.tvChooseLanguage.text = localized.getString(R.string.language_choose)
        binding.btnSaveLanguage.text = localized.getString(R.string.language_save)
        binding.root.layoutDirection = localized.resources.configuration.layoutDirection
    }

    override fun handleClickEvent() {
        binding.btnSaveLanguage.onClickListener {
            if (changeLanguageJob?.isActive == true) return@onClickListener
            LanguageHelper.saveLocale(this, languageViewModel.selectedLanguageCode)
            newActivity(IntroActivity::class.java)
            finish()
        }
    }

    override fun observerData() {
        languageAdapter.submitList(languageViewModel.fetchListLanguage()) {
            languageAdapter.selectLanguage(languageViewModel.selectedLanguageCode)
        }
    }

    private fun handleSelectedLanguage(code : String){
        languageViewModel.selectLanguage(code)
        showTranslating()
    }

    override fun onDestroy() {
        changeLanguageJob?.cancel()
        translateDialog?.dismiss()
        LanguageHelper.clearTemporaryLocale()
        LanguageHelper.refreshApplicationLanguage(this)
        super.onDestroy()
    }
}
