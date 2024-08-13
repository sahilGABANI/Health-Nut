package com.hotbox.terminal.helper

import java.util.*
import java.util.concurrent.TimeUnit

class TimezoneHelper(private val timezone: String) {
    private val inputFormat = "MM-dd-yyyy hh:mm a"
    private val dateFormat = java.text.SimpleDateFormat(inputFormat, Locale.getDefault())

    init {
        dateFormat.timeZone = TimeZone.getTimeZone(timezone)
    }

    fun getTimeDifference(timestamp: String): Int {
        val tz = TimeZone.getTimeZone(timezone)
        TimeZone.setDefault(tz)
        val currentTime = Calendar.getInstance(tz)
        val inputTime = Calendar.getInstance()
        currentTime.timeZone =  TimeZone.getTimeZone(timezone)
        inputTime.time = dateFormat.parse(timestamp) as Date
        val diff: Long = inputTime.time.time.let { it1 ->
            currentTime.time.time.minus(
                it1
            )
        }
        val differnce: Long = diff.let { it1 -> TimeUnit.MINUTES.convert(it1, TimeUnit.MILLISECONDS) } ?: 0

        return differnce.toInt()
    }
}