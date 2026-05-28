package me.ashishekka.echo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import me.ashishekka.echo.ui.theme.EchoTheme

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

    NavHost(
        navController = navController,
        startDestination = ListDestination
    ) {
        composable<ListDestination> {
            Text("Home Chat List (TODO)")
        }
        composable<DetailDestination> { backStackEntry ->
            val destination = backStackEntry.toRoute<DetailDestination>()
            Text("Chat Detail for: ${destination.objectId}")
        }
    }
}
