package com.hotbox.terminal.helper

import android.icu.text.DateFormatSymbols
import android.icu.text.SimpleDateFormat
import android.icu.text.TimeZoneFormat
import android.icu.util.TimeZone
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

const val CLOCK_IN_OUT_FORMAT = "MM/dd/yy hh:mm a"
fun String.toDate(
    dateFormat: String = "yyyy-MM-dd'T'HH:mm:ss",
    timeZone: TimeZone = TimeZone.getTimeZone("UTC")
): Date? {
    try {
        val parser = SimpleDateFormat(dateFormat, Locale.getDefault())
        //parser.timeZone = TimeZone.getDefault()
        return parser.parse(this)
    } catch(e: Exception) {
        Timber.e(e, "Invalid Format Time :'$this' ")
    }
    return null

}


fun String.toStoreTimeZoneDate(
    dateFormat: String = "yyyy-MM-dd'T'HH:mm:ss",
    timeZone: TimeZone = TimeZone.getTimeZone("UTC")
): Date? {
    try {
        val parser = SimpleDateFormat(dateFormat, Locale.US)
        parser.timeZone = timeZone
        return parser.parse(this)
    } catch(e: Exception) {
        Timber.e(e, "Invalid Format Time :'$this' ")
    }
    return null

}

fun Date.formatTo(dateFormat: String, timeZone: TimeZone = TimeZone.getDefault()): String {
    val formatter = SimpleDateFormat(dateFormat, Locale.getDefault())
    formatter.timeZone = timeZone
    return formatter.format(this)
}

fun Int.getMonthNameFromInt(): String {
    return DateFormatSymbols.getInstance().months[this]
}

fun getCurrentsStoreTime(locationLocationTimezone: String): String {
    val calendar = Calendar.getInstance()
    val sdf = java.text.SimpleDateFormat("dd-MMM-yyyy HH:mm:ss z")
    sdf.timeZone = java.util.TimeZone.getTimeZone(locationLocationTimezone)
    return sdf.format(calendar.time)
}
fun String.formatToStoreTime(dateFormat: String): String {
    val outputFormat = DateTimeFormatter.ofPattern(dateFormat)
    val inputFormat = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss z")
    val dateTime = LocalDateTime.parse(this,inputFormat)
    return dateTime.format(outputFormat)
}