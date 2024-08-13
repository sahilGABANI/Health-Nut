package com.hotbox.terminal.helper

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import com.epson.epos2.Epos2CallbackCode
import com.epson.epos2.Epos2Exception
import com.epson.epos2.printer.Printer
import com.epson.epos2.printer.PrinterStatusInfo
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.order.model.OrderDetailsResponse
import com.hotbox.terminal.base.extension.showToast
import com.hotbox.terminal.base.extension.toDollar
import com.hotbox.terminal.base.extension.toMinusDollar
import com.hotbox.terminal.ui.main.MainActivity
import com.hotbox.terminal.utils.Constants
import timber.log.Timber

class FohPrinterHelper private constructor() : com.epson.epos2.printer.ReceiveListener {


    private lateinit var fohPrinter : Printer
    private lateinit var context : Context

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: FohPrinterHelper? = null
        private lateinit var activity : Activity
        fun getInstance(mainActivity: Activity): FohPrinterHelper {
            activity = mainActivity
            return instance ?: synchronized(this) {
                instance ?: FohPrinterHelper().also { instance = it }
            }
        }
    }

    fun printerInitialize(context : Context) {
        this.context = context
        try {
            fohPrinter = Printer(Printer.TM_T88, Printer.MODEL_ANK, context)
        } catch (e: Epos2Exception) {
            context.showToast(e.message.toString())
        }
    }

    fun printerConnect(fohPrintAddress :String?) :Boolean {
        Timber.tag("Printer").i("FOH Connect Printer")
        try {
            fohPrinter.connect("TCP:$fohPrintAddress", Printer.PARAM_DEFAULT)
        } catch (e: java.lang.Exception) {
            fohPrinter.setReceiveEventListener(null)
            return false
        }
        return true
    }

    fun printerReceiverAdd() {
        fohPrinter.setReceiveEventListener(this)
    }

    fun runPrintReceiptSequence(
        context: Context, orderDetails: OrderDetailsResponse, loggedInUserCache: LoggedInUserCache, fohPrintAddress: String?
    ): Boolean {
        if (!createReceiptData(context, orderDetails, loggedInUserCache)) {
            return false
        }
        return printData(fohPrintAddress)
    }

    private fun createReceiptData(
        context: Context, orderDetails: OrderDetailsResponse, loggedInUserCache: LoggedInUserCache
    ): Boolean {
        val logoData = BitmapFactory.decodeResource(context.resources, R.drawable.healthnuts_printer_image)
        if (fohPrinter == null) {
            return false
        }
        try {
            fohPrinter.clearCommandBuffer()
            fohPrinter.addTextAlign(Printer.ALIGN_LEFT)
            if (!orderDetails.orderLocation?.locationName?.trim().isNullOrEmpty()) {
                fohPrinter.addText("Health Nut (${orderDetails.orderLocation?.locationName})\n")
            }
            fohPrinter.addTextAlign(Printer.ALIGN_LEFT)
            if (!orderDetails.orderLocation?.locationAddress1?.trim().isNullOrEmpty()) {
                fohPrinter.addText("${orderDetails.orderLocation?.locationAddress1}")
            }
            println("\n")
            if (orderDetails.orderLocation?.locationAddress2?.isNotEmpty() == true) {
                fohPrinter.addTextAlign(Printer.ALIGN_LEFT)
                fohPrinter.addText("${orderDetails.orderLocation.locationAddress2}")
            }

            fohPrinter.addTextAlign(Printer.ALIGN_LEFT)
            if (!orderDetails.orderLocation?.locationCity.isNullOrEmpty() && !orderDetails.orderLocation?.locationState.isNullOrEmpty()) {
                fohPrinter.addText("\n${loggedInUserCache.getStoreResponse()?.locationLocationCity},${loggedInUserCache.getStoreResponse()?.locationLocationState}")
            }
            if (!orderDetails.orderLocation?.locationZip.isNullOrEmpty()) {
                fohPrinter.addText(",${loggedInUserCache.getStoreResponse()?.locationLocationZip}")
            }
            fohPrinter.addTextAlign(Printer.ALIGN_LEFT)
            fohPrinter.addText("\n${loggedInUserCache.getlocationPhone()}")
            fohPrinter.addText("\n${Constants.WEB_SITE}")
            fohPrinter.addText("\n${Constants.MENTION}\n")
            fohPrinter.addTextAlign(Printer.ALIGN_LEFT)
            if (!orderDetails.orderCreationDate?.trim().isNullOrEmpty()) {
                fohPrinter.addText(spaceBetweenProductAndPrice(orderDetails.orderCreationDate?.toDate()?.formatTo("MMMM dd, yyyy").toString(), orderDetails.orderCreationDate?.toDate()?.formatTo("hh:mm a").toString()))
            }
            fohPrinter.addText("\n------------------------------------------")
            if (orderDetails.id != 0 && orderDetails.id != null) {
                fohPrinter.addText(spaceBetweenProductAndPrice(Constants.RECEIPT,"#${orderDetails.id}"))
            }
            fohPrinter.addText("\n------------------------------------------")
            fohPrinter.addTextAlign(Printer.ALIGN_LEFT)
            if (!orderDetails.cartGroup?.cart.isNullOrEmpty()) {
                orderDetails.cartGroup?.cart?.forEach {
                    var productPrice: Double = 0.00
                    if (it.menuItemPrice != null){
                        productPrice =  it.menuItemPrice ?: 0.00
                    }
                    it.menuItemModifiers?.forEach {
                        it.options?.forEach {
                            productPrice = productPrice.plus(it.optionPrice?.toInt() ?: 0)
                        }
                    }
                    fohPrinter.addText("\n")
                    if (!it.menu?.product?.productName?.trim().isNullOrEmpty()) {
                        fohPrinter.addText(spaceBetweenProductAndPrice(it.menu?.product?.productName.toString().plus(" X ${it.menuItemQuantity}"), productPrice.div(100).toDollar()))
                    }
                    fohPrinter.addTextAlign(Printer.ALIGN_LEFT)
                    if (!it.menuItemModifiers.isNullOrEmpty()) {
                        it.menuItemModifiers.forEach { item ->
                            item.options?.forEach { item1 ->
                                if (item.options.firstOrNull()?.equals(item1) == true) {
                                    if (item1.optionPrice != null && item1.optionPrice != 0.0 && !item1.optionName.isNullOrEmpty() ) {
//                                        fohPrinter.addText("\n\t-${item1.optionName} ${(item1.optionPrice.div(100).toInt() ?: 0).toDollar()}\n")
                                    } else {
                                        if (!item1.optionName.isNullOrEmpty()){
                                            fohPrinter.addText("\n\t-${item1.optionName}\n")
                                        }
                                    }
                                } else {
                                    if (item1.optionPrice != null && item1.optionPrice != 0.0 && !item1.optionName.isNullOrEmpty()) {
//                                        fohPrinter.addText("\n\t-${item1.optionName} ${(item1.optionPrice.div(100).toInt() ?: 0).toDollar()}\n")
                                    } else {
                                        if (!item1.optionName.isNullOrEmpty()){
                                            fohPrinter.addText("\n\t-${item1.optionName}\n")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    it.menuItemInstructions?.trim()?.let {
                        fohPrinter.addTextAlign(Printer.ALIGN_LEFT)
                        fohPrinter.addText("\nNote :$it\n")
                    }
                }
            }

            fohPrinter.addTextAlign(Printer.ALIGN_LEFT)
            fohPrinter.addText("\n------------------------------------------\n")
            if (orderDetails.orderSubtotal != null) {
                fohPrinter.addText(spaceBetweenProductAndPrice("Subtotal", orderDetails.orderSubtotal.div(100).toDollar()))
            }
            if (orderDetails.orderDeliveryFee != null && orderDetails.orderDeliveryFee != 0.00 && orderDetails.orderType?.isDelivery == true) {
                fohPrinter.addText("\n")
                fohPrinter.addText(spaceBetweenProductAndPrice("Delivery Fee", orderDetails.orderDeliveryFee?.div(100).toDollar()))
            }
            if (orderDetails.orderTax != null) {
                fohPrinter.addText("\n")
                fohPrinter.addText(spaceBetweenProductAndPrice("Sales Tax", orderDetails.orderTax.div(100).toDollar()))
            }
            if (orderDetails.orderTip != null && orderDetails.orderTip != 0.00) {
                fohPrinter.addText("\n")
                fohPrinter.addText(spaceBetweenProductAndPrice("Tip", orderDetails.orderTip.div(100).toDollar()))
            }
            if (orderDetails.orderGiftCardAmount != null && orderDetails.orderGiftCardAmount != 0.00) {
                fohPrinter.addText("\n")
                fohPrinter.addText(
                    spaceBetweenProductAndPrice(
                        "Gift Cart",
                        "-${(orderDetails.orderGiftCardAmount.div(100)).toMinusDollar().toDollar()}"
                    )
                )
            }
            if (orderDetails.orderCouponCodeDiscount != null && orderDetails.orderCouponCodeDiscount != 0.00) {
                fohPrinter.addText("\n")
                fohPrinter.addText(
                    spaceBetweenProductAndPrice(
                        "Promocode",
                        "-${(orderDetails.orderCouponCodeDiscount.div(100)).toMinusDollar().toDollar()}"
                    )
                )
            }
            fohPrinter.addText("\n------------------------------------------\n")
            if (orderDetails.orderTotal != null) {
                fohPrinter.addText(spaceBetweenProductAndPrice("Total", orderDetails.orderTotal?.div(100).toDollar()))
            }
            fohPrinter.addTextAlign(Printer.ALIGN_CENTER)
            fohPrinter.addText("\n")
            fohPrinter.addTextAlign(Printer.ALIGN_CENTER)
            fohPrinter.addText("\n")
            fohPrinter.addText(Constants.FRESH_OBSESSED)
            fohPrinter.addFeedLine(3)
            fohPrinter.addCut(Printer.CUT_FEED)
        } catch (e: java.lang.Exception) {
            fohPrinter.clearCommandBuffer()
            try {
                fohPrinter.disconnect()
            } catch (ex: java.lang.Exception) {
                // Do nothing
            }
            return false
        }
        return true
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

    private fun printData(fohPrintAddress: String?): Boolean {
        if (fohPrinter == null) {
            return false
        }
        if (!printerConnect(fohPrintAddress)) {
            fohPrinter.clearCommandBuffer()
            return false
        }
        try {
            fohPrinter.sendData(Printer.PARAM_DEFAULT)
        } catch (e: Exception) {
            fohPrinter.clearCommandBuffer()
            try {
                fohPrinter.disconnect()
            } catch (ex: Exception) {
                return false
                // Do nothing
            }
        }
        return true
    }

    override fun onPtrReceive(printerObj: Printer?, code: Int, status: PrinterStatusInfo?, printJobId: String?) {
        activity.runOnUiThread {
            disconnectPrinter()
        }

    }

    fun disconnectPrinter() {
        fohPrinter.setReceiveEventListener(null)
        if (fohPrinter == null) {
            return
        }
        while (true) {
            try {
                fohPrinter.disconnect()
                break
            } catch (e: java.lang.Exception) {
                if (e is Epos2Exception) {
                    //Note: If printer is processing such as printing and so on, the disconnect API returns ERR_PROCESSING.
                    if (e.errorStatus == Epos2Exception.ERR_PROCESSING) {
                        try {
//                            Thread.sleep(DISCONNECT_INTERVAL.toLong())
                        } catch (ex: java.lang.Exception) {
                        }
                    } else {
//                        requireActivity().runOnUiThread(Runnable { ShowMsg.showException(e, "disconnect", mContext) })
                        break
                    }
                } else {
//                    requireActivity().runOnUiThread(Runnable { ShowMsg.showException(e, "disconnect", mContext) })
                    break
                }
            }
        }
    }

    private fun getCodeText(state: Int): String? {
        var return_text = ""
        return_text = when (state) {
            Epos2CallbackCode.CODE_SUCCESS -> "PRINT_SUCCESS"
            Epos2CallbackCode.CODE_PRINTING -> "PRINTING"
            Epos2CallbackCode.CODE_ERR_AUTORECOVER -> "ERR_AUTORECOVER"
            Epos2CallbackCode.CODE_ERR_COVER_OPEN -> "ERR_COVER_OPEN"
            Epos2CallbackCode.CODE_ERR_CUTTER -> "ERR_CUTTER"
            Epos2CallbackCode.CODE_ERR_MECHANICAL -> "ERR_MECHANICAL"
            Epos2CallbackCode.CODE_ERR_EMPTY -> "ERR_EMPTY"
            Epos2CallbackCode.CODE_ERR_UNRECOVERABLE -> "ERR_UNRECOVERABLE"
            Epos2CallbackCode.CODE_ERR_FAILURE -> "ERR_FAILURE"
            Epos2CallbackCode.CODE_ERR_NOT_FOUND -> "ERR_NOT_FOUND"
            Epos2CallbackCode.CODE_ERR_SYSTEM -> "ERR_SYSTEM"
            Epos2CallbackCode.CODE_ERR_PORT -> "ERR_PORT"
            Epos2CallbackCode.CODE_ERR_TIMEOUT -> "ERR_TIMEOUT"
            Epos2CallbackCode.CODE_ERR_JOB_NOT_FOUND -> "ERR_JOB_NOT_FOUND"
            Epos2CallbackCode.CODE_ERR_SPOOLER -> "ERR_SPOOLER"
            Epos2CallbackCode.CODE_ERR_BATTERY_LOW -> "ERR_BATTERY_LOW"
            Epos2CallbackCode.CODE_ERR_TOO_MANY_REQUESTS -> "ERR_TOO_MANY_REQUESTS"
            Epos2CallbackCode.CODE_ERR_REQUEST_ENTITY_TOO_LARGE -> "ERR_REQUEST_ENTITY_TOO_LARGE"
            Epos2CallbackCode.CODE_CANCELED -> "CODE_CANCELED"
            Epos2CallbackCode.CODE_ERR_NO_MICR_DATA -> "ERR_NO_MICR_DATA"
            Epos2CallbackCode.CODE_ERR_ILLEGAL_LENGTH -> "ERR_ILLEGAL_LENGTH"
            Epos2CallbackCode.CODE_ERR_NO_MAGNETIC_DATA -> "ERR_NO_MAGNETIC_DATA"
            Epos2CallbackCode.CODE_ERR_RECOGNITION -> "ERR_RECOGNITION"
            Epos2CallbackCode.CODE_ERR_READ -> "ERR_READ"
            Epos2CallbackCode.CODE_ERR_NOISE_DETECTED -> "ERR_NOISE_DETECTED"
            Epos2CallbackCode.CODE_ERR_PAPER_JAM -> "ERR_PAPER_JAM"
            Epos2CallbackCode.CODE_ERR_PAPER_PULLED_OUT -> "ERR_PAPER_PULLED_OUT"
            Epos2CallbackCode.CODE_ERR_CANCEL_FAILED -> "ERR_CANCEL_FAILED"
            Epos2CallbackCode.CODE_ERR_PAPER_TYPE -> "ERR_PAPER_TYPE"
            Epos2CallbackCode.CODE_ERR_WAIT_INSERTION -> "ERR_WAIT_INSERTION"
            Epos2CallbackCode.CODE_ERR_ILLEGAL -> "ERR_ILLEGAL"
            Epos2CallbackCode.CODE_ERR_INSERTED -> "ERR_INSERTED"
            Epos2CallbackCode.CODE_ERR_WAIT_REMOVAL -> "ERR_WAIT_REMOVAL"
            Epos2CallbackCode.CODE_ERR_DEVICE_BUSY -> "ERR_DEVICE_BUSY"
            Epos2CallbackCode.CODE_ERR_IN_USE -> "ERR_IN_USE"
            Epos2CallbackCode.CODE_ERR_CONNECT -> "ERR_CONNECT"
            Epos2CallbackCode.CODE_ERR_DISCONNECT -> "ERR_DISCONNECT"
            Epos2CallbackCode.CODE_ERR_MEMORY -> "ERR_MEMORY"
            Epos2CallbackCode.CODE_ERR_PROCESSING -> "ERR_PROCESSING"
            Epos2CallbackCode.CODE_ERR_PARAM -> "ERR_PARAM"
            Epos2CallbackCode.CODE_RETRY -> "RETRY"
            Epos2CallbackCode.CODE_ERR_DIFFERENT_MODEL -> "ERR_DIFFERENT_MODEL"
            Epos2CallbackCode.CODE_ERR_DIFFERENT_VERSION -> "ERR_DIFFERENT_VERSION"
            Epos2CallbackCode.CODE_ERR_DATA_CORRUPTED -> "ERR_DATA_CORRUPTED"
            Epos2CallbackCode.CODE_ERR_JSON_FORMAT -> "ERR_JSON_FORMAT"
            Epos2CallbackCode.CODE_NO_PASSWORD -> "NO_PASSWORD"
            Epos2CallbackCode.CODE_ERR_INVALID_PASSWORD -> "ERR_INVALID_PASSWORD"
            else -> String.format("%d", state)
        }
        return return_text
    }
}