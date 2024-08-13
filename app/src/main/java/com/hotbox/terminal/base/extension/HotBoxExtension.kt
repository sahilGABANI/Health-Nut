package com.hotbox.terminal.base.extension

import android.os.Bundle
import com.hotbox.terminal.utils.Constants.isDebugMode

fun getAPIBaseUrl(): String {
    return if (isDebugMode()) {
        "https://hndevapi.oper.live/"
    } else {
//        "https://hndevapi.oper.live/"
        "https://hnapi.oper.live/"
    }
}

fun Bundle.putEnum(key:String, enum: Enum<*>){
    putString(key, enum.name)
}

inline fun <reified T: Enum<T>> Bundle.getEnum(key: String, default: T): T {
    val found = getString(key)
    return if (found == null) { default } else enumValueOf(found)
}