package com.hotbox.terminal.ui.main.settings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.epson.epos2.Epos2Exception
import com.epson.epos2.printer.Printer
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.loyalty.model.AddLoyaltyPointRequest
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseFragment
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hotbox.terminal.base.extension.throttleClicks
import com.hotbox.terminal.databinding.FragmentOrdersBinding
import com.hotbox.terminal.databinding.FragmentSettingsBinding
import com.hotbox.terminal.helper.TestPrintHelper
import com.hotbox.terminal.ui.main.order.OrdersFragment
import com.hotbox.terminal.ui.userstore.AdminPinDialogFragment
import timber.log.Timber
import javax.inject.Inject

class SettingsFragment : BaseFragment() {
    companion object {
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var printer: Printer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        val fohPrintAddress = loggedInUserCache.getLocationInfo()?.printAddress ?: throw Exception("printAddress not found")
        val bohPrintAddress = loggedInUserCache.getLocationInfo()?.bohPrintAddress ?: throw Exception("bohPrintAddress not found")
        fohOrBohSelection(true)
        printerInitialize()
        binding.fohSelectLinear.throttleClicks().subscribeAndObserveOnMainThread {
            fohOrBohSelection(true)
        }.autoDispose()
        binding.bohSelectLinear.throttleClicks().subscribeAndObserveOnMainThread {
            fohOrBohSelection(false)
        }.autoDispose()

        binding.fohPrint.printReceiptButton.throttleClicks().subscribeAndObserveOnMainThread {
            val t: Thread = object : Thread() {
                override fun run() {
                    if (loggedInUserCache.isAdmin()) {
                        TestPrintHelper(requireContext(), requireActivity()).runPrintReceiptSequence(printer, fohPrintAddress)
                    } else {
                        val adminPinDialogFragment = AdminPinDialogFragment().apply {
                            adminPinSuccess.subscribeAndObserveOnMainThread {
                                TestPrintHelper(requireContext(), requireActivity()).runPrintReceiptSequence(printer, fohPrintAddress)
                            }.autoDispose()
                        }
                        adminPinDialogFragment.show(parentFragmentManager, AdminPinDialogFragment::class.java.name)
                    }
                }
            }
            t.start()
        }.autoDispose()

        binding.bohPrint.printReceiptButton.throttleClicks().subscribeAndObserveOnMainThread {
            val t: Thread = object : Thread() {
                override fun run() {
                    if (loggedInUserCache.isAdmin()) {
                        TestPrintHelper(requireContext(), requireActivity()).runPrintBOHReceiptSequence(printer, bohPrintAddress)
                    } else {
                        val adminPinDialogFragment = AdminPinDialogFragment().apply {
                            adminPinSuccess.subscribeAndObserveOnMainThread {
                                TestPrintHelper(requireContext(), requireActivity()).runPrintBOHReceiptSequence(printer, bohPrintAddress)
                            }.autoDispose()
                        }
                        adminPinDialogFragment.show(parentFragmentManager, AdminPinDialogFragment::class.java.name)
                    }
                }
            }
            t.start()
        }.autoDispose()
    }

    private fun printerInitialize() {
        try {
            printer = Printer(Printer.TM_T88, Printer.MODEL_ANK, requireContext())
        } catch (e: Epos2Exception) {
            Timber.e(e)
        }
    }

    private fun fohOrBohSelection(isPhysical: Boolean) {
        binding.fohSelectLinear.isSelected = isPhysical
        binding.fohTextview.isSelected = isPhysical
        binding.fohPrint.root.isVisible = isPhysical
        binding.bohSelectLinear.isSelected = !isPhysical
        binding.bohTextview.isSelected = !isPhysical
        binding.bohPrint.root.isVisible = !isPhysical
    }


}