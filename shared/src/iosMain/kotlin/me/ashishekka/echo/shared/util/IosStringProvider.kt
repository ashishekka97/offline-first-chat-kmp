package me.ashishekka.echo.shared.util

import platform.Foundation.NSBundle
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

/**
 * iOS implementation of [StringProvider] using NSLocalizedString.
 */
class IosStringProvider : StringProvider {
    override fun get(key: EchoString): String {
        val resourceKey = when (key) {
            EchoString.HomeTitle -> "home_title"
            EchoString.HomeEmptyMessage -> "home_empty_message"
            EchoString.HomeStartNew -> "home_start_new"
            EchoString.HomeDeleteChatTitle -> "home_delete_chat_title"
            EchoString.HomeDeleteChatMessage -> "home_delete_chat_message"
            EchoString.HomeDeleting -> "home_deleting"
            EchoString.ChatNewTitle -> "chat_new_title"
            EchoString.ChatRenameTitle -> "chat_rename_title"
            EchoString.ChatRenameLabel -> "chat_rename_label"
            EchoString.ChatRenameMessage -> "chat_rename_message"
            EchoString.ChatChooseSource -> "chat_choose_source"
            EchoString.ChatGallery -> "chat_gallery"
            EchoString.ChatCamera -> "chat_camera"
            EchoString.ChatMessagePlaceholder -> "chat_message_placeholder"
            EchoString.ChatAttachDesc -> "chat_attach_desc"
            EchoString.ChatSendDesc -> "chat_send_desc"
            EchoString.ChatBackDesc -> "chat_back_desc"
            EchoString.ChatNoMessages -> "chat_no_messages"
            EchoString.JustNow -> "shared_just_now"
            EchoString.MinutesAgo -> "shared_minutes_ago"
            EchoString.Yesterday -> "shared_yesterday"
            EchoString.Photo -> "shared_photo"
            EchoString.ImageChat -> "shared_image_chat"
            EchoString.NewChat -> "shared_new_chat"
            EchoString.Delete -> "common_delete"
            EchoString.Cancel -> "common_cancel"
            EchoString.Save -> "common_save"
            EchoString.Dismiss -> "common_dismiss"
            EchoString.OK -> "common_ok"
            EchoString.ErrorPrefix -> "common_error"
            EchoString.DraftPrefix -> "common_draft_prefix"
            EchoString.MonthJan -> "shared_month_jan"
            EchoString.MonthFeb -> "shared_month_feb"
            EchoString.MonthMar -> "shared_month_mar"
            EchoString.MonthApr -> "shared_month_apr"
            EchoString.MonthMay -> "shared_month_may"
            EchoString.MonthJun -> "shared_month_jun"
            EchoString.MonthJul -> "shared_month_jul"
            EchoString.MonthAug -> "shared_month_aug"
            EchoString.MonthSep -> "shared_month_sep"
            EchoString.MonthOct -> "shared_month_oct"
            EchoString.MonthNov -> "shared_month_nov"
            EchoString.MonthDec -> "shared_month_dec"
        }
        // Explicitly look in the main bundle where the xcstrings file lives
        return NSBundle.mainBundle.localizedStringForKey(resourceKey, value = resourceKey, table = null)
    }

    override val is24HourFormat: Boolean
        get() {
            val format = NSDateFormatter.dateFormatFromTemplate("j", 0uL, NSLocale.currentLocale)
            return format?.contains("H") == true
        }
}
