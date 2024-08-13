package com.hotbox.terminal.ui.main.orderdetail

import android.Manifest
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.epson.epos2.Epos2Exception
import com.epson.epos2.printer.Printer
import com.epson.epos2.printer.PrinterStatusInfo
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.gson.Gson
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.order.model.OrderDetailsResponse
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseDialogFragment
import com.hotbox.terminal.base.RxBus
import com.hotbox.terminal.base.RxEvent
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.PrintReceiptDialogBinding
import com.hotbox.terminal.helper.*
import com.hotbox.terminal.helper.formatTo
import com.hotbox.terminal.helper.toDate
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject

class PrintReceiptDialog : BaseDialogFragment() {

    private var _binding: PrintReceiptDialogBinding? = null
    private val binding get() = _binding!!

    private val REQUEST_PERMISSION = 100
    private val DISCONNECT_INTERVAL = 500

    private val TAG = PrintReceiptDialog::class.java.simpleName
    private val printReceiptSubject: PublishSubject<String> = PublishSubject.create()
    val printReceiptDismissed: Observable<String> = printReceiptSubject.hide()

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private lateinit var printer: Printer
    private lateinit var bohPrinter: Printer
    lateinit var orderDetails: OrderDetailsResponse

    companion object {
        const val INTENT_CART_GROUP = "Intent Cart Group"
        fun newInstance(messageInfo: OrderDetailsResponse?): PrintReceiptDialog {
            val args = Bundle()
            val gson = Gson()
            val json: String = gson.toJson(messageInfo)
            json.let { args.putString(INTENT_CART_GROUP, it) }
            val fragment = PrintReceiptDialog()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        setStyle(STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = PrintReceiptDialogBinding.inflate(inflater, container, false)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.decorView?.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestRuntimePermission()
        enableLocationSetting()
        listenToViewEvent()

    }

    private fun listenToViewEvent() {
        val productsDetails = arguments?.getString(INTENT_CART_GROUP)
        val gson = Gson()
        orderDetails = gson.fromJson(productsDetails, OrderDetailsResponse::class.java)
        val fohPrintAddress = loggedInUserCache.getLocationInfo()?.printAddress ?: throw Exception("printAddress not found")
        val bohPrintAddress = loggedInUserCache.getLocationInfo()?.bohPrintAddress ?: throw Exception("bohPrintAddress not found")
        binding.ipAddress.text = "FOH: $fohPrintAddress & BOH: $bohPrintAddress "
        binding.progressBar.isVisible = false
        try {
            printer = Printer(Printer.TM_T88, Printer.MODEL_ANK, requireContext())
        } catch (e: Epos2Exception) {
            showToast(e.message.toString())
        }
        try {
            bohPrinter = Printer(Printer.TM_T88, Printer.MODEL_ANK, requireContext())
        } catch (e: Epos2Exception) {
            showToast(e.message.toString())
        }

        binding.printBOHReceiptButton.throttleClicks().subscribeAndObserveOnMainThread {
           PrintReceiptHelper(requireContext(),requireActivity()).runPrintBOHReceiptSequence(printer,orderDetails,bohPrintAddress)
//            val bohPrinterHelper = BohPrinterHelper.getInstance()
//            bohPrinterHelper.runPrintBOHReceiptSequence(orderDetails, promisedTime, bohPrintAddress)
        }.autoDispose()

        binding.printFOHReceiptButton.throttleClicks().subscribeAndObserveOnMainThread {
            val timeZone = loggedInUserCache.getStoreResponse()?.locationLocationTimezone ?:"America/Los_Angeles"
            val currentTime = getCurrentsStoreTime(timeZone).formatToStoreTime("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            PrintReceiptHelper(requireContext(), requireActivity()).runPrintReceiptSequence(
                requireContext(),
                printer,
                orderDetails,
                loggedInUserCache,
                fohPrintAddress,
                currentTime
            )
//            val fohPrinterHelper = FohPrinterHelper.getInstance()
//            fohPrinterHelper.runPrintReceiptSequence(requireContext(),orderDetails,loggedInUserCache,fohPrintAddress)
        }.autoDispose()

        binding.closeButtonMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()
    }


    private fun spaceBetweenProductAndPrice(product: String, price: String): String {
        val l = "${product}${price}".length;
        if (l < 42) {
            val s = 42 - l;

            var space: String = ""
            for (i in 1..s) {
                space = "$space ";
            }
            return product.plus(space).plus(price)
        } else {
            val s = 42 - price.length;

            var space: String = ""
            for (i in 1..s) {
                space = "$space ";
            }
            return product.plus("\n$space").plus(price)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            printer.disconnect()
        } catch (e: Epos2Exception) {

        }
    }

    private fun connectFohPrinter(fohPrintAddress: String): Boolean {
        try {
            printer.connect("TCP:$fohPrintAddress", Printer.PARAM_DEFAULT)
        } catch (e: java.lang.Exception) {
            showToast(makeStatusMassage(printer.status))
            Timber.e(e, "connect : $e")
            return false
        }
        return true
    }

    private fun connectBohPrinter(fohPrintAddress: String): Boolean {
        try {
            bohPrinter.connect("TCP:$fohPrintAddress", Printer.PARAM_DEFAULT)
        } catch (e: java.lang.Exception) {
            showToast(makeStatusMassage(bohPrinter.status))
            Timber.e(e, "connect : $e")
            return false
        }
        return true
    }

    private fun makeStatusMassage(statusInfo: PrinterStatusInfo): String {
        var msg = ""
        msg += "connection:"
        when (statusInfo.connection) {
            Printer.TRUE -> msg += "CONNECT"
            Printer.FALSE -> msg += "DISCONNECT"
            Printer.UNKNOWN -> msg += "UNKNOWN"
            else -> {}
        }
        msg += "\n"
        msg += "online:"
        when (statusInfo.online) {
            Printer.TRUE -> msg += "ONLINE"
            Printer.FALSE -> msg += "OFFLINE"
            Printer.UNKNOWN -> msg += "UNKNOWN"
            else -> {}
        }
        return msg
    }


    private fun requestRuntimePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }
        val requestPermissions: MutableList<String> = ArrayList()
        if (Build.VERSION_CODES.S <= Build.VERSION.SDK_INT) {
            // If your app targets Android 12 (API level 31) and higher, it's recommended that you declare BLUETOOTH permission.
            val permissionBluetoothScan = ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_SCAN)
            val permissionBluetoothConnect = ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_CONNECT)
            if (permissionBluetoothScan == PackageManager.PERMISSION_DENIED) {
                requestPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (permissionBluetoothConnect == PackageManager.PERMISSION_DENIED) {
                requestPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT && Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            // If your app targets Android 11 (API level 30) or lower, it's necessary that you declare ACCESS_FINE_LOCATION permission.
            val permissionLocationFine = ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
            if (permissionLocationFine == PackageManager.PERMISSION_DENIED) {
                requestPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        } else {
            // If your app targets Android 9 (API level 28) or lower, you can declare the ACCESS_COARSE_LOCATION permission instead of the ACCESS_FINE_LOCATION permission.
            val permissionLocationCoarse = ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
            if (permissionLocationCoarse == PackageManager.PERMISSION_DENIED) {
                requestPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
        if (!requestPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(requireActivity(), requestPermissions.toTypedArray<String>(), REQUEST_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode != REQUEST_PERMISSION || grantResults.isEmpty()) {
            return
        }
        val requestPermissions: MutableList<String> = ArrayList()
        for (i in permissions.indices) {
            if (Build.VERSION_CODES.S <= Build.VERSION.SDK_INT) {
                // If your app targets Android 12 (API level 31) and higher, it's recommended that you declare BLUETOOTH permission.
                if (permissions[i] == Manifest.permission.BLUETOOTH_SCAN && grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    requestPermissions.add(permissions[i])
                }
                if (permissions[i] == Manifest.permission.BLUETOOTH_CONNECT && grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    requestPermissions.add(permissions[i])
                }
            } else if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT && Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                // If your app targets Android 11 (API level 30) or lower, it's necessary that you declare ACCESS_FINE_LOCATION permission.
                if (permissions[i] == Manifest.permission.ACCESS_FINE_LOCATION && grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    requestPermissions.add(permissions[i])
                }
            } else {
                // If your app targets Android 9 (API level 28) or lower, you can declare the ACCESS_COARSE_LOCATION permission instead of the ACCESS_FINE_LOCATION permission.
                if (permissions[i] == Manifest.permission.ACCESS_COARSE_LOCATION && grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    requestPermissions.add(permissions[i])
                }
            }
        }
        if (requestPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(requireActivity(), requestPermissions.toTypedArray<String>(), REQUEST_PERMISSION)
        }
    }

    private fun enableLocationSetting() {
        val locationRequest: LocationRequest = LocationRequest.create()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val builder: LocationSettingsRequest.Builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(requireActivity())
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener(requireActivity()) {
            // All location settings are satisfied. The client can initialize
            // location requests here.
            // ...
        }
        task.addOnFailureListener(requireActivity()) { e ->
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    val resolvable: ResolvableApiException = e as ResolvableApiException
                    resolvable.startResolutionForResult(
                        requireActivity(), CommonStatusCodes.RESOLUTION_REQUIRED
                    )
                } catch (sendEx: SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        RxBus.listen(RxEvent.DismissedPrinterDialog::class.java).subscribeOnIoAndObserveOnMainThread({
            printReceiptSubject.onNext("")
        },{
            Timber.e(it)
        }).autoDispose()
    }

}