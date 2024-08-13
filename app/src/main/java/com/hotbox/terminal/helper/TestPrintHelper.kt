package com.hotbox.terminal.helper

import android.app.Activity
import android.content.Context
import com.epson.epos2.Epos2Exception
import com.epson.epos2.printer.Printer
import com.epson.epos2.printer.PrinterStatusInfo
import com.hotbox.terminal.base.RxBus
import com.hotbox.terminal.base.RxEvent
import com.hotbox.terminal.base.extension.showToast
import timber.log.Timber

class TestPrintHelper(val context: Context, val activity: Activity) : com.epson.epos2.printer.ReceiveListener {

    fun runPrintBOHReceiptSequence(printer: Printer, bohPrintAddress: String): Boolean {
        if (!createBohReceiptData(printer)) {
            return false
        }
        return printBohData(printer, bohPrintAddress)
    }

    private fun printBohData(printer: Printer, bohPrintAddress: String): Boolean {
        if (printer == null) {
            return false
        }

        if (!connectPrinter(printer, bohPrintAddress)) {
            printer.clearCommandBuffer()
            return false
        }
        try {
            printer.sendData(Printer.PARAM_DEFAULT)
        } catch (e: Epos2Exception) {
            Timber.e(e)
        }
        return true
    }

    private fun createBohReceiptData(printer: Printer): Boolean {
        if (printer == null) {
            return false
        }
        try {
            printer.clearCommandBuffer()
            printer.addTextLang(Printer.LANG_EN)
            printer.addTextAlign(Printer.ALIGN_CENTER)
            printer.addTextSize(2, 3)
            printer.addText("Test Successful")
            printer.addFeedLine(15)
            printer.addCut(Printer.CUT_FEED)
        } catch (e: Epos2Exception) {
            Timber.e(e)
        }
        return true
    }


    fun runPrintReceiptSequence(
        printer: Printer, fohPrintAddress: String
    ): Boolean {
        if (!createReceiptData(printer)) {
            return false
        }
        return printData(printer, fohPrintAddress)
    }

    private fun createReceiptData(
        printer: Printer
    ): Boolean {
        if (printer == null) {
            return false
        }
        try {
            printer.clearCommandBuffer()
            printer.addTextLang(Printer.LANG_EN)
            printer.addTextAlign(Printer.ALIGN_CENTER)
            printer.addTextSize(2, 3)
            printer.addText("Test Successful")
            printer.addFeedLine(15)
            printer.addCut(Printer.CUT_FEED)
        } catch (e: Epos2Exception) {
            printer.clearCommandBuffer()
            return false
        }
        return true
    }

    private fun printData(printer: Printer, fohPrintAddress: String): Boolean {
        if (printer == null) {
            return false
        }

        if (!connectPrinter(printer, fohPrintAddress)) {
            printer.clearCommandBuffer()
            return false
        }
        try {
            printer.sendData(Printer.PARAM_DEFAULT)
        } catch (e: Epos2Exception) {
            Timber.e(e)
        }
        return true
    }

    private fun connectPrinter(printer: Printer, printAddress: String): Boolean {
        try {
            printer.setReceiveEventListener(this)
            printer.connect("TCP:$printAddress", Printer.PARAM_DEFAULT)
        } catch (e: Epos2Exception) {
            Timber.e(e, "connect : $e")
            return false
        }
        return true
    }

    override fun onPtrReceive(printerObj: Printer?, code: Int, status: PrinterStatusInfo?, printJobId: String?) {
        activity.runOnUiThread {
            disconnectPrinter(printerObj)
        }
    }

    private fun disconnectPrinter(printer: Printer?) {
        if (printer == null) {
            return
        }
        while (true) {
            try {
                printer.disconnect()
                break
            } catch (e: Epos2Exception) {
                Timber.e(e)
            }
        }
    }
}