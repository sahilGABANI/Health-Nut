package com.hotbox.terminal.ui.wifi

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.annotation.NonNull
import androidx.core.location.LocationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.hotbox.terminal.api.wifi.WifiInfo
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseActivity
import com.hotbox.terminal.base.extension.showLongToast
import com.hotbox.terminal.base.extension.showToast
import com.hotbox.terminal.base.extension.startActivityWithDefaultAnimation
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hotbox.terminal.databinding.ActivityAvailableWifiBinding
import com.hotbox.terminal.ui.wifi.view.WifiAdapter
import com.thanosfisherman.wifiutils.WifiUtils
import timber.log.Timber

class AvailableWifiActivity : BaseActivity() {

    private lateinit var wifiAdapter: WifiAdapter
    private lateinit var binding: ActivityAvailableWifiBinding
    private var listofwifi: ArrayList<WifiInfo> = arrayListOf()
    private val RC_GPS_SETTINGS = 100001

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, AvailableWifiActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        binding = ActivityAvailableWifiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.decorView.apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        }
        initUI()
        initAdapter()
    }

    private fun initUI() {
        XXPermissions.with(this)
            .permission(Permission.ACCESS_COARSE_LOCATION)
            .permission(Permission.ACCESS_FINE_LOCATION)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (all) {
                        if (!isLocationEnabled(this@AvailableWifiActivity)) {
                            showGPSSettingsAlert()
                        } else {
                            WifiUtils.enableLog(true)
                            WifiUtils.withContext(applicationContext)
                                .enableWifi(this@AvailableWifiActivity::checkResult)
                        }
                        return
                    }
                }

                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                    if (never) {
                        XXPermissions.startPermissionActivity(
                            this@AvailableWifiActivity,
                            permissions
                        )
                    } else {
                        showToast("Please enable permission")
                    }
                }
            })
    }


    private fun showGPSSettingsAlert() {
        showLongToast("Please enable location to scan nearby wifi")
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivityForResult(intent, RC_GPS_SETTINGS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_GPS_SETTINGS) {
            if (isLocationEnabled(this@AvailableWifiActivity)) {
                WifiUtils.enableLog(true)
                WifiUtils.withContext(applicationContext).enableWifi(this@AvailableWifiActivity::checkResult)
            }
        }
    }

    private fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }


    private fun checkResult(isSuccess: Boolean) {
        if (isSuccess) {
            WifiUtils.withContext(applicationContext).scanWifi(this::getScanResults).start();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startActivityForResult(Intent(Settings.Panel.ACTION_WIFI), 1)
            }
            showLongToast("Could not enable wifi. Please enable manually")
        }
    }

    private fun getScanResults(@NonNull results: List<ScanResult>) {
        if (results.isEmpty()) {
            Timber.i("SCAN RESULTS IT'S EMPTY")
            return
        }
        Timber.i("GOT SCAN RESULTS $results")
        Timber.i("GOT SCAN RESULTS " + results.size)
        results.forEachIndexed { index, scanResult ->
            listofwifi.add(WifiInfo(index, scanResult.SSID))
        }
        binding.progressBar.visibility = View.GONE
        wifiAdapter.listOfWifi = listofwifi
    }

    private fun initAdapter() {
        wifiAdapter = WifiAdapter(this).apply {
            wifiActionState.subscribeAndObserveOnMainThread {
                startActivityWithDefaultAnimation(SelectWifiActivity.getIntent(this@AvailableWifiActivity, it.wifiName))
                finish()
            }.autoDispose()
        }
        binding.wifiList.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        binding.wifiList.apply {
            adapter = wifiAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        initUI()
    }
}