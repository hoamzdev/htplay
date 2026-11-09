package com.htdev.movie.htplay.language

import android.content.Context
import androidx.lifecycle.ViewModel
import com.htdev.movie.htplay.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject


@HiltViewModel
class LanguageViewModel @Inject constructor(
    @ApplicationContext context: Context
) : ViewModel() {
    var selectedLanguageCode: String = LanguageHelper.getSavedLocale(context)
        private set

    fun selectLanguage(code: String) {
        if (languages.any { it.code == code }) {
            selectedLanguageCode = code
        }
    }

    fun fetchListLanguage(): List<LanguageModel> = languages

    private val languages by lazy {
        listOf(
            LanguageModel(
                name = "English",
                code = "en",
                image = R.drawable.iv_lan_english
            ),
            LanguageModel(
                name = "Tiếng Việt",
                code = "vi",
                image = R.drawable.iv_lan_vietnam
            ),
            LanguageModel(
                name = "Français",
                code = "fr",
                image = R.drawable.iv_lan_french
            ),
            LanguageModel(
                name = "Deutsch",
                code = "de",
                image = R.drawable.iv_lan_german
            ),
            LanguageModel(
                name = "Italiano",
                code = "it",
                image = R.drawable.iv_lan_italia
            ),
            LanguageModel(
                name = "中文",
                code = "zh",
                image = R.drawable.iv_lan_china
            ),
            LanguageModel(
                name = "日本語",
                code = "ja",
                image = R.drawable.iv_lan_japan
            ),
            LanguageModel(
                name = "한국어",
                code = "ko",
                image = R.drawable.iv_lan_korea
            )
        )
    }
}
