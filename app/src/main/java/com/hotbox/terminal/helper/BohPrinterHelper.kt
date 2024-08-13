package com.hotbox.terminal.helper

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.text.SpannableStringBuilder
import com.epson.epos2.Epos2Exception
import com.epson.epos2.printer.Printer
import com.epson.epos2.printer.PrinterStatusInfo
import com.hotbox.terminal.api.order.model.OrderDetailsResponse
import com.hotbox.terminal.base.extension.showToast
import com.hotbox.terminal.utils.Constants
import timber.log.Timber

class BohPrinterHelper private constructor() : com.epson.epos2.printer.ReceiveListener {
    private lateinit var bohPrinter: Printer

    companion object {
        @Volatile
        private var instance: BohPrinterHelper? = null

        @SuppressLint("StaticFieldLeak")
        private lateinit var activity: Activity
        fun getInstance(activity: Activity): BohPrinterHelper {
            this.activity = activity
            return instance ?: synchronized(this) {
                instance ?: BohPrinterHelper().also { instance = it }
            }
        }
    }

    fun printerInitialize(context: Context) {
        try {
            bohPrinter = Printer(Printer.TM_T88, Printer.MODEL_ANK, context)
            bohPrinter.setReceiveEventListener(this)
        } catch (e: Epos2Exception) {
            Timber.e(e)
            activity.runOnUiThread {
                context.showToast(e.message.toString())
            }
        }
    }

    fun printerConnect(bohPrintAddress: String?): Boolean {
        Timber.tag("Printer").i("BOH Connect Printer")
        try {
            bohPrinter.connect("TCP:$bohPrintAddress", Printer.PARAM_DEFAULT)
        } catch (e: Epos2Exception) {
            Timber.e(e, "connect : $e")
            return false
        }
        return true
    }

    fun isPrinterConnected(): Boolean {
        return bohPrinter.status.connection == 1
    }

    fun runPrintBOHReceiptSequence(listOfOderDetails: OrderDetailsResponse, bohPrintAddress: String?): Boolean {
        bohPrinter.clearCommandBuffer()
        val textData = SpannableStringBuilder("")
        if (listOfOderDetails.guest?.isEmpty() == true) {
            if (listOfOderDetails.user?.fullName()?.isNotEmpty() == true) {
                textData.append("\n${listOfOderDetails.user.fullName()}")
            }
        } else {
            if (listOfOderDetails.guest?.firstOrNull()?.fullName()?.isNotEmpty() == true) {
                textData.append("\n${listOfOderDetails.guest.firstOrNull()?.fullName()}")
            }
        }
        textData.append("\n\n")

        if (!listOfOderDetails.orderPromisedTime?.trim().isNullOrEmpty()) {
            textData.append("\n")
            textData.append(
                spaceBetweenProductAndPrice(
                    listOfOderDetails.orderPromisedTime?.toDate("yyyy-MM-dd HH:mm a")?.formatTo("MMMM dd, yyyy").toString(),
                    listOfOderDetails.orderPromisedTime?.toDate("yyyy-MM-dd hh:mm a")?.formatTo("hh:mm:ss a").toString()
                )
            )
        }
        textData.append("\n------------------------------------------\n")
        if (listOfOderDetails.id != 0 && listOfOderDetails.id != null) {
            textData.append(spaceBetweenProductAndPrice(Constants.RECEIPT, "#${listOfOderDetails.id}"))
        }
        if (listOfOderDetails.orderMode?.modeName?.isNotEmpty() == true && listOfOderDetails.orderType?.subcategory?.isNotEmpty() == true) {
            textData.append("\n")
            textData.append("${listOfOderDetails.orderMode.modeName} - ${listOfOderDetails.orderType.subcategory}")
        }
        textData.append("\n------------------------------------------\n")
        bohPrinter.addTextSize(1, 2)
        if (listOfOderDetails.cartGroup?.cart?.isNotEmpty() == true) {
            listOfOderDetails.cartGroup.cart.forEach {
                bohPrinter.addHPosition(0)
                if (it.menuItemQuantity != null && !it.menu?.product?.productName.isNullOrEmpty()) {
                    bohPrinter.addTextSmooth(1)
                    bohPrinter.addTextStyle(1, 0, 0, 0)
                    textData.append("\n  ${it.menuItemQuantity} x ${it.menu?.product?.productName} \n")
                }
                bohPrinter.addTextStyle(0, 0, 0, 0)
                if (!it.menuItemModifiers.isNullOrEmpty()) {
                    it.menuItemModifiers.forEach { item ->
                        item.options?.forEach { item1 ->
                            bohPrinter.addHPosition(40)
                            if (!item1.optionName.isNullOrEmpty()) {
                                textData.append("\t${item1.optionName}\n")
                            }

                        }
                    }

                }
                it.menuItemInstructions?.trim()?.let {
                    textData.append("Note :$it")
                }
            }
        }
        Timber.tag("OkHttpClient").i(textData.toString())
        if (!createBohReceiptData(listOfOderDetails)) {
            return false
        }
        return printBohData(bohPrintAddress)
    }

