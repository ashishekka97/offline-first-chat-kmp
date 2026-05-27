package me.ashishekka.echo.shared.di

import me.ashishekka.echo.shared.data.PreferenceStorage
import me.ashishekka.echo.shared.screens.home.HomeViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

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
}
