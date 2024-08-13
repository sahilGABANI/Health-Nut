package com.hotbox.terminal.ui.main.store

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.store.model.AssignedEmployeeInfo
import com.hotbox.terminal.api.store.model.StoreResponse
import com.hotbox.terminal.api.store.model.StoreShiftTime
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseFragment
import com.hotbox.terminal.base.RxBus
import com.hotbox.terminal.base.RxEvent
import com.hotbox.terminal.base.ViewModelFactory
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.FragmentStoreBinding
import com.hotbox.terminal.helper.formatTo
import com.hotbox.terminal.helper.formatToStoreTime
import com.hotbox.terminal.helper.getCurrentsStoreTime
import com.hotbox.terminal.helper.toDate
import com.hotbox.terminal.ui.main.store.view.AssignedEmployeesAdapter
import com.hotbox.terminal.ui.main.store.view.OrderingHoursAdapter
import com.hotbox.terminal.ui.main.store.viewmodel.StoreState
import com.hotbox.terminal.ui.main.store.viewmodel.StoreViewModel
import com.hotbox.terminal.utils.UserInteractionInterceptor
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import io.reactivex.Observable
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class StoreFragment : BaseFragment() {

    companion object {
        @JvmStatic
        fun newInstance() = StoreFragment()
    }

    private var _binding: FragmentStoreBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<StoreViewModel>
    private lateinit var storeViewModel: StoreViewModel
    private lateinit var assignedEmployeesAdapter: AssignedEmployeesAdapter
    private lateinit var orderingHoursAdapter: OrderingHoursAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        storeViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        initAdapter()
        binding.openStoreTimeLinearLayout.isSelected = true
        binding.tvOpenAndClose.isSelected = true
        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            binding.swipeRefreshLayout.isRefreshing = true
            binding.storeConstraintLayout.isVisible = false
            Observable.timer(2000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                onResume()
                binding.swipeRefreshLayout.isRefreshing = false
                binding.storeConstraintLayout.isVisible = true
            }.autoDispose()
        }
        binding.storeNameAppCompatTextView.text =
            loggedInUserCache.getLocationInfo()?.location?.locationName ?: "Manhattan Beach Health Nut"
        binding.subtractionPickupTimeMaterialCardView.throttleClicks()
            .subscribeAndObserveOnMainThread {
                if (loggedInUserCache.isAdmin()) {
                    storeViewModel.updateBufferTimeForPickUpOrDelivery(
                        isBufferTimePlush = false,
                        isPickUpBufferTime = true
                    )
                } else {
                    showToast("This feature requires elevated permissions")
                }
            }.autoDispose()
        binding.additionPickUpTimeMaterialCardView.throttleClicks()
            .subscribeAndObserveOnMainThread {
                if (loggedInUserCache.isAdmin()) {
                    storeViewModel.updateBufferTimeForPickUpOrDelivery(
                        isBufferTimePlush = true,
                        isPickUpBufferTime = true
                    )
                } else {
                    showToast("This feature requires elevated permissions")
                }
            }.autoDispose()
        binding.subtractionDeliveryTimeMaterialCardView.throttleClicks()
            .subscribeAndObserveOnMainThread {
                if (loggedInUserCache.isAdmin()) {
                    storeViewModel.updateBufferTimeForPickUpOrDelivery(
                        isBufferTimePlush = false,
                        isPickUpBufferTime = false
                    )
                } else {
                    showToast("This feature requires elevated permissions")
                }
            }.autoDispose()
        binding.additionDeliveryTimeMaterialCardView.throttleClicks()
            .subscribeAndObserveOnMainThread {
                if (loggedInUserCache.isAdmin()) {
                    storeViewModel.updateBufferTimeForPickUpOrDelivery(
                        isBufferTimePlush = true,
                        isPickUpBufferTime = false
                    )
                } else {
                    showToast("This feature requires elevated permissions")
                }
            }.autoDispose()
    }

    private fun initAdapter() {
        assignedEmployeesAdapter = AssignedEmployeesAdapter(requireContext())
        orderingHoursAdapter = OrderingHoursAdapter(requireContext())
        binding.assignedEmployeesRecyclerView.apply {
            adapter = assignedEmployeesAdapter
        }
        binding.orderingHoursRecyclerView.apply {
            adapter = orderingHoursAdapter
        }
    }

    private fun listenToViewModel() {
        storeViewModel.storeState.subscribeAndObserveOnMainThread {
            when (it) {
                is StoreState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is StoreState.LoadingState -> {
                }
                is StoreState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is StoreState.StoreResponses -> {
                    binding.storeLocationAppCompatTextView.text =
                        it.storeResponse.getSafeAddressName()
                    val employeeInfo = it.storeResponse.employee?.filter { item -> item.role?.id == 2 }
                    binding.storeContactNumberAppCompatTextView.text =
                        it.storeResponse.getSafePhoneNumber()
                    binding.storeEmailIdAppCompatTextView.text =
                        employeeInfo?.firstOrNull()?.user?.userEmail ?: ""
                    binding.storeHeadPersonNameAppCompatTextView.text =
                        employeeInfo?.firstOrNull()?.getSafeFullNameWithRoleName()
                    setAssignedEmployeesData(it.storeResponse.employee)
                    setStoreOpenAndClose(it.storeResponse)
                }
                is StoreState.BufferResponses -> {
                    binding.pickupMinNumberAppCompatTextView.text =
                        it.bufferResponse.getSafeTakeOutBufferTime().toString()
                    binding.deliveryMinNumberAppCompatTextView.text =
                        it.bufferResponse.getSafeDeliveryBufferTime().toString()
                }
                is StoreState.LoadStoreShiftTime -> {
                    setTimeHours(it.listOfShiftTime)
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun setStoreOpenAndClose(storeResponse: StoreResponse) {
        val timeZone = storeResponse.locationLocationTimezone ?: "America/Los_Angeles"
        binding.liveTimeTextClock.timeZone = loggedInUserCache.getStoreResponse()?.locationLocationTimezone ?: "America/Los_Angeles"
        val c = Calendar.getInstance()
        val dayOfWeek = c[Calendar.DAY_OF_WEEK]
        val currentTime = getCurrentsStoreTime(timeZone).formatToStoreTime("HH:mm:ss")
        when (dayOfWeek - 1) {
            0 -> {
                val isSundayClosed = storeResponse.isSundayClosed == 1
                binding.openStoreTimeLinearLayout.isSelected = false
                binding.tvOpenAndClose.isSelected = false
                binding.tvOpenAndClose.text = resources.getText(if (isSundayClosed) R.string.close else R.string.open)
                if (!isSundayClosed) {
                    val openTime = storeResponse.sundayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                    val closeTime = storeResponse.sundayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                    if (openTime != null && closeTime != null) {
                        val isOpen = openTime < currentTime && closeTime > currentTime
                        binding.openStoreTimeLinearLayout.isSelected = isOpen
                        binding.tvOpenAndClose.isSelected = isOpen
                        binding.tvOpenAndClose.text = resources.getText(if (isOpen) R.string.open else R.string.close)
                    }
                }
            }
            1 -> {
                val isMondayClosed = storeResponse.isMondayClosed == 1
                binding.openStoreTimeLinearLayout.isSelected = false
                binding.tvOpenAndClose.isSelected = false
                binding.tvOpenAndClose.text = resources.getText(if (isMondayClosed) R.string.close else R.string.open)

                if (!isMondayClosed) {
                    val openTime = storeResponse.mondayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                    val closeTime = storeResponse.mondayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")

                    if (openTime != null && closeTime != null) {
                        val isOpen = openTime < currentTime && closeTime > currentTime

                        binding.openStoreTimeLinearLayout.isSelected = isOpen
                        binding.tvOpenAndClose.isSelected = isOpen
                        binding.tvOpenAndClose.text = resources.getText(if (isOpen) R.string.open else R.string.close)
                    }
                }
            }
            2 -> {
                val isTuesdayClosed = storeResponse.isTuesdayClosed == 1
                binding.openStoreTimeLinearLayout.isSelected = false
                binding.tvOpenAndClose.isSelected = false
                binding.tvOpenAndClose.text = resources.getText(if (isTuesdayClosed) R.string.close else R.string.open)

                if (!isTuesdayClosed) {
                    val openTime = storeResponse.tuesdayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                    val closeTime = storeResponse.tuesdayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")

                    if (openTime != null && closeTime != null) {
                        val isOpen = openTime < currentTime && closeTime > currentTime

                        binding.openStoreTimeLinearLayout.isSelected = isOpen
                        binding.tvOpenAndClose.isSelected = isOpen
                        binding.tvOpenAndClose.text = resources.getText(if (isOpen) R.string.open else R.string.close)
                    }
                }
            }
            3 -> {
                val isWednesdayClosed = storeResponse.isWednesdayClosed == 1
                binding.openStoreTimeLinearLayout.isSelected = false
                binding.tvOpenAndClose.isSelected = false
                binding.tvOpenAndClose.text = resources.getText(if (isWednesdayClosed) R.string.close else R.string.open)

                if (!isWednesdayClosed) {
                    val openTime = storeResponse.wednesdayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                    val closeTime = storeResponse.wednesdayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")

                    if (openTime != null && closeTime != null) {
                        val isOpen = openTime < currentTime && closeTime > currentTime

                        binding.openStoreTimeLinearLayout.isSelected = isOpen
                        binding.tvOpenAndClose.isSelected = isOpen
                        binding.tvOpenAndClose.text = resources.getText(if (isOpen) R.string.open else R.string.close)
                    }
                }
            }
            4 -> {
                val isThursdayClosed = storeResponse.isThursdayClosed == 1
                binding.openStoreTimeLinearLayout.isSelected = false
                binding.tvOpenAndClose.isSelected = false
                binding.tvOpenAndClose.text = resources.getText(if (isThursdayClosed) R.string.close else R.string.open)

                if (!isThursdayClosed) {
                    val openTime = storeResponse.thursdayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                    val closeTime = storeResponse.thursdayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")

                    if (openTime != null && closeTime != null) {
                        val isOpen = openTime < currentTime && closeTime > currentTime

                        binding.openStoreTimeLinearLayout.isSelected = isOpen
                        binding.tvOpenAndClose.isSelected = isOpen
                        binding.tvOpenAndClose.text = resources.getText(if (isOpen) R.string.open else R.string.close)
                    }
                }
            }
            5 -> {
                val isFridayClosed = storeResponse.isFridayClosed == 1
                binding.openStoreTimeLinearLayout.isSelected = false
                binding.tvOpenAndClose.isSelected = false
                binding.tvOpenAndClose.text = resources.getText(if (isFridayClosed) R.string.close else R.string.open)

                if (!isFridayClosed) {
                    val openTime = storeResponse.fridayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                    val closeTime = storeResponse.fridayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")

                    if (openTime != null && closeTime != null) {
                        val isOpen = openTime < currentTime && closeTime > currentTime

                        binding.openStoreTimeLinearLayout.isSelected = isOpen
                        binding.tvOpenAndClose.isSelected = isOpen
                        binding.tvOpenAndClose.text = resources.getText(if (isOpen) R.string.open else R.string.close)
                    }
                }
            }
            6 -> {
                val isSaturdayClosed = storeResponse.isSaturdayClosed == 1
                binding.openStoreTimeLinearLayout.isSelected = false
                binding.tvOpenAndClose.isSelected = false
                binding.tvOpenAndClose.text = resources.getText(if (isSaturdayClosed) R.string.close else R.string.open)

                if (!isSaturdayClosed) {
                    val openTime = storeResponse.saturdayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                    val closeTime = storeResponse.saturdayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")

                    if (openTime != null && closeTime != null) {
                        val isOpen = openTime < currentTime && closeTime > currentTime

                        binding.openStoreTimeLinearLayout.isSelected = isOpen
                        binding.tvOpenAndClose.isSelected = isOpen
                        binding.tvOpenAndClose.text = resources.getText(if (isOpen) R.string.open else R.string.close)
                    }
                }
            }
        }
    }


    private fun setTimeHours(storeShiftTime: List<StoreShiftTime>) {
        orderingHoursAdapter.listOfOrderingHours = storeShiftTime
    }

    private fun setAssignedEmployeesData(assignedEmployeesDetails: List<AssignedEmployeeInfo>?) {
        val list = assignedEmployeesDetails?.sortedBy { it.role?.id }
        assignedEmployeesAdapter.listOfAssignedEmployees = list
    }

    override fun onResume() {
        super.onResume()
        RxBus.publish(RxEvent.HideShowEditTextMainActivity(false))
        storeViewModel.loadCurrentStoreResponse()
        storeViewModel.loadBufferTIme()
    }

    override fun onStart() {
        super.onStart()

    }
}