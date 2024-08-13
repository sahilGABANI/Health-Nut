package com.hotbox.terminal.ui.main.deliveries

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentTransaction
import com.hotbox.terminal.R
import com.hotbox.terminal.api.order.model.OrdersInfo
import com.hotbox.terminal.api.order.model.SectionInfo
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseFragment
import com.hotbox.terminal.base.RxBus
import com.hotbox.terminal.base.RxEvent
import com.hotbox.terminal.base.ViewModelFactory
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.FragmentDeliveriesBinding
import com.hotbox.terminal.helper.formatTo
import com.hotbox.terminal.helper.toDate
import com.hotbox.terminal.ui.main.deliveries.viewmodel.DeliveriesViewModel
import com.hotbox.terminal.ui.main.deliveries.viewmodel.DeliveriesViewState
import com.hotbox.terminal.ui.main.order.view.OrderAdapter
import com.hotbox.terminal.utils.Constants
import java.util.*
import javax.inject.Inject

class DeliveriesFragment : BaseFragment() {
    companion object {
        @JvmStatic
        fun newInstance() = DeliveriesFragment()
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<DeliveriesViewModel>
    private lateinit var deliveriesViewModel: DeliveriesViewModel

    private var currentDate: String = ""
    private var calenderDate: String = ""
    private var calenderSelectedDate: String = ""
    private lateinit var orderAdapter: OrderAdapter
    private var _binding: FragmentDeliveriesBinding? = null
    private val binding get() = _binding!!
    private val calendar = Calendar.getInstance()
    private val year: Int = calendar.get(Calendar.YEAR)
    private var month: Int = calendar.get(Calendar.MONTH)
    private val day: Int = calendar.get(Calendar.DAY_OF_MONTH)
    private var isCheck: String? = null
    private var orderList: List<OrdersInfo> = arrayListOf()
    private var searchText: String = ""
    private var orderListFilter: List<OrdersInfo> = arrayListOf()
    private var isLastSelected: String? = Constants.ACTIVE
    private var isOrderDetailsScreenOpen: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        deliveriesViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeliveriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewModel()
        listenToViewEvent()
        deliveriesViewModel.loadDeliverOrderData(calenderSelectedDate, getString(R.string.delivery), isCheck)
    }

