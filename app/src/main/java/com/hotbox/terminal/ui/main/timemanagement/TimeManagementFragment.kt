package com.hotbox.terminal.ui.main.timemanagement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import by.dzmitry_lakisau.month_year_picker_dialog.MonthYearPickerDialog
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.clockinout.model.ClockType
import com.hotbox.terminal.api.clockinout.model.TimeResponse
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseFragment
import com.hotbox.terminal.base.ViewModelFactory
import com.hotbox.terminal.base.extension.getViewModelFromFactory
import com.hotbox.terminal.base.extension.showToast
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hotbox.terminal.base.extension.throttleClicks
import com.hotbox.terminal.databinding.FragmentTimeManagementBinding
import com.hotbox.terminal.helper.CLOCK_IN_OUT_FORMAT
import com.hotbox.terminal.helper.getMonthNameFromInt
import com.hotbox.terminal.ui.login.LoginActivity
import com.hotbox.terminal.ui.main.CheckInDialogFragment
import com.hotbox.terminal.ui.main.timemanagement.view.ClockInOutAdapter
import com.hotbox.terminal.ui.main.viewmodel.ClockInOutState
import com.hotbox.terminal.ui.main.viewmodel.ClockInOutViewModel
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import io.reactivex.Observable
import java.util.*
import java.util.Calendar.MONTH
import java.util.Calendar.YEAR
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.properties.Delegates

class TimeManagementFragment : BaseFragment() {

    companion object {
        @JvmStatic
        fun newInstance() = TimeManagementFragment()
    }

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ClockInOutViewModel>
    private lateinit var clockInOutViewModel: ClockInOutViewModel
    private lateinit var clockInOutAdapter: ClockInOutAdapter
    private var month: Int = Calendar.getInstance().get(MONTH)
    private var year: Int = Calendar.getInstance().get(YEAR)
    private var _binding: FragmentTimeManagementBinding? = null
    private val binding get() = _binding!!
    private lateinit var clockType: ClockType
    private var loggedInUserId by Delegates.notNull<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        clockInOutViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimeManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        loggedInUserId = loggedInUserCache.getLoggedInUserId() ?: throw IllegalStateException("User it not login")
        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewModel() {
        clockInOutViewModel.clockInOutState.subscribeAndObserveOnMainThread {
            when (it) {
                is ClockInOutState.ErrorMessage -> {
                    showToast(it.errorMessage)
                    clockInOutAdapter.listOfClockInOut = null
                }
                is ClockInOutState.LoadingState -> {
                    binding.progressBar.isVisible = it.isLoading
                }
                is ClockInOutState.SuccessMessage -> {

                }
                is ClockInOutState.LoadClockInOutResponse -> {
                    setClockInOutData(it.clockInOutDetailsInfo.listOfTimeResponse)
                }
                is ClockInOutState.CurrentTimeStatus -> {
                    if (it.currentTimeResponse.isInitClockTime()) {
                        binding.actionAppCompatTextView.text = resources.getString(R.string.clocked_out)
                        binding.clockOutMaterialButton.text = resources.getString(R.string.clock_in)
                        binding.timeTextView.text = "-"
                        return@subscribeAndObserveOnMainThread
                    }
                    clockType = it.currentTimeResponse.getClockType()
                    when (clockType) {
                        ClockType.ClockIn -> {
                            binding.actionAppCompatTextView.text =
                                resources.getString(R.string.clocked_in)
                            binding.clockOutMaterialButton.text =
                                resources.getString(R.string.clock_out)
                        }
                        ClockType.ClockOut -> {
                            binding.actionAppCompatTextView.text =
                                resources.getString(R.string.clocked_out)
                            binding.clockOutMaterialButton.text =
                                resources.getString(R.string.clock_in)
                        }
                    }
                    binding.timeTextView.text = it.currentTimeResponse.getActionFormattedTime(CLOCK_IN_OUT_FORMAT)
                }
                is ClockInOutState.StoreLocation -> {
                    binding.employeeStoreLocation.text = it.fullAddress
                }
            }
        }.autoDispose()
    }

    private fun setClockInOutData(data: List<TimeResponse>?) {
        clockInOutAdapter.listOfClockInOut = data
    }

    private fun listenToViewEvent() {
        initAdapter()
        initUI()
        binding.timeManagementSwipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.timeManagementSwipeRefreshLayout.isRefreshing = true
            binding.timeManagementConstraintLayout.isVisible = false
            Observable.timer(2000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                onResume()
                binding.timeManagementSwipeRefreshLayout.isRefreshing = false
                binding.timeManagementConstraintLayout.isVisible = true
            }.autoDispose()
        }.autoDispose()
        binding.calenderCardView.throttleClicks().subscribeAndObserveOnMainThread {
            openCalender()
        }.autoDispose()
        binding.clockOutMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            val checkInDialogFragment = CheckInDialogFragment.newInstance(ClockType.ClockOut)
            checkInDialogFragment.clockInOutSuccess.subscribeAndObserveOnMainThread {
                if (it) {
                    clockInOutViewModel.getCurrentTime(loggedInUserId)
                    clockInOutViewModel.loadClockInOutData(loggedInUserId, month+1, year)
                }
            }.autoDispose()
            checkInDialogFragment.show(childFragmentManager, LoginActivity::class.java.name)
        }.autoDispose()
    }

    private fun initUI() {
        binding.currentMonthTextView.text = month.getMonthNameFromInt()
        binding.employeeNameTextView.text = loggedInUserCache.getLoggedInUserFullName()
        binding.employeeContactNumber.text = loggedInUserCache.getLoggedInUserDetail()?.userPhone
        binding.employeeEmailId.text = loggedInUserCache.getLoggedInUserDetail()?.userEmail
//        binding.employeeStoreName.text = loggedInUserCache.getLocationInfo()?.locationName ?: ""
//        binding.employeePositionTextView.text = loggedInUserCache.getLoggedInUserRole()
    }

    private fun openCalender() {
        MonthYearPickerDialog
            .Builder(
                requireContext(),
                R.style.Style_MonthYearPickerDialog, { selectedYear, month ->
                    binding.currentMonthTextView.text = month.getMonthNameFromInt()
                    clockInOutViewModel.loadClockInOutData(loggedInUserId, month + 1, selectedYear)
                }, year, month
            )
            .setMaxMonth(month)
            .setMaxYear(Calendar.getInstance().get(YEAR))
            .build().show()
    }

    private fun initAdapter() {
        clockInOutAdapter = ClockInOutAdapter(requireContext())
        binding.rvClockInOut.apply {
            adapter = clockInOutAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        binding.currentMonthTextView.text = month.getMonthNameFromInt()
//        clockInOutViewModel.loadCurrentStoreLocation()
//        clockInOutViewModel.loadClockInOutData(loggedInUserId, month+1, year)
//        clockInOutViewModel.getCurrentTime(loggedInUserId)
    }
}