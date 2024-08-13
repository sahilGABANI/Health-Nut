package com.hotbox.terminal.base.prefs

import android.content.Context
import dagger.Module
import dagger.Provides

@Module
class PrefsModule {

    @Provides
    fun provideLocalPrefs(context: Context): LocalPrefs {
        return LocalPrefs(context)
    }
}