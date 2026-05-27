package me.ashishekka.echo

import android.app.Application
import me.ashishekka.echo.shared.di.initKoin
import org.koin.android.ext.koin.androidContext

class Echo : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@Echo)
        }
    }
}
