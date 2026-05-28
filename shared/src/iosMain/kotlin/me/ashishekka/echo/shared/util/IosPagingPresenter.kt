package me.ashishekka.echo.shared.util

import androidx.paging.PagingData
import androidx.paging.PagingDataPresenter
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * An iOS-specific presenter that converts [PagingData] into a [StateFlow] of [List].
 * This allows SwiftUI to consume the existing Paging 3 flow without extra DAO methods.
 */
class IosPagingPresenter<T : Any>(
    private val scope: CoroutineScope
) : PagingDataPresenter<T>() {

    constructor() : this(CoroutineScope(Dispatchers.Main))
    
    private val _items = MutableStateFlow<List<T>>(emptyList())
    
    @NativeCoroutines
    val items: StateFlow<List<T>> = _items.asStateFlow()

    override suspend fun presentPagingDataEvent(event: androidx.paging.PagingDataEvent<T>) {
        _items.value = snapshot().items
    }

    /**
     * Starts collecting from the given [pagingDataFlow].
     */
    fun collectFrom(pagingDataFlow: Flow<PagingData<T>>) {
        scope.launch {
            pagingDataFlow.collectLatest { pagingData ->
                this@IosPagingPresenter.collectFrom(pagingData)
            }
        }
    }
}
