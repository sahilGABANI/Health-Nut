package com.hotbox.terminal.ui.main.order

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hotbox.terminal.R
import com.hotbox.terminal.api.order.model.OrdersInfo
import com.hotbox.terminal.api.order.model.SectionInfo
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseFragment
import com.hotbox.terminal.base.RxBus
import com.hotbox.terminal.base.RxEvent
import com.hotbox.terminal.base.ViewModelFactory
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.FragmentOrdersBinding
import com.hotbox.terminal.helper.formatTo
import com.hotbox.terminal.helper.toDate
import com.hotbox.terminal.ui.main.deliveries.DeliveriesOrderDetailsFragment
import com.hotbox.terminal.ui.main.order.view.OrderAdapter
import com.hotbox.terminal.ui.main.order.viewmodel.OrderViewModel
import com.hotbox.terminal.ui.main.order.viewmodel.OrderViewState
import com.hotbox.terminal.ui.main.orderdetail.OrderDetailsFragment
import com.hotbox.terminal.utils.Constants.ACTIVE
import com.hotbox.terminal.utils.Constants.NEW
import com.hotbox.terminal.utils.Constants.PAST
import com.hotbox.terminal.utils.UserInteractionInterceptor
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class OrdersFragment : BaseFragment() {

    companion object {
        @JvmStatic
        fun newInstance() = OrdersFragment()
    }

    private var orderList: List<OrdersInfo> = arrayListOf()
    private var orderListFilter: List<OrdersInfo> = arrayListOf()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<OrderViewModel>
    private lateinit var orderViewModel: OrderViewModel

    private lateinit var orderAdapter: OrderAdapter
    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!
    private var currentDate: String? = null
    private val calendar = Calendar.getInstance()
    private val year = calendar.get(Calendar.YEAR)
    private var month: Int = calendar.get(Calendar.MONTH)
    private val day = calendar.get(Calendar.DAY_OF_MONTH)

    private var calenderSelectedDate: String = ""
    private var calenderDate: String = ""
    private var statusSelection: String? = null
    private var isCheck: String? = null
    private var size: Int? = 0
    private var isLastSelected: String? = ACTIVE
    private var searchText: String = ""
    var isOrderDetailsScreenOpen: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        orderViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewModel() {
        orderViewModel.orderState.subscribeAndObserveOnMainThread {
            when (it) {
                is OrderViewState.OrderInfoSate -> {
                    setOrderData(it.orderInfo)
                    orderList = it.orderInfo
                    it.orderInfo.forEach {
                        it.status = it.status?.sortedBy { it.id }
                    }
                    val filterList = orderList.filter {
                        it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.completed)
                            .toLowerCase() && it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.cancelled)
                            .toLowerCase() && it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.delivered)
                            .toLowerCase() && it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.refunded)
                            .toLowerCase() && it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.new_text).toLowerCase()
                    }
                    RxBus.publish(RxEvent.EventOrderCountListen(filterList.size))
                }
                is OrderViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is OrderViewState.LoadingState -> {
                    if(orderList.isEmpty()) {
                        binding.progressBar.isVisible = it.isLoading
                    }
                }
                is OrderViewState.SuccessMessage -> {
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
                NEW -> {
                    listOfOderInfo.filter { it.status?.lastOrNull()?.orderStatus.toString() == resources.getString(R.string.new_text).toLowerCase() }
                }
                ACTIVE -> {
                    listOfOderInfo.filter {
                        it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.completed)
                            .toLowerCase() && it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.cancelled)
                            .toLowerCase() && it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.delivered)
                            .toLowerCase() && it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.refunded)
                            .toLowerCase() && it.status?.lastOrNull()?.orderStatus.toString() != resources.getString(R.string.new_text).toLowerCase()
                    }
                }
                PAST -> {
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
                emptyMessageVisibility()
                orderAdapter.listOfOrder = null
            }
        } else {
            emptyMessageVisibility()
            orderAdapter.listOfOrder = null
        }
    }

    @SuppressLint("SetTextI18n")
    private fun emptyMessageVisibility() {
        binding.emptyMessageAppCompatTextView.isVisible = true
        binding.emptyMessageAppCompatTextView.text = getText(R.string.no_order_for_today)

    }

    private fun listenToViewEvent() {
        month++
        initAdapter()

        binding.allCheckBox.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            binding.allCheckBox.isChecked = true
            binding.pickupCheckBox.isChecked = false
            binding.deliveryCheckBox.isChecked = false
            binding.inStoreCheckBox.isChecked = false
            onCheckboxClicked()
        }.autoDispose()
        binding.pickupCheckBox.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            binding.allCheckBox.isChecked = false
            binding.pickupCheckBox.isChecked = true
            binding.deliveryCheckBox.isChecked = false
            binding.inStoreCheckBox.isChecked = false
            onCheckboxClicked()
        }.autoDispose()
        binding.deliveryCheckBox.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            binding.allCheckBox.isChecked = false
            binding.pickupCheckBox.isChecked = false
            binding.deliveryCheckBox.isChecked = true
            binding.inStoreCheckBox.isChecked = false
            onCheckboxClicked()
        }.autoDispose()
        binding.inStoreCheckBox.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            binding.allCheckBox.isChecked = false
            binding.pickupCheckBox.isChecked = false
            binding.deliveryCheckBox.isChecked = false
            binding.inStoreCheckBox.isChecked = true
            onCheckboxClicked()
        }.autoDispose()
        val arrayAdapter = ArrayAdapter.createFromResource(
            requireContext(), R.array.statusArray, android.R.layout.simple_spinner_dropdown_item
        )

        val arrayAdapterDate = ArrayAdapter.createFromResource(
            requireContext(), R.array.dateArray, android.R.layout.simple_spinner_dropdown_item
        )
        binding.dateTextView.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            binding.dateTextView.setAdapter(arrayAdapterDate)
            binding.dateTextView.showDropDown()
        }.autoDispose()
        binding.dateTextView.onItemClickListener = OnItemClickListener { parent, _, position, _ ->
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
                    isLastSelected = ACTIVE
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
                emptyMessageVisibility()
                orderAdapter.listOfOrder = null
            }
        }

        binding.llSpinner.throttleClicks().subscribeAndObserveOnMainThread {
            binding.autoCompleteStatus.setAdapter(arrayAdapter)
            requireActivity().hideKeyboard()
            binding.autoCompleteStatus.showDropDown()
        }.autoDispose()
        binding.autoCompleteStatus.throttleClicks().subscribeAndObserveOnMainThread {
            binding.autoCompleteStatus.setAdapter(arrayAdapter)
            requireActivity().hideKeyboard()
            binding.autoCompleteStatus.showDropDown()
        }.autoDispose()
        binding.autoCompleteStatus.onItemClickListener = OnItemClickListener { parent, _, position, _ ->
            statusSelection = when (position) {
                0 -> {
                    null
                }
                2 -> {
                    resources.getString(R.string.making)
                }
                else -> {
                    parent.getItemAtPosition(position).toString().lowercase()
                }
            }
            orderViewModel.loadOrderData(calenderSelectedDate, isCheck, statusSelection)
        }
        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            binding.swipeRefreshLayout.isRefreshing = true
            binding.relativeLayout.isVisible = false
            Observable.timer(2000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                onResume()
                binding.swipeRefreshLayout.isRefreshing = false
                binding.relativeLayout.isVisible = true
            }.autoDispose()
        }.autoDispose()

        RxBus.listen(RxEvent.SearchOrderFilter::class.java).subscribeAndObserveOnMainThread { item ->
            if (isVisible) {
                searchText = item.searchText
                if (item.searchText.isNotBlank()) {
                    var list = orderListFilter
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
                    orderAdapter.listOfOrder = orderListFilter
                }
            }
            //it.customerFullName().contains(item.searchText)
        }.autoDispose()
    }

    private fun initAdapter() {
        orderAdapter = OrderAdapter(requireContext()).apply {
            orderActionState.subscribeAndObserveOnMainThread {
                isOrderDetailsScreenOpen = true
                requireActivity().hideKeyboard()
                if (it.orderType?.isDelivery == true) {
                    val trans: FragmentTransaction = parentFragmentManager.beginTransaction()
                    trans.replace(R.id.frameLayout, DeliveriesOrderDetailsFragment.newInstance(it.id, true))
                    trans.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    trans.addToBackStack("CLOSE_DELIVERY")
                    trans.commit()
                    val orderList = orderAdapter.listOfOrder
                    orderList?.find { it.isSelected }.apply { it.isSelected = false }
                    orderAdapter.listOfOrder = orderList
                } else {
                    val trans: FragmentTransaction = requireFragmentManager().beginTransaction()
                    trans.replace(R.id.frameLayout, OrderDetailsFragment.newInstance(it.id))
                    trans.addToBackStack(null)
                    trans.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    trans.commit()
                    val orderList = orderAdapter.listOfOrder
                    orderList?.find { it.isSelected }.apply { it.isSelected = false }
                    orderAdapter.listOfOrder = orderList
                }
            }.autoDispose()
        }
        binding.rvOrderView.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
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

    private fun onCheckboxClicked() {
        isCheck = when {
            binding.allCheckBox.isChecked -> {
                null
            }
            binding.pickupCheckBox.isChecked -> {
                resources.getString(R.string.pickup)
            }
            binding.deliveryCheckBox.isChecked -> {
                resources.getString(R.string.delivery)
            }
            binding.inStoreCheckBox.isChecked -> {
                resources.getString(R.string.in_store_text)
            }
            else -> {
                null
            }
        }
        orderViewModel.loadOrderData(calenderSelectedDate, isCheck, statusSelection)
    }

    private fun openCalender() {
        val datePickerDialog = DatePickerDialog(requireContext(), { _, year, monthOfYear, dayOfMonth ->
            val calenderMonth = monthOfYear + 1
            val calendar = Calendar.getInstance()
            val yearf = calendar.get(Calendar.YEAR)
            val month: Int = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            currentDate = "${month + 1}/$day/$yearf".toDate("MM/dd/yyyy")?.formatTo("MM/dd/yyyy")
            calenderSelectedDate = "$year-$calenderMonth-$dayOfMonth".toDate("yyyy-MM-dd")?.formatTo("yyyy-MM-dd").toString()
            calenderDate = "$calenderMonth/$dayOfMonth/$year".toDate("MM/dd/yyyy")?.formatTo("MM/dd/yyyy").toString()
            orderViewModel.loadOrderData(calenderSelectedDate, isCheck, statusSelection)
//            binding.dateTextView.text = if (calenderDate == currentDate) {
//                resources.getText(R.string.today)
//            } else {
//                calenderDate
//            }
        }, year, month - 1, day)
        datePickerDialog.show()
    }

    override fun onResume() {
        super.onResume()
        orderViewModel.loadOrderData(calenderSelectedDate, isCheck, statusSelection)
        binding.allCheckBox.isChecked = true
        binding.inStoreCheckBox.isChecked = false
        binding.deliveryCheckBox.isChecked = false
        binding.pickupCheckBox.isChecked = false
        RxBus.listen(RxEvent.VisibleOrderFragment::class.java).subscribeAndObserveOnMainThread {
            isOrderDetailsScreenOpen = false
        }.autoDispose()
        if (isOrderDetailsScreenOpen) RxBus.publish(RxEvent.HideShowEditTextMainActivity(false)) else RxBus.publish(
            RxEvent.HideShowEditTextMainActivity(
                true
            )
        )
    }

    override fun onDestroy() {
        orderViewModel.closeObserver()
        super.onDestroy()
    }
}