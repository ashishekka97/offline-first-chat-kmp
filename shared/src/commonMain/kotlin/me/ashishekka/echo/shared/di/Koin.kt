package me.ashishekka.echo.shared.di

import me.ashishekka.echo.shared.data.AppDatabase
import me.ashishekka.echo.shared.data.createDatabase
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

expect fun getPlatformDataModule(): Module

val dispatcherModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
}

val dataModule = module {
    single<AppDatabase> { 
        createDatabase(
            builder = get(),
            queryContext = get<DispatcherProvider>().io
        ) 
    }
    single { get<AppDatabase>().chatDao() }
    single { get<AppDatabase>().messageDao() }
    single { get<AppDatabase>().participantDao() }
}

val viewModelModule = module {
    // We will inject ChatViewModel here later
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        printLogger(Level.INFO)
        appDeclaration()
        modules(
            dispatcherModule,
            getPlatformDataModule(),
            dataModule,
            viewModelModule,
        )
    }
}
