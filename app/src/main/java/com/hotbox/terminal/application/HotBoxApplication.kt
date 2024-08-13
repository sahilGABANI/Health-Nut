package com.hotbox.terminal.application

import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.bugsnag.android.Bugsnag
import com.hotbox.terminal.base.ActivityManager
import com.hotbox.terminal.di.BaseAppComponent
import com.hotbox.terminal.di.BaseUiApp
import com.hotbox.terminal.utils.Constants.isDebugMode
import timber.log.Timber

open class HotBoxApplication :  BaseUiApp() {

    companion object {
        lateinit var component: BaseAppComponent

        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        context = this
        ActivityManager.getInstance().init(this)
        if (!isDebugMode()) {
            Bugsnag.start(this)
        }
        setupLog()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
    }

    private fun setupLog() {
        if (isDebugMode()) {
            Timber.plant(Timber.DebugTree())
        }

    }


    override fun getAppComponent(): BaseAppComponent {
        return component
    }
    override fun setAppComponent(baseAppComponent: BaseAppComponent) {
        component = baseAppComponent
    }
}