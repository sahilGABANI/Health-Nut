package com.hotbox.terminal.application

import android.app.Activity
import android.app.Application
import com.hotbox.terminal.di.DaggerHotBoxAppComponent
import com.hotbox.terminal.di.HotBoxAppComponent
import com.hotbox.terminal.di.HotboxAppModule

class HotBox : HotBoxApplication()  {
    companion object {

        operator fun get(app: Application): HotBox {
            return app as HotBox
        }

        operator fun get(activity: Activity): HotBox {
            return activity.application as HotBox
        }

        lateinit var component: HotBoxAppComponent
            private set
    }
    override fun onCreate() {
        super.onCreate()
        try {
            component = DaggerHotBoxAppComponent.builder()
                .hotboxAppModule(HotboxAppModule(this))
                .build()
            component.inject(this)
            super.setAppComponent(component)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
