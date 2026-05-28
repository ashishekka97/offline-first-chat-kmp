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

        object Dark {
            const val Primary = "#BB86FC"
            const val PrimaryVariant = "#3700B3"
            const val Secondary = "#03DAC6"
            
            const val Background = "#121212"
            const val Surface = "#121212"
            const val Error = "#CF6679"
            
            const val UserBubble = "#3700B3"
            const val AgentBubble = "#2C2C2C"
            
            const val OnPrimary = "#000000"
            const val OnSecondary = "#000000"
            const val OnBackground = "#FFFFFF"
            const val OnSurface = "#FFFFFF"
            const val OnError = "#000000"
            
            const val TextPrimary = "#FFFFFF"
            const val TextSecondary = "#B0B0B0"
            const val TextHint = "#666666"
        }
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
