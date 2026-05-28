package me.ashishekka.echo

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import me.ashishekka.echo.screens.ChatDetailScreen
import me.ashishekka.echo.screens.FullscreenImageScreen
import me.ashishekka.echo.screens.HomeScreen
import me.ashishekka.echo.screens.SplashScreen
import me.ashishekka.echo.shared.screens.home.HomeIntent
import me.ashishekka.echo.shared.screens.home.HomeSideEffect
import me.ashishekka.echo.shared.screens.home.HomeViewModel
import me.ashishekka.echo.ui.theme.EchoTheme
import org.koin.compose.viewmodel.koinViewModel

@Serializable
object ListDestination

@Serializable
data class DetailDestination(val objectId: String)

@Serializable
data class FullscreenImageDestination(val imageUrl: String)

@Composable
fun App() {
    val homeViewModel = koinViewModel<HomeViewModel>()
    val state by homeViewModel.state.collectAsState()

    EchoTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AnimatedContent(
                targetState = state.isInitialBootstrap,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                },
                label = "SplashToHome"
            ) { isBootstrapping ->
                if (isBootstrapping) {
                    SplashScreen()
                } else {
                    EchoNavGraph(homeViewModel)
                }
            }
        }
    }
}

@Composable
fun EchoNavGraph(homeViewModel: HomeViewModel) {
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        homeViewModel.sideEffect.collect { effect ->
            when (effect) {
                is HomeSideEffect.NavigateToChat -> {
                    navController.navigate(DetailDestination(effect.chatId.value))
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = ListDestination
    ) {
        composable<ListDestination> {
            HomeScreen(
                onChatClick = { chatId ->
                    homeViewModel.onIntent(HomeIntent.ClickChat(chatId))
                },
                onNewChatClick = {
                    homeViewModel.onIntent(HomeIntent.NewChat)
                },
                viewModel = homeViewModel
            )
        }
        composable<DetailDestination> { backStackEntry ->
            val destination = backStackEntry.toRoute<DetailDestination>()
            ChatDetailScreen(
                chatId = destination.objectId,
                onBackClick = { navController.popBackStack() },
                onImageClick = { imageUrl ->
                    navController.navigate(FullscreenImageDestination(imageUrl))
                }
            )
        }
        composable<FullscreenImageDestination> { backStackEntry ->
            val destination = backStackEntry.toRoute<FullscreenImageDestination>()
            FullscreenImageScreen(
                imageUrl = destination.imageUrl,
                onCloseClick = { navController.popBackStack() }
            )
        }
    }
}
