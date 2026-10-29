package com.htdev.movie.htplay.domain.usecase.movie

import com.htdev.movie.htplay.domain.repository.LocalStorageRepository
import javax.inject.Inject

/**
 * @author hwa..
 */
class GetCurrentTabSearch @Inject constructor(
    private val localStorageRepository: LocalStorageRepository
){
    operator fun invoke() = localStorageRepository.getCurrentTabSearch()
}