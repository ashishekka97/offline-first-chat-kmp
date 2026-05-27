package me.ashishekka.echo.shared.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider

actual fun getTestDataStorePath(fileName: String): String {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return context.filesDir.resolve(fileName).absolutePath
}
