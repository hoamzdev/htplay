package com.htdev.movie.htplay.domain.usecase.movie

import com.htdev.movie.htplay.domain.repository.LocalStorageRepository
import javax.inject.Inject

/**
 * @author hwa..
 */
class SaveCurrentTabSearch @Inject constructor(
    private val localStorageRepository: LocalStorageRepository
) {
    operator fun invoke(idTab : Int) = localStorageRepository.saveCurrentTabSearch(idTab)
}