package com.hotbox.terminal.base

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.os.Bundle

class ActivityManager {

    companion object {

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: ActivityManager? = null

        fun getInstance(): ActivityManager =
            instance ?: synchronized(this) {
                instance ?: ActivityManager().also { instance = it }
            }
    }

    var application: Application? = null
        private set

    var foregroundActivity: Activity? = null
        private set

    fun init(application: Application) {
        this.application = application
        this.application?.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                foregroundActivity = activity
            }

            override fun onActivityStarted(activity: Activity) {
                foregroundActivity = activity
            }

            override fun onActivityResumed(activity: Activity) {
                foregroundActivity = activity
            }

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState : Bundle) {

            }

            override fun onActivityDestroyed(activity: Activity) {
                if (foregroundActivity != null && foregroundActivity == activity) {
                    foregroundActivity = null
                }
            }
        })
    }
}