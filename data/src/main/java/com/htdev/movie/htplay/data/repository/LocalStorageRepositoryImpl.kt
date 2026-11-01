package com.htdev.movie.htplay.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import com.htdev.movie.htplay.data.common.ConstantKey
import com.htdev.movie.htplay.domain.repository.LocalStorageRepository
import javax.inject.Inject

/**
 * @author hwa..
 */
class LocalStorageRepositoryImpl @Inject constructor(
    private val sharedPreferences: SharedPreferences,
) : LocalStorageRepository{
    override fun getCurrentTabSearch(): Int {
        return sharedPreferences
            .getInt(ConstantKey.TAB_SEARCH_CURRENT,-1)
    }

    override fun saveCurrentTabSearch(idTab: Int) {
        sharedPreferences.edit {
            putInt(ConstantKey.TAB_SEARCH_CURRENT,idTab)
        }
    }

}
