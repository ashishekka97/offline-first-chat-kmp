package me.ashishekka.echo.shared.di

import org.koin.core.context.startKoin
import org.koin.dsl.module

val dataModule = module {
    // We will inject Room Database and ChatRepository here later
}

val viewModelModule = module {
    // We will inject ChatViewModel here later
}

fun initKoin() {
    startKoin {
        modules(
            dataModule,
            viewModelModule,
        )
    }
}
