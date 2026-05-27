package me.ashishekka.echo.shared.di

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

interface DispatcherProvider {
    val io: CoroutineContext
    val main: CoroutineContext
    val default: CoroutineContext
}

class DefaultDispatcherProvider : DispatcherProvider {
    override val io: CoroutineContext = Dispatchers.IO
    override val main: CoroutineContext = Dispatchers.Main
    override val default: CoroutineContext = Dispatchers.Default
}
