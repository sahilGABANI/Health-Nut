package com.hotbox.terminal.base.extension

import android.graphics.drawable.Drawable
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.TextView
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.text.DecimalFormat
import kotlin.math.abs

fun View.onClick(callback: () -> Unit) {
    setOnClickListener { callback.invoke() }
}

fun EditText.isNotValidEmail(): Boolean {
    if (!Patterns.EMAIL_ADDRESS.matcher(this.text.toString().trim()).matches()) {
        this.requestFocus()
        return true
    }
    return false
}

fun EditText.isFieldBlank(): Boolean {
    if (this.text.toString().isEmpty()) {
        this.requestFocus()
        return true
    }
    return false
}

fun EditText.isPhoneNumber(): Boolean {
    if (this.text.toString().isEmpty()) {
        this.requestFocus()
        return true
    }
    return false
}

fun EditText.isNotValidPhoneLength(): Boolean {
    if (this.text.toString().trim().length < 8 || this.text.toString().trim().length > 15) {
        this.requestFocus()
        return true
    }
    return false
}

fun EditText.isNotValidPinLength(): Boolean {
    if (this.text.toString().trim().length < 6 || this.text.toString().trim().length > 6) {
        this.requestFocus()
        return true
    }
    return false
}

fun Any?.toDollar(): String {
    val df = DecimalFormat("0.00")
    val value : String? =  df.format(this)
    return if (value == null) {
        "$0"
    } else {
        "$${value}"
    }
}
fun Any?.toDollarWithOutFormat(): String {
    return if (this == null) {
        "-$0"
    } else {
        "-$${this}"
    }
}
fun Double?.toMinusDollar(): Double? {
    return this?.let { abs(it) }
}
fun Double?.toConvertDecimalFormat(): Double {
    val df = DecimalFormat("0.00")
    return df.format(this).toDouble()
}

fun TextView.leftDrawable(drawable: Drawable?) {
    this.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
}

fun TextView.clearRightDrawable() {
    this.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
}

fun InputStream.getMessage(): String {
    val responseString = BufferedReader(InputStreamReader(this)).use { it.readText() }
    val responseJson = JSONObject(responseString)
    return responseJson.getString("message")
}