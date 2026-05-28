package me.ashishekka.echo.shared.util

import android.content.Context

import me.ashishekka.echo.shared.R

/**
 * Android implementation of [StringProvider] using resources.
 */
class AndroidStringProvider(private val context: Context) : StringProvider {
    override fun get(key: EchoString): String = context.getString(when (key) {
        EchoString.HomeTitle -> R.string.home_title
        EchoString.HomeEmptyMessage -> R.string.home_empty_message
        EchoString.HomeStartNew -> R.string.home_start_new
        EchoString.HomeDeleteChatTitle -> R.string.home_delete_chat_title
        EchoString.HomeDeleteChatMessage -> R.string.home_delete_chat_message
        EchoString.HomeDeleting -> R.string.home_deleting
        EchoString.ChatNewTitle -> R.string.chat_new_title
        EchoString.ChatRenameTitle -> R.string.chat_rename_title
        EchoString.ChatRenameLabel -> R.string.chat_rename_label
        EchoString.ChatRenameMessage -> R.string.chat_rename_message
        EchoString.ChatChooseSource -> R.string.chat_choose_source
        EchoString.ChatGallery -> R.string.chat_gallery
        EchoString.ChatCamera -> R.string.chat_camera
        EchoString.ChatMessagePlaceholder -> R.string.chat_message_placeholder
        EchoString.ChatAttachDesc -> R.string.chat_attach_desc
        EchoString.ChatSendDesc -> R.string.chat_send_desc
        EchoString.ChatBackDesc -> R.string.chat_back_desc
        EchoString.ChatNoMessages -> R.string.chat_no_messages
        EchoString.JustNow -> R.string.shared_just_now
        EchoString.MinutesAgo -> R.string.shared_minutes_ago
        EchoString.Yesterday -> R.string.shared_yesterday
        EchoString.Photo -> R.string.shared_photo
        EchoString.ImageChat -> R.string.shared_image_chat
        EchoString.NewChat -> R.string.shared_new_chat
        EchoString.Delete -> R.string.common_delete
        EchoString.Cancel -> R.string.common_cancel
        EchoString.Save -> R.string.common_save
        EchoString.Dismiss -> R.string.common_dismiss
        EchoString.OK -> R.string.common_ok
        EchoString.ErrorPrefix -> R.string.common_error_prefix
        EchoString.DraftPrefix -> R.string.common_draft_prefix
        EchoString.MonthJan -> R.string.shared_month_jan
        EchoString.MonthFeb -> R.string.shared_month_feb
        EchoString.MonthMar -> R.string.shared_month_mar
        EchoString.MonthApr -> R.string.shared_month_apr
        EchoString.MonthMay -> R.string.shared_month_may
        EchoString.MonthJun -> R.string.shared_month_jun
        EchoString.MonthJul -> R.string.shared_month_jul
        EchoString.MonthAug -> R.string.shared_month_aug
        EchoString.MonthSep -> R.string.shared_month_sep
        EchoString.MonthOct -> R.string.shared_month_oct
        EchoString.MonthNov -> R.string.shared_month_nov
        EchoString.MonthDec -> R.string.shared_month_dec
    })

    override val is24HourFormat: Boolean
        get() = android.text.format.DateFormat.is24HourFormat(context)
}
