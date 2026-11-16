package com.htdev.movie.htplay.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.entities.Favorite
import com.htdev.movie.htplay.domain.usecase.movie.DeleteFavoriteMovie
import com.htdev.movie.htplay.domain.usecase.movie.GetListFavoriteMovies
import com.htdev.movie.htplay.home.favorite.FavoriteMovieViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteMovieViewModelTest {
    @Before fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun searchMatchesNamesWithoutCaseOrVietnameseAccentsAndAlsoMatchesSlugs() = runTest {
        val storage = FakeLocalStorageRepository().apply {
            favorites.value = listOf(movie("Đường Đến Hạnh Phúc", "duong-den-hanh-phuc"), movie("Other", "other"))
        }
        val viewModel = viewModel(storage)
        viewModel.openSearch()
        for (query in listOf("  DUONG   DEN  ", "đường đến", "hanh phuc", "duong-den")) {
            viewModel.setQuery(query)
            advanceUntilIdle()
            assertEquals(listOf("duong-den-hanh-phuc"), items(viewModel).map { it.slugMovie })
            assertEquals(query, viewModel.uiState.value.query)
        }
        storage.favorites.value = listOf(movie("An English Title", "ten-phim"))
        viewModel.setQuery("ten phim")
        advanceUntilIdle()
        assertEquals("An English Title", items(viewModel).single().name)
    }

    @Test fun noMatchesProducesAnEmptySearchAndClearingTheQueryRestoresAllFavorites() = runTest {
        val movies = listOf(movie("First", "first"), movie("Second", "second"))
        val storage = FakeLocalStorageRepository().apply { favorites.value = movies }
        val viewModel = viewModel(storage)
        viewModel.openSearch()
        viewModel.setQuery("missing")
        advanceUntilIdle()
        assertTrue(items(viewModel).isEmpty())
        assertTrue(viewModel.uiState.value.query.isNotBlank())
        assertTrue(viewModel.uiState.value.isSearchOpen)
        assertEquals(movies, storage.favorites.value)

        viewModel.setQuery("   ")
        advanceUntilIdle()
        assertEquals(movies, items(viewModel))
    }

    @Test fun closingSearchClearsTheFilterAndAnEmptyLibraryRemainsEmpty() = runTest {
        val movies = listOf(movie("First", "first"), movie("Second", "second"))
        val storage = FakeLocalStorageRepository().apply { favorites.value = movies }
        val viewModel = viewModel(storage)
        viewModel.openSearch()
        viewModel.setQuery("first")
        advanceUntilIdle()
        assertEquals(1, items(viewModel).size)
        viewModel.closeSearch()
        advanceUntilIdle()
        assertEquals("", viewModel.uiState.value.query)
        assertFalse(viewModel.uiState.value.isSearchOpen)
        assertEquals(movies, items(viewModel))

        storage.favorites.value = emptyList()
        advanceUntilIdle()
        assertTrue(items(viewModel).isEmpty())
        assertTrue(viewModel.uiState.value.query.isBlank())
    }

    @Test fun deletingTheLastMatchAndSavingNewFavoritesUpdatesTheCurrentSearch() = runTest {
        val storage = FakeLocalStorageRepository().apply {
            favorites.value = listOf(movie("Matched", "matched"), movie("Other", "other"))
        }
        val viewModel = viewModel(storage)
        viewModel.openSearch()
        viewModel.setQuery("match")
        advanceUntilIdle()
        assertEquals("matched", items(viewModel).single().slugMovie)
        viewModel.deleteFavorite("matched")
        advanceUntilIdle()
        assertTrue(items(viewModel).isEmpty())
        assertEquals("match", viewModel.uiState.value.query)
        assertEquals("other", storage.favorites.value.single().slugMovie)

        storage.saveFavorite(movie("Another match", "another-match"))
        advanceUntilIdle()
        assertEquals("another-match", items(viewModel).single().slugMovie)
        assertEquals(1, storage.favorites.subscriptionCount.value)
    }

    @Test fun searchQueryAndOpenStateAreRestoredTogether() = runTest {
        val savedState = SavedStateHandle()
        val storage = FakeLocalStorageRepository().apply {
            favorites.value = listOf(movie("First", "first"), movie("Second", "second"))
        }
        val original = viewModel(storage, savedState)
        original.openSearch()
        original.setQuery("second")
        advanceUntilIdle()
        val restoredState = SavedStateHandle(savedState.keys().associateWith { savedState.get<Any?>(it) })
        val restored = viewModel(storage, restoredState)
        advanceUntilIdle()

        assertTrue(restored.uiState.value.isSearchOpen)
        assertEquals("second", restored.uiState.value.query)
        assertEquals("second", items(restored).single().slugMovie)
    }

    private fun viewModel(storage: FakeLocalStorageRepository, state: SavedStateHandle = SavedStateHandle()) =
        FavoriteMovieViewModel(DeleteFavoriteMovie(storage), GetListFavoriteMovies(storage), state)

    private fun items(viewModel: FavoriteMovieViewModel) =
        (viewModel.uiState.value.favorites as Result.Success).data

    private fun movie(name: String, slug: String) = Favorite(name = name, slugMovie = slug, genre = "")
}
