package me.ashishekka.echo.shared.util

/**
 * Single source of truth for Echo Chat App branding and UI constants.
 * These are platform-agnostic values (hex strings, floats) that ensure
 * visual consistency between Compose (Android) and SwiftUI (iOS).
 */
object DesignTokens {
    
    object Colors {
        // Branding
        const val Primary = "#6200EE"
        const val PrimaryVariant = "#3700B3"
        const val Secondary = "#03DAC6"
        
        // Backgrounds
        const val Background = "#FFFFFF"
        const val Surface = "#FFFFFF"
        const val Error = "#B00020"
        
        // Chat Bubbles
        const val UserBubble = "#E3F2FD"
        const val AgentBubble = "#F5F5F5"
        
        // Text
        const val OnPrimary = "#FFFFFF"
        const val OnSecondary = "#000000"
        const val OnBackground = "#000000"
        const val OnSurface = "#000000"
        const val OnError = "#FFFFFF"
        
        const val TextPrimary = "#212121"
        const val TextSecondary = "#757575"
        const val TextHint = "#BDBDBD"
    }
    
    object Spacing {
        const val Tiny = 4f
        const val Small = 8f
        const val Medium = 16f
        const val Large = 24f
        const val ExtraLarge = 32f
        
        const val BubblePaddingHorizontal = 12f
        const val BubblePaddingVertical = 8f
        const val ChatItemSpacing = 4f
    }
    
    object Shape {
        const val CornerRadiusSmall = 4f
        const val CornerRadiusMedium = 8f
        const val CornerRadiusLarge = 12f
        const val BubbleCornerRadius = 16f
    }
}
