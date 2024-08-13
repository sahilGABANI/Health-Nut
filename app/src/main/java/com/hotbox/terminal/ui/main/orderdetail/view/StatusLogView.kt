package com.hotbox.terminal.ui.main.orderdetail.view

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import android.view.View
import androidx.core.content.ContextCompat
import com.hotbox.terminal.R
import com.hotbox.terminal.api.order.model.StatusItem
import com.hotbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hotbox.terminal.databinding.ViewStatusListBinding
import timber.log.Timber
import java.util.*

class StatusLogView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private lateinit var binding: ViewStatusListBinding
    private lateinit var statuslogInfo: StatusItem

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_status_list, this)
        binding = ViewStatusListBinding.bind(view)
    }

    fun bind(statusItem: StatusItem) {
        statuslogInfo = statusItem

        binding.apply {
            dateAndTimeTextView.text = statusItem.timestamp?.toDate("MM-dd-yyyy, hh:mm a")?.formatTo("MM/dd/yyyy, hh:mm a")
            orderStatusTextView.text = statusItem.orderStatus
            customerTextView.text = statusItem.user?.fullName()

            when (statusItem.orderStatus) {
                resources.getString(R.string.received).lowercase() -> statusCardView.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context, R.color.color_FFE0C2
                    )
                )
                resources.getString(R.string.making).lowercase() -> statusCardView.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context, R.color.color_C0F2EC
                    )
                )
                resources.getString(R.string.completed).lowercase() -> statusCardView.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context, R.color.color_CFE2FE
                    )
                )
                resources.getString(R.string.assigned).lowercase() -> statusCardView.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context, R.color.color_FAEDBF
                    )
                )
                else -> statusCardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.light_green))
            }
        }
    }

    fun String.toDate(
        dateFormat: String = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timeZone: TimeZone = TimeZone.getTimeZone("PST")
    ): Date? {
        try {
            val parser = SimpleDateFormat(dateFormat, Locale.getDefault())
//            parser.timeZone = timeZone
            return parser.parse(this)
        } catch (e: Exception) {
            Timber.e(e, "Invalid Format Time :'$this' ")
        }
        return null

    }

    fun Date.formatTo(dateFormat: String, timeZone: TimeZone = TimeZone.getDefault()): String {
        val formatter = SimpleDateFormat(dateFormat, Locale.getDefault())
        formatter.timeZone = timeZone
        return formatter.format(this)
    }
}