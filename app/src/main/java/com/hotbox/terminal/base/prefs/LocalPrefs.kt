package com.hotbox.terminal.base.prefs

import android.content.Context
import android.content.SharedPreferences
import io.reactivex.Observable

class LocalPrefs(context: Context) {

    private val sharedPrefs = context.getSharedPreferences(HotBoxSharedPreferences.PREFERENCE_NAME, Context.MODE_PRIVATE)

    fun putBoolean(key: String, value: Boolean) {
        sharedPrefs.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        sharedPrefs.getBoolean(key, defaultValue)

    fun putString(key: String, value: String?) {
        sharedPrefs.edit().putString(key, value).apply()
    }

    fun getString(key: String, defaultValue: String? = null): String? =
        sharedPrefs.getString(key, defaultValue)

    fun putInt(key: String, value: Int) {
        sharedPrefs.edit().putInt(key, value).apply()
    }

    fun getInt(key: String, defaultValue: Int): Int =
        sharedPrefs.getInt(key, defaultValue)

    fun putLong(key: String, value: Long) {
        sharedPrefs.edit().putLong(key, value).apply()
    }

    fun getLong(key: String, defaultValue: Long): Long =
        sharedPrefs.getLong(key, defaultValue)

    fun putStringSet(key: String, value: Set<String>) {
        sharedPrefs.edit().putStringSet(key, value).apply()
    }

    fun getStringSet(key: String, defaultValue: MutableSet<String>): MutableSet<String> =
        sharedPrefs.getStringSet(key, defaultValue) ?: mutableSetOf()

    fun contains(key: String): Boolean =
        sharedPrefs.contains(key)

    fun removeValue(key: String) {
        sharedPrefs.edit().remove(key).apply()
    }

    fun removeStringFromSet(courseId: String, setKey: String) {
        val setOfStrings = getStringSet(setKey, mutableSetOf())
        setOfStrings.remove(courseId)
        putStringSet(setKey, setOfStrings)
    }

    fun addStringToSet(courseId: String, setKey: String) {
        val setOfStrings = getStringSet(setKey, mutableSetOf())
        setOfStrings.add(courseId)
        putStringSet(setKey, setOfStrings)
    }

    fun changes(): Observable<String> =
        Observable.create { e ->
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                e.onNext(key)
            }

            e.setCancellable {
                sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
            }
            sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
        }
}