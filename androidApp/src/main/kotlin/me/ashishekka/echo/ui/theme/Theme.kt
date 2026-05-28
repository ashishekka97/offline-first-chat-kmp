package me.ashishekka.echo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import me.ashishekka.echo.shared.util.DesignTokens

fun String.toColor(): Color {
    return Color(android.graphics.Color.parseColor(this))
}

private val LightColorScheme = lightColorScheme(
    primary = DesignTokens.Colors.Primary.toColor(),
    secondary = DesignTokens.Colors.Secondary.toColor(),
    background = DesignTokens.Colors.Background.toColor(),
    surface = DesignTokens.Colors.Surface.toColor(),
    error = DesignTokens.Colors.Error.toColor(),
    onPrimary = DesignTokens.Colors.OnPrimary.toColor(),
    onSecondary = DesignTokens.Colors.OnSecondary.toColor(),
    onBackground = DesignTokens.Colors.OnBackground.toColor(),
    onSurface = DesignTokens.Colors.OnSurface.toColor(),
    onError = DesignTokens.Colors.OnError.toColor()
)

private val DarkColorScheme = darkColorScheme(
    primary = DesignTokens.Colors.Dark.Primary.toColor(),
    secondary = DesignTokens.Colors.Dark.Secondary.toColor(),
    background = DesignTokens.Colors.Dark.Background.toColor(),
    surface = DesignTokens.Colors.Dark.Surface.toColor(),
    error = DesignTokens.Colors.Dark.Error.toColor(),
    onPrimary = DesignTokens.Colors.Dark.OnPrimary.toColor(),
    onSecondary = DesignTokens.Colors.Dark.OnSecondary.toColor(),
    onBackground = DesignTokens.Colors.Dark.OnBackground.toColor(),
    onSurface = DesignTokens.Colors.Dark.OnSurface.toColor(),
    onError = DesignTokens.Colors.Dark.OnError.toColor()
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun EchoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
