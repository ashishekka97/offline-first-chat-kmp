package me.ashishekka.echo.shared.util

/**
 * Interface for providing localized strings to shared logic.
 * This prevents hardcoding text like "Just now" in the domain layer.
 */
interface StringProvider {
    fun get(key: EchoString): String
    val is24HourFormat: Boolean
}

/**
 * Keys for all localized strings required by the app.
 * Using an enum ensures compile-time safety across platforms.
 */
enum class EchoString {
    // Home Screen
    HomeTitle,
    HomeEmptyMessage,
    HomeStartNew,
    HomeDeleteChatTitle,
    HomeDeleteChatMessage,
    HomeDeleting,

    // Chat Detail Screen
    ChatNewTitle,
    ChatRenameTitle,
    ChatRenameLabel,
    ChatRenameMessage,
    ChatChooseSource,
    ChatGallery,
    ChatCamera,
    ChatMessagePlaceholder,
    ChatAttachDesc,
    ChatSendDesc,
    ChatBackDesc,
    ChatNoMessages,

    // Shared / Domain
    JustNow,
    MinutesAgo,
    Yesterday,
    Photo,
    ImageChat,
    NewChat,
    
    // Common
    Delete,
    Cancel,
    Save,
    Dismiss,
    OK,
    ErrorPrefix,
    DraftPrefix,

    // Months
    MonthJan, MonthFeb, MonthMar, MonthApr, MonthMay, MonthJun,
    MonthJul, MonthAug, MonthSep, MonthOct, MonthNov, MonthDec
}
