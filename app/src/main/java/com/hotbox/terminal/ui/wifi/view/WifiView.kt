package com.hotbox.terminal.ui.wifi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.View
import com.hotbox.terminal.R
import com.hotbox.terminal.api.wifi.WifiInfo
import com.hotbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hotbox.terminal.base.extension.throttleClicks
import com.hotbox.terminal.databinding.ViewWifiLayoutBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class WifiView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val wifiStateSubject: PublishSubject<WifiInfo> = PublishSubject.create()
    val wifiActionState: Observable<WifiInfo> = wifiStateSubject.hide()

    private lateinit var binding: ViewWifiLayoutBinding
    private lateinit var wifiinfo: WifiInfo

    init {
        inflateUi()
    }

    @SuppressLint("ResourceAsColor")
    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_wifi_layout, this)

        binding = ViewWifiLayoutBinding.bind(view)

        binding.wifiSelectLayout.throttleClicks().subscribeAndObserveOnMainThread {
            binding.wifiSelectLayout.isSelected = true
            binding.wifiImageView.isSelected = true
            binding.wifiNameTextView.setTextColor(Color.parseColor("#000000"))
            binding.connectionStatusTextview.setText(R.string.connecting)
            binding.connectionStatusTextview.setTextColor(Color.parseColor("#666666"))
            wifiStateSubject.onNext(wifiinfo)
        }
    }

    fun bind(wifiInfo: WifiInfo) {
        wifiinfo = wifiInfo
        binding.wifiNameTextView.text = wifiInfo.wifiName
    }
}