    private fun listenToViewModel() {
        deliveriesViewModel.deliveriesState.subscribeAndObserveOnMainThread {
            when (it) {
                is DeliveriesViewState.OrderInfoSate -> {
                    orderList = it.orderInfo.filter { it.orderType?.isDelivery == true }
                    orderList.forEach {
                        it.status = it.status?.sortedBy { it.id }
                    }
                    val filterList = orderList.filter {
                        it.status?.lastOrNull()?.orderStatus.toString().toLowerCase() != resources.getString(R.string.completed)
                            .toLowerCase() && it.status?.lastOrNull()?.orderStatus.toString().toLowerCase() != resources.getString(R.string.cancelled)
                            .toLowerCase() && it.status?.lastOrNull()?.orderStatus.toString().toLowerCase() != resources.getString(R.string.delivered)
                            .toLowerCase() && it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.refunded)
                            .toLowerCase() && it.status?.lastOrNull()?.orderStatus.toString().toLowerCase() != resources.getString(R.string.new_text)
                            .toLowerCase()
                    }
                    RxBus.publish(RxEvent.EventDeliveryCountListen(filterList.size))
                    setOrderData(it.orderInfo.filter { it.orderType?.isDelivery == true })
                }
                is DeliveriesViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is DeliveriesViewState.LoadingState -> {
                    if (orderList.isEmpty()) {
                        binding.progressBar.isVisible = it.isLoading
                    }
                }
                is DeliveriesViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
            }
        }.autoDispose()
    }

    private fun setOrderData(listOfOderInfo: List<OrdersInfo>?) {
        if (!listOfOderInfo.isNullOrEmpty()) {
            listOfOderInfo.forEach {
                it.status = it.status?.sortedBy { it.id }
            }
            binding.emptyMessageAppCompatTextView.isVisible = false
            orderListFilter = when (isLastSelected) {
                Constants.NEW -> {
                    listOfOderInfo.filter { it.status?.lastOrNull()?.orderStatus.toString() == resources.getString(R.string.new_text).toLowerCase() }
                }
                Constants.ACTIVE -> {
                    listOfOderInfo.filter {
                        it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.completed)
                            .toLowerCase() && it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.cancelled)
                            .toLowerCase() && it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.delivered)
                            .toLowerCase() && it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.refunded)
                            .toLowerCase() && it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.new_text).toLowerCase()
                    }
                }
                Constants.PAST -> {
                    listOfOderInfo.filter {
                        it.status?.lastOrNull()?.orderStatus.toString() == resources.getString(R.string.completed)
                            .toLowerCase() || it.status?.lastOrNull()?.orderStatus.toString() == resources.getString(R.string.cancelled)
                            .toLowerCase() || it.status?.lastOrNull()?.orderStatus.toString() == resources.getString(R.string.refunded)
                            .toLowerCase() || it.status?.lastOrNull()?.orderStatus.toString() == resources.getString(R.string.delivered).toLowerCase()
                    }
                }
                else -> {
                    listOfOderInfo.filter {
                        it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.completed)
                            .toLowerCase() && it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.cancelled)
                            .toLowerCase() && it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.delivered)
                            .toLowerCase() && it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.refunded)
                            .toLowerCase() && it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.new_text).toLowerCase()
                    }
                }
            }
            if (orderListFilter.isNotEmpty()) {
                if (searchText.isNotEmpty()) {
                    var list = orderListFilter
                    list = list.filter {
                        it.id.toString().contains(searchText) || it.customerFullName()
                            .contains(searchText, ignoreCase = true) || it.guest?.firstOrNull()?.guestEmail?.contains(
                            searchText, ignoreCase = true
                        ) == true || it.guest?.firstOrNull()?.guestPhone?.contains(
                            searchText, ignoreCase = true
                        ) == true || it.user?.userEmail?.contains(searchText, ignoreCase = true) == true || it.user?.userPhone?.contains(
                            searchText, ignoreCase = true
                        ) == true
                    }
                    if (list.isEmpty()) {
                        binding.emptyMessageAppCompatTextView.isVisible = true
                        binding.emptyMessageAppCompatTextView.text = resources.getText(R.string.search_not_founded)
                    } else {
                        binding.emptyMessageAppCompatTextView.isVisible = false
                        orderAdapter.listOfOrder = list
                    }

                } else {
                    binding.emptyMessageAppCompatTextView.isVisible = false
                    orderAdapter.listOfOrder = orderListFilter
                }
            } else {
                setEmptyMessageAndVisibility()
                orderAdapter.listOfOrder = null
            }
        } else {
            setEmptyMessageAndVisibility();
            orderAdapter.listOfOrder = null
        }
    }

    private fun setEmptyMessageAndVisibility() {
        binding.emptyMessageAppCompatTextView.isVisible = true
        binding.emptyMessageAppCompatTextView.text = getText(R.string.no_deliveries_for_today)
    }

    private fun listenToViewEvent() {
        initAdapter()
        month++
        currentDate = "$month/$day/$year".toDate("MM/dd/yyyy")?.formatTo("MM/dd/yyyy").toString()
        val arrayAdapter = ArrayAdapter.createFromResource(
            requireContext(), R.array.dateArray, android.R.layout.simple_spinner_dropdown_item
        )
        binding.dateTextView.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            orderListFilter = when (position) {
                0 -> {
                    isLastSelected = parent.getItemAtPosition(position).toString().lowercase()
                    if (orderList.isNotEmpty()) {
                        orderList.filter {
                            it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.completed)
                                .toLowerCase() && it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.cancelled)
                                .toLowerCase() && it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.delivered)
                                .toLowerCase() && it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.refunded)
                                .toLowerCase() && it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.new_text)
                                .toLowerCase()
                        }
                    } else {
                        arrayListOf()
                    }
                }
                1 -> {
                    isLastSelected = parent.getItemAtPosition(position).toString().lowercase()
                    if (orderList.isNotEmpty()) {
                        orderList.filter { it.status?.lastOrNull()?.orderStatus.toString() == resources.getString(R.string.new_text).toLowerCase() }
                    } else {
                        arrayListOf()
                    }
                }
                2 -> {
                    isLastSelected = parent.getItemAtPosition(position).toString().lowercase()
                    if (orderList.isNotEmpty()) {
                        orderList.filter {
                            it.status?.lastOrNull()?.orderStatus.toString() == resources.getString(R.string.completed)
                                .toLowerCase() || it.status?.lastOrNull()?.orderStatus.toString() == resources.getString(R.string.cancelled)
                                .toLowerCase() || it.status?.lastOrNull()?.orderStatus.toString() == resources.getString(R.string.delivered)
                                .toLowerCase() || it.status?.lastOrNull()?.orderStatus.toString() == resources.getString(R.string.refunded)
                                .toLowerCase()
                        }
                    } else {
                        arrayListOf()
                    }
                }
                else -> {
                    isLastSelected = Constants.ACTIVE
                    orderList.filter {
                        it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.completed)
                            .toLowerCase() && it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.cancelled)
                            .toLowerCase() && it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.delivered)
                            .toLowerCase() && it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.refunded)
                            .toLowerCase() && it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.new_text).toLowerCase()
                    }
                }
            }
            if (orderListFilter.isNotEmpty()) {
                if (searchText.isNotEmpty()) {
                    var list = orderListFilter
                    list = list.filter {
                        it.id.toString().contains(searchText) || it.customerFullName()
                            .contains(searchText, ignoreCase = true) || it.guest?.firstOrNull()?.guestEmail?.contains(
                            searchText, ignoreCase = true
                        ) == true || it.guest?.firstOrNull()?.guestPhone?.contains(
                            searchText, ignoreCase = true
                        ) == true || it.user?.userEmail?.contains(searchText, ignoreCase = true) == true || it.user?.userPhone?.contains(
                            searchText, ignoreCase = true
                        ) == true
                    }
                    if (list.isEmpty()) {
                        binding.emptyMessageAppCompatTextView.isVisible = true
                        binding.emptyMessageAppCompatTextView.text = resources.getText(R.string.search_not_founded)
                    } else {
                        binding.emptyMessageAppCompatTextView.isVisible = false
                        orderAdapter.listOfOrder = list
                    }
                } else {
                    binding.emptyMessageAppCompatTextView.isVisible = false
                    orderAdapter.listOfOrder = orderListFilter
                }
            } else {
                setEmptyMessageAndVisibility()
                orderAdapter.listOfOrder = null
            }
        }
        binding.dateCardView.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            binding.dateTextView.setAdapter(arrayAdapter)
            binding.dateTextView.showDropDown()
        }.autoDispose()
        binding.dateTextView.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            binding.dateTextView.setAdapter(arrayAdapter)
            binding.dateTextView.showDropDown()
        }.autoDispose()
        binding.allCheckBox.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            binding.allCheckBox.isChecked = true
            binding.newCheckBox.isChecked = false
            binding.receivedCheckBox.isChecked = false
            binding.assignedCheckBox.isChecked = false
            binding.deliveredCheckBox.isChecked = false
            binding.dispatchedCheckBox.isChecked = false
            onCheckboxClicked()
        }.autoDispose()
        binding.newCheckBox.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            binding.allCheckBox.isChecked = false
            binding.newCheckBox.isChecked = true
            binding.receivedCheckBox.isChecked = false
            binding.assignedCheckBox.isChecked = false
            binding.deliveredCheckBox.isChecked = false
            binding.dispatchedCheckBox.isChecked = false
            onCheckboxClicked()
        }.autoDispose()
        binding.receivedCheckBox.throttleClicks().subscribeAndObserveOnMainThread {
            binding.allCheckBox.isChecked = false
            binding.newCheckBox.isChecked = false
            binding.receivedCheckBox.isChecked = true
            binding.assignedCheckBox.isChecked = false
            binding.deliveredCheckBox.isChecked = false
            binding.dispatchedCheckBox.isChecked = false
            onCheckboxClicked()
        }.autoDispose()
        binding.assignedCheckBox.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            binding.allCheckBox.isChecked = false
            binding.newCheckBox.isChecked = false
            binding.receivedCheckBox.isChecked = false
            binding.dispatchedCheckBox.isChecked = false
            binding.deliveredCheckBox.isChecked = false
            binding.assignedCheckBox.isChecked = true
            onCheckboxClicked()
        }.autoDispose()
        binding.dispatchedCheckBox.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            binding.allCheckBox.isChecked = false
            binding.newCheckBox.isChecked = false
            binding.receivedCheckBox.isChecked = false
            binding.assignedCheckBox.isChecked = false
            binding.deliveredCheckBox.isChecked = false
            binding.dispatchedCheckBox.isChecked = true
            onCheckboxClicked()
        }.autoDispose()
        binding.deliveredCheckBox.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            binding.allCheckBox.isChecked = false
            binding.newCheckBox.isChecked = false
            binding.receivedCheckBox.isChecked = false
            binding.assignedCheckBox.isChecked = false
            binding.dispatchedCheckBox.isChecked = false
            binding.deliveredCheckBox.isChecked = true
            onCheckboxClicked()
        }.autoDispose()

        RxBus.listen(RxEvent.SearchOrderFilter::class.java).subscribeAndObserveOnMainThread { item ->
            if (isVisible) {
                searchText = item.searchText
                if (item.searchText.isNotBlank()) {
                    var list = orderList
                    list = list.filter {
                        it.id.toString().contains(item.searchText) || it.customerFullName()
                            .contains(item.searchText, ignoreCase = true) || it.guest?.firstOrNull()?.guestEmail?.contains(
                            item.searchText, ignoreCase = true
                        ) == true || it.guest?.firstOrNull()?.guestPhone?.contains(
                            item.searchText, ignoreCase = true
                        ) == true || it.user?.userEmail?.contains(
                            item.searchText, ignoreCase = true
                        ) == true || it.user?.userPhone?.contains(item.searchText, ignoreCase = true) == true
                    }
                    orderAdapter.listOfOrder = list
                } else {
                    orderAdapter.listOfOrder = orderList
                }
            }
        }.autoDispose()
    }

    private fun onCheckboxClicked() {
        isCheck = when {
            binding.allCheckBox.isChecked -> {
                null
            }
            binding.newCheckBox.isChecked -> {
                resources.getString(R.string.new_text)
            }
            binding.receivedCheckBox.isChecked -> {
                resources.getString(R.string.received)
            }
            binding.assignedCheckBox.isChecked -> {
                resources.getString(R.string.assigned)
            }
            binding.dispatchedCheckBox.isChecked -> {
                resources.getString(R.string.dispatched)
            }
            binding.deliveredCheckBox.isChecked -> {
                resources.getString(R.string.delivered)
            }
            else -> {
                null
            }
        }
        deliveriesViewModel.loadDeliverOrderData(calenderSelectedDate, getString(R.string.delivery), isCheck?.toLowerCase())
    }

    private fun initAdapter() {
        orderAdapter = OrderAdapter(requireContext()).apply {
            orderActionState.subscribeAndObserveOnMainThread {
                isOrderDetailsScreenOpen = true
                requireActivity().hideKeyboard()
                val trans: FragmentTransaction = parentFragmentManager.beginTransaction()
                trans.replace(R.id.deliveriesFrameLayout, DeliveriesOrderDetailsFragment.newInstance(it.id))
                trans.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                trans.addToBackStack("CLOSE_DELIVERY_FRAGMENT")
                trans.commit()
                val orderList = orderAdapter.listOfOrder
                orderList?.find { it.isSelected }.apply { it.isSelected = false }
                orderAdapter.listOfOrder = orderList
            }.autoDispose()
        }
        binding.rvOrderView.apply {
            adapter = orderAdapter
        }
        orderAdapter.headerInfo = SectionInfo(
            getString(R.string.order_id),
            getString(R.string.guest),
            getString(R.string.total),
            getString(R.string.order_type),
            getString(R.string.status),
            getString(R.string.promised_time_text),
            getString(R.string.order_Placed)
        )
    }

    private fun openCalender() {
        val datePickerDialog = DatePickerDialog(requireContext(), { _, year, monthOfYear, dayOfMonth ->
            val calenderMonth = monthOfYear + 1
            calenderSelectedDate = "$year-$calenderMonth-$dayOfMonth".toDate("yyyy-MM-dd")?.formatTo("yyyy-MM-dd").toString()
            calenderDate = "$calenderMonth/$dayOfMonth/$year".toDate("MM/dd/yyyy")?.formatTo("MM/dd/yyyy").toString()
            deliveriesViewModel.loadDeliverOrderData(calenderSelectedDate, getString(R.string.delivery), isCheck?.toLowerCase())
//            binding.dateTextView.text = if (calenderDate == currentDate) {
//                resources.getText(R.string.today)
//            } else {
//                calenderDate
//            }
        }, year, month, day)
        datePickerDialog.show()
        val maximumDate = Calendar.getInstance()
        maximumDate.set(Calendar.DAY_OF_MONTH, day)
        maximumDate.set(Calendar.MONTH, month - 1)
        maximumDate.set(Calendar.YEAR, year)
        datePickerDialog.datePicker.maxDate = maximumDate.timeInMillis
    }

    override fun onResume() {
        super.onResume()
        deliveriesViewModel.loadDeliverOrderData(calenderSelectedDate, getString(R.string.delivery), isCheck)
        RxBus.listen(RxEvent.VisibleDeliveryFragment::class.java).subscribeAndObserveOnMainThread {
            isOrderDetailsScreenOpen = false
        }.autoDispose()
        if (isOrderDetailsScreenOpen) RxBus.publish(RxEvent.HideShowEditTextMainActivity(false)) else RxBus.publish(
            RxEvent.HideShowEditTextMainActivity(
                true
            )
        )
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        deliveriesViewModel.closeObserver()
    }

}