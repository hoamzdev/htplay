package com.htdev.movie.htplay.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.usecase.movie.GetMoviesLatest
import com.htdev.movie.htplay.domain.usecase.movie.pager.GetMoviesBySearch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val getMoviesBySearch: GetMoviesBySearch,
    private val getMoviesLatest: GetMoviesLatest,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val query = savedStateHandle.getStateFlow(QUERY, "")
    private val latestRetry = MutableStateFlow(0)
    private var latestMovies: List<SearchItem>? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val movies = combine(query, latestRetry) { keyword, _ -> keyword }
        .flatMapLatest { keyword ->
            flow {
                // Clear the previous pager as soon as the input changes.
                emit(PagingData.empty<SearchItem>(sourceLoadStates = loadStates(LoadState.Loading)))
                if (keyword.isBlank()) {
                    val items = latestMovies ?: when (val result = getMoviesLatest()) {
                        is Result.Success -> result.data.take(PAGE_SIZE).map { it.toSearchItem() }
                            .also { latestMovies = it }
                        is Result.Error -> throw result.throwable
                        else -> emptyList()
                    }
                    emit(PagingData.from(items, sourceLoadStates = loadStates(LoadState.NotLoading(true))))
                } else {
                    // Delaying inside flatMapLatest also cancels in-flight old requests immediately.
                    delay(500.milliseconds)
                    emitAll(getMoviesBySearch(keyword = keyword, limit = PAGE_SIZE)
                        .map { page -> page.map { it.toSearchItem() } })
                }
            }.catch { error ->
                emit(PagingData.empty(sourceLoadStates = loadStates(LoadState.Error(error))))
            }
        }.cachedIn(viewModelScope)

    fun setQuery(value: String) {
        savedStateHandle[QUERY] = value.trim()
    }

    fun retryLatest() {
        latestRetry.value += 1
    }

    private fun loadStates(refresh: LoadState) = LoadStates(
        refresh = refresh,
        prepend = LoadState.NotLoading(true),
        append = LoadState.NotLoading(true),
    )

    private companion object {
        const val QUERY = "search_query"
        const val PAGE_SIZE = 20
    }
}
