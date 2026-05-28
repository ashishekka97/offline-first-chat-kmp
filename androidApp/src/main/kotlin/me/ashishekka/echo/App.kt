package me.ashishekka.echo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import me.ashishekka.echo.screens.HomeScreen
import me.ashishekka.echo.shared.screens.home.HomeIntent
import me.ashishekka.echo.shared.screens.home.HomeSideEffect
import me.ashishekka.echo.shared.screens.home.HomeViewModel
import me.ashishekka.echo.ui.theme.EchoTheme
import org.koin.compose.viewmodel.koinViewModel

@Serializable
object ListDestination

@Serializable
data class DetailDestination(val objectId: String)

@Composable
fun App() {
    EchoTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            EchoNavGraph()
        }
    }
}

@Composable
fun EchoNavGraph() {
    val navController = rememberNavController()
    val homeViewModel = koinViewModel<HomeViewModel>()

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
            Text("Chat Detail for: ${destination.objectId}")
        }
    }
}
