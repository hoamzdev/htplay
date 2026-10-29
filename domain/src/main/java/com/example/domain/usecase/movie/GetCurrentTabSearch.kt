package com.example.domain.usecase.movie

import com.example.domain.repository.LocalStorageRepository
import javax.inject.Inject

/**
 * @author hwa..
 */
class GetCurrentTabSearch @Inject constructor(
    private val localStorageRepository: LocalStorageRepository
){
    operator fun invoke() = localStorageRepository.getCurrentTabSearch()
}