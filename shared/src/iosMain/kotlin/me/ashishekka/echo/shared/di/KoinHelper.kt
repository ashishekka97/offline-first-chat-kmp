package me.ashishekka.echo.shared.di

import me.ashishekka.echo.shared.data.PreferenceStorage
import me.ashishekka.echo.shared.domain.model.ChatId
import me.ashishekka.echo.shared.screens.chat.ChatDetailViewModel
import me.ashishekka.echo.shared.screens.home.HomeViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * A helper class for iOS to initialize Koin.
 * This provides a clean, object-oriented API for Swift.
 */
class KoinHelper : KoinComponent {
    /**
     * Initializes Koin for the iOS application.
     */
    fun doInitKoin() {
        initKoin()
    }

    /**
     * Provides the [PreferenceStorage] instance to Swift.
     */
    val preferenceStorage: PreferenceStorage by inject()

    /**
     * Provides the [HomeViewModel] instance to Swift.
     */
    val homeViewModel: HomeViewModel by inject()

    /**
     * Provides a [ChatDetailViewModel] for the given [chatId].
     */
    fun getChatDetailViewModel(chatId: String): ChatDetailViewModel {
        return get(parameters = { parametersOf(ChatId(chatId)) })
    }
}
