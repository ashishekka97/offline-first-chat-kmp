package me.ashishekka.echo.shared.di

import me.ashishekka.echo.shared.data.AppDatabase
import me.ashishekka.echo.shared.data.DataStorePreferenceStorage
import me.ashishekka.echo.shared.data.PreferenceStorage
import me.ashishekka.echo.shared.data.backup.BackupParser
import me.ashishekka.echo.shared.data.backup.BackupRestorationEngine
import me.ashishekka.echo.shared.data.backup.DefaultBackupParser
import me.ashishekka.echo.shared.data.backup.DefaultBackupRestorationEngine
import me.ashishekka.echo.shared.data.backup.DefaultMediaRestorationService
import me.ashishekka.echo.shared.data.backup.DefaultSeedDataRepository
import me.ashishekka.echo.shared.data.backup.MediaRestorationService
import me.ashishekka.echo.shared.data.backup.SeedDataRepository
import me.ashishekka.echo.shared.data.createDatabase
import me.ashishekka.echo.shared.data.repository.OfflineFirstChatRepository
import me.ashishekka.echo.shared.data.repository.OfflineFirstMessageRepository
import me.ashishekka.echo.shared.domain.repository.ChatRepository
import me.ashishekka.echo.shared.domain.repository.MessageRepository
import me.ashishekka.echo.shared.domain.service.AgentService
import me.ashishekka.echo.shared.domain.service.DefaultAgentService
import me.ashishekka.echo.shared.domain.usecase.SendMessageUseCase
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
    single { get<AppDatabase>().restorationDao() }

    single<PreferenceStorage> { DataStorePreferenceStorage(get()) }
    single<BackupParser> { DefaultBackupParser() }
    single<SeedDataRepository> { DefaultSeedDataRepository(get()) }
    single<MediaRestorationService> { DefaultMediaRestorationService(get(), get(), get()) }
    single<BackupRestorationEngine> { DefaultBackupRestorationEngine(get(), get(), get(), get(), get(), get()) }

    single<AgentService> { DefaultAgentService(get(), get()) }

    single<ChatRepository> { OfflineFirstChatRepository(get()) }
    single<MessageRepository> { OfflineFirstMessageRepository(get()) }

    // Use Cases
    factory { SendMessageUseCase(get(), get()) }
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