    private fun printBohData(bohPrintAddress: String?): Boolean {
        if (bohPrinter == null) {
            return false
        }

        if (!isPrinterConnected()) {
            bohPrinter.clearCommandBuffer()
            return false
        }
        try {
            bohPrinter.sendData(Printer.PARAM_DEFAULT)
        } catch (e: Epos2Exception) {
            return false
        }
        return true
    }

    private fun createBohReceiptData(orderDetails: OrderDetailsResponse): Boolean {
        if (bohPrinter == null) {
            return false
        }
        try {
            bohPrinter.addTextLang(Printer.LANG_EN)
            bohPrinter.addTextAlign(Printer.ALIGN_LEFT)
            bohPrinter.addTextSize(2, 3)
            bohPrinter.addTextSmooth(1)
            bohPrinter.addTextStyle(1, 0, 0, 0)
            if (orderDetails.guest?.isEmpty() == true) {
                if (orderDetails.user?.fullName()?.isNotEmpty() == true) {
                    bohPrinter.addText("\n${orderDetails.user.fullName()}")
                }
            } else {
                if (orderDetails.guest?.firstOrNull()?.fullName()?.isNotEmpty() == true) {
                    bohPrinter.addText("\n${orderDetails.guest.firstOrNull()?.fullName()}")
                }
            }
            bohPrinter.addFeedLine(3)
            bohPrinter.addTextStyle(0, 0, 0, 0)
            bohPrinter.addTextSize(1, 1)
            bohPrinter.addText("\n\n")
            bohPrinter.addTextAlign(Printer.ALIGN_LEFT)
            if (!orderDetails.orderPromisedTime?.trim().isNullOrEmpty()) {
                bohPrinter.addText("\n")
                bohPrinter.addText(
                    spaceBetweenProductAndPrice(
                        orderDetails.orderPromisedTime?.toDate("yyyy-MM-dd HH:mm a")?.formatTo("MMMM dd, yyyy").toString(),
                        orderDetails.orderPromisedTime?.toDate("yyyy-MM-dd hh:mm a")?.formatTo("hh:mm:ss a").toString()
                    )
                )
            }
            bohPrinter.addTextAlign(Printer.ALIGN_RIGHT)
            bohPrinter.addText("\n------------------------------------------\n")
            bohPrinter.addTextAlign(Printer.ALIGN_CENTER)
            if (orderDetails.id != 0 && orderDetails.id != null) {
                bohPrinter.addTextSize(1, 2)
                bohPrinter.addText(spaceBetweenProductAndPrice(Constants.RECEIPT, "#${orderDetails.id}"))
            }
            bohPrinter.addTextSize(1, 1)
            if (orderDetails.orderMode?.modeName?.isNotEmpty() == true && orderDetails.orderType?.subcategory?.isNotEmpty() == true) {
                bohPrinter.addText("\n")
                bohPrinter.addText("${orderDetails.orderMode.modeName} - ${orderDetails.orderType.subcategory}")
            }
            bohPrinter.addTextAlign(Printer.ALIGN_LEFT)
            bohPrinter.addText("\n------------------------------------------\n")
            bohPrinter.addTextAlign(Printer.ALIGN_LEFT)
            bohPrinter.addTextSize(1, 2)
            if (orderDetails.cartGroup?.cart?.isNotEmpty() == true) {
                orderDetails.cartGroup.cart.forEach {
                    bohPrinter.addHPosition(0)
                    if (it.menuItemQuantity != null && !it.menu?.product?.productName.isNullOrEmpty()) {
                        bohPrinter.addTextSmooth(1)
                        bohPrinter.addTextStyle(1, 0, 0, 0)
                        bohPrinter.addText("\n  ${it.menuItemQuantity} x ${it.menu?.product?.productName} \n")
                    }
                    bohPrinter.addTextStyle(0, 0, 0, 0)
                    if (!it.menuItemModifiers.isNullOrEmpty()) {
                        it.menuItemModifiers.forEach { item ->
                            item.options?.forEach { item1 ->
                                bohPrinter.addHPosition(40)
                                if (!item1.optionName.isNullOrEmpty()) {
                                    bohPrinter.addText("\t${item1.optionName}\n")
                                }

                            }
                        }

                    }
                    it.menuItemInstructions?.trim()?.let {
                        bohPrinter.addText("Note :$it")
                    }
                }
            }
            bohPrinter.addFeedLine(3)
            bohPrinter.addCut(Printer.CUT_FEED)
        } catch (e: java.lang.Exception) {
            return false
        }
        return true
    }

    private fun createBohReceiptsData(listOfOderDetails: List<OrderDetailsResponse>): Boolean {
        listOfOderDetails.forEach {
            createBohReceiptData(it)
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


    override fun onPtrReceive(printerObj: Printer?, code: Int, status: PrinterStatusInfo?, printJobId: String?) {
        activity.runOnUiThread {
            disconnectPrinter()
        }
    }


    private fun disconnectPrinter() {
        if (bohPrinter == null) {
            return
        }
        while (true) {
            try {
                bohPrinter.disconnect()
                break
            } catch (e: Epos2Exception) {
                Timber.e(e)
            }
        }
    }
}