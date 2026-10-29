package com.htdev.movie.htplay.data.repository

import androidx.paging.PagingSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class MoviePagingSourceTest {
    private fun refresh(key: Int? = null) = PagingSource.LoadParams.Refresh(
        key = key, loadSize = 10, placeholdersEnabled = false
    )

    @Test
    fun firstPageAndEmptyLastPageHaveCorrectKeys() = runTest {
        val source = MoviePagingSource { page -> if (page == 1) listOf("movie") else emptyList() }
        val first = source.load(refresh()) as PagingSource.LoadResult.Page<Int, String>
        assertNull(first.prevKey)
        assertEquals(2, first.nextKey)
        assertEquals(listOf("movie"), first.data)
        val last = source.load(refresh(2)) as PagingSource.LoadResult.Page<Int, String>
        assertEquals(1, last.prevKey)
        assertNull(last.nextKey)
    }

    @Test
    fun failureIsReturnedForPagingRetry() = runTest {
        val failure = IOException("offline")
        val source = MoviePagingSource<String> { throw failure }
        val result = source.load(refresh()) as PagingSource.LoadResult.Error<Int, String>
        assertSame(failure, result.throwable)
    }

    @Test
    fun cancellationPropagatesInsteadOfBecomingAnErrorPage() = runTest {
        val cancelled = CancellationException("screen closed")
        val source = MoviePagingSource<String> { throw cancelled }
        try {
            source.load(refresh())
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancelled, actual)
        }
    }
}
