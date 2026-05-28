package me.ashishekka.echo.shared.di

import me.ashishekka.echo.shared.data.AppDatabase
import me.ashishekka.echo.shared.data.DataStorePreferenceStorage
import me.ashishekka.echo.shared.data.PreferenceStorage
import me.ashishekka.echo.shared.data.backup.*
import me.ashishekka.echo.shared.data.createDatabase
import me.ashishekka.echo.shared.data.repository.OfflineFirstChatRepository
import me.ashishekka.echo.shared.data.repository.OfflineFirstMessageRepository
import me.ashishekka.echo.shared.data.repository.OfflineFirstParticipantRepository
import me.ashishekka.echo.shared.domain.model.ChatId
import me.ashishekka.echo.shared.domain.repository.ChatRepository
import me.ashishekka.echo.shared.domain.repository.MessageRepository
import me.ashishekka.echo.shared.domain.repository.ParticipantRepository
import me.ashishekka.echo.shared.domain.service.*
import me.ashishekka.echo.shared.domain.usecase.*
import me.ashishekka.echo.shared.screens.chat.ChatDetailViewModel
import me.ashishekka.echo.shared.screens.home.HomeViewModel
import me.ashishekka.echo.shared.util.Log
import me.ashishekka.echo.shared.util.Logger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

expect fun getPlatformDataModule(): Module

val dispatcherModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
}

val persistenceModule = module {
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
}

val infrastructureModule = module {
    single<BackupParser> { DefaultBackupParser() }
    single<SeedDataRepository> { DefaultSeedDataRepository(get()) }
    single<MediaRestorationService> { DefaultMediaRestorationService(get(), get(), get()) }
    single<BackupRestorationEngine> { DefaultBackupRestorationEngine(get(), get(), get(), get(), get(), get()) }
    single<IdGenerator> { DefaultIdGenerator() }
    single<MediaService> { DefaultMediaService(get(), get(), get()) }
}

val domainModule = module {
    single<AgentService> { DefaultAgentService(get(), get(), get()) }
    single<ChatRepository> { OfflineFirstChatRepository(get(), get(), get()) }
    single<MessageRepository> { OfflineFirstMessageRepository(get(), get(), get()) }
    single<ParticipantRepository> { OfflineFirstParticipantRepository(get()) }
}

val useCaseModule = module {
    factory { GetPagedChatsUseCase(get()) }
    factory { GetPagedMessagesUseCase(get()) }
    factory { GetChatByIdUseCase(get()) }
    factory { StartChatUseCase(get(), get(), get(), get()) }
    factory { SendMessageUseCase(get(), get(), get(), get()) }
    factory { DeleteChatUseCase(get(), get(), get(), get()) }
}

val viewModelModule = module {
    factory { HomeViewModel(get(), get(), get(), get(), get()) }
    factory { (chatId: ChatId) ->
        ChatDetailViewModel(
            chatId = chatId,
            getChatByIdUseCase = get(),
            getPagedMessagesUseCase = get(),
            sendMessageUseCase = get(),
            startChatUseCase = get(),
            agentService = get(),
            participantRepository = get(),
            chatRepository = get(),
            preferenceStorage = get(),
            idGenerator = get()
        )
    }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    val koinApp = startKoin {
        printLogger(Level.INFO)
        appDeclaration()
        modules(
            dispatcherModule,
            getPlatformDataModule(),
            persistenceModule,
            infrastructureModule,
            domainModule,
            useCaseModule,
            viewModelModule,
        )
    }
    
    // Initialize the global Log accessor
    Log.init(koinApp.koin.get<Logger>())
}
