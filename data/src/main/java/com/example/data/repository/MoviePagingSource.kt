package com.example.data.repository

import androidx.paging.PagingSource
import kotlinx.coroutines.CancellationException
import androidx.paging.PagingState

/**
 * @author hwa..
 */
class MoviePagingSource<T : Any>(
    private val apiCall : suspend (page : Int) -> List<T>
) : PagingSource<Int, T>(){

    override fun getRefreshKey(state: PagingState<Int, T>): Int? {
        return state.anchorPosition?.let {
            val page = state.closestPageToPosition(it)
            page?.prevKey?.plus(1) ?: page?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        return try{
            val page = params.key ?: 1

            val response = apiCall(page)
            val prevKey = if(page == 1) null else page - 1
            val nextKey = if(response.isEmpty()) null else page + 1

            LoadResult.Page(
                data = response,
                prevKey = prevKey,
                nextKey = nextKey
            )
        }catch (e : Exception){
            if (e is CancellationException) throw e
            LoadResult.Error(e)
        }
    }
}