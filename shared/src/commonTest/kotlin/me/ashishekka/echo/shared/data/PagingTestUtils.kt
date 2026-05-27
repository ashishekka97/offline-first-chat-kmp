package me.ashishekka.echo.shared.data

import androidx.paging.PagingSource

/**
 * Extension function to load data from a [PagingSource] for testing purposes.
 */
suspend fun <K : Any, V : Any> PagingSource<K, V>.getData(): List<V> {
    val result = load(
        PagingSource.LoadParams.Refresh(
            key = null,
            loadSize = 100,
            placeholdersEnabled = false
        )
    )
    return when (result) {
        is PagingSource.LoadResult.Page -> result.data
        is PagingSource.LoadResult.Error -> throw result.throwable
        is PagingSource.LoadResult.Invalid -> emptyList()
    }
}
