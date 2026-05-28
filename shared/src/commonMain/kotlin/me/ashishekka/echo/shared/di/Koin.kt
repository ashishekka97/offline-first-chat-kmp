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
import me.ashishekka.echo.shared.data.repository.OfflineFirstParticipantRepository
import me.ashishekka.echo.shared.domain.repository.ChatRepository
import me.ashishekka.echo.shared.domain.repository.MessageRepository
import me.ashishekka.echo.shared.domain.repository.ParticipantRepository
import me.ashishekka.echo.shared.domain.service.AgentService
import me.ashishekka.echo.shared.domain.service.DefaultAgentService
import me.ashishekka.echo.shared.domain.usecase.DeleteChatUseCase
import me.ashishekka.echo.shared.domain.usecase.GetChatByIdUseCase
import me.ashishekka.echo.shared.domain.usecase.GetPagedChatsUseCase
import me.ashishekka.echo.shared.domain.usecase.GetPagedMessagesUseCase
import me.ashishekka.echo.shared.domain.usecase.SendMessageUseCase
import me.ashishekka.echo.shared.domain.usecase.StartChatUseCase
import me.ashishekka.echo.shared.screens.chat.ChatDetailViewModel
import me.ashishekka.echo.shared.screens.home.HomeViewModel
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
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

    single<ChatRepository> { OfflineFirstChatRepository(get(), get()) }
    single<MessageRepository> { OfflineFirstMessageRepository(get()) }

    single<ParticipantRepository> { OfflineFirstParticipantRepository(get()) }

    // Use Cases
    factory { GetPagedChatsUseCase(get()) }
    factory { GetPagedMessagesUseCase(get()) }
    factory { GetChatByIdUseCase(get()) }
    factory { StartChatUseCase(get(), get()) }
    factory { SendMessageUseCase(get(), get()) }
    factory { DeleteChatUseCase(get(), get(), get()) }
}

val viewModelModule = module {
    factoryOf(::HomeViewModel)
    factory { (chatId: String) ->
        ChatDetailViewModel(
            chatId = chatId,
            getChatByIdUseCase = get(),
            getPagedMessagesUseCase = get(),
            sendMessageUseCase = get(),
            startChatUseCase = get()
        )
    }
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
