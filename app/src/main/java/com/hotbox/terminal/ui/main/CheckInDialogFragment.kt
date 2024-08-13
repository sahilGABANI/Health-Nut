package com.hotbox.terminal.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.isVisible
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.clockinout.model.ClockType
import com.hotbox.terminal.api.clockinout.model.SubmitTimeRequest
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseDialogFragment
import com.hotbox.terminal.base.ViewModelFactory
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.ClockedInDialogBinding
import com.hotbox.terminal.helper.CLOCK_IN_OUT_FORMAT
import com.hotbox.terminal.ui.main.viewmodel.ClockInOutState
import com.hotbox.terminal.ui.main.viewmodel.ClockInOutViewModel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class CheckInDialogFragment : BaseDialogFragment() {

    private var _binding: ClockedInDialogBinding? = null
    private val binding get() = _binding!!
    private lateinit var clockType: ClockType

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ClockInOutViewModel>
    private lateinit var clockInOutViewModel: ClockInOutViewModel

    private val clockInOutSuccessSubject: PublishSubject<Boolean> = PublishSubject.create()
    val clockInOutSuccess: Observable<Boolean> = clockInOutSuccessSubject.hide()

    companion object {
        const val CLOCK_TYPE = "clock_type"
        fun newInstance(clockType: ClockType): CheckInDialogFragment {
            val args = Bundle()
            args.putEnum(CLOCK_TYPE, clockType)
            val fragment = CheckInDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clockType = arguments?.getEnum(CLOCK_TYPE, ClockType.ClockIn)
            ?: throw IllegalStateException("No args provided")
        setStyle(STYLE_NORMAL, R.style.MyDialog)
        HotBoxApplication.component.inject(this)
        clockInOutViewModel = getViewModelFromFactory(viewModelFactory)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ClockedInDialogBinding.inflate(inflater, container, false)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val userId = loggedInUserCache.getLoggedInUserId()
            ?: throw IllegalStateException("User it not login")
        listenToViewModel()
        listenToViewEvent()
//        clockInOutViewModel.getCurrentTime(userId)
    }

    private fun listenToViewModel() {
        clockInOutViewModel.clockInOutState.subscribeAndObserveOnMainThread {
            when (it) {
                is ClockInOutState.LoadingState -> {
                    buttonVisibility(it.isLoading)
                }
                is ClockInOutState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is ClockInOutState.CurrentTimeStatus -> {
                    if (it.currentTimeResponse.isInitClockTime()) {
                        binding.titleAppCompatTextView.text = resources.getString(R.string.clocked_out)
                        binding.checkOutButton.text = resources.getString(R.string.clock_in)
                        binding.clockedTimeTextView.isVisible = false
                        return@subscribeAndObserveOnMainThread
                    }
                    clockType = it.currentTimeResponse.getClockType()
                    when (clockType) {
                        ClockType.ClockIn -> {
                            binding.titleAppCompatTextView.text =
                                resources.getString(R.string.clocked_in)
                            binding.checkOutButton.text = resources.getString(R.string.clock_out)
                        }
                        ClockType.ClockOut -> {
                            binding.titleAppCompatTextView.text =
                                resources.getString(R.string.clocked_out)
                            binding.checkOutButton.text = resources.getString(R.string.clock_in)
                        }
                    }
                    binding.clockedTimeTextView.text =
                        it.currentTimeResponse.getActionFormattedTime(
                            CLOCK_IN_OUT_FORMAT
                        )
                }
                is ClockInOutState.SuccessMessage -> {
                    dialog?.dismiss()
                    clockInOutSuccessSubject.onNext(true)
                }
                else -> {
                }
            }
        }.autoDispose()
    }

    private fun buttonVisibility(loading: Boolean) {
        if (loading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.checkOutButton.visibility = View.GONE
        } else {
            binding.progressBar.visibility = View.GONE
            binding.checkOutButton.visibility = View.VISIBLE
        }
    }

    private fun listenToViewEvent() {
        binding.crewUserNameTextView.text = loggedInUserCache.getLoggedInUserFullName()
//        binding.crewUserJobPost.text = loggedInUserCache.getLoggedInUserRole()
        binding.dismissAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            dialog?.dismiss()
            clockInOutSuccessSubject.onNext(false)
        }.autoDispose()
        binding.checkOutButton.throttleClicks().subscribeAndObserveOnMainThread {
            val userId = loggedInUserCache.getLoggedInUserId()
                ?: throw IllegalStateException("User it not login")
            val submitTimeRequest = when (clockType) {
                ClockType.ClockIn -> {
//                    SubmitTimeRequest(userId, ClockType.ClockOut.type)
                }
                ClockType.ClockOut -> {
//                    SubmitTimeRequest(userId, ClockType.ClockIn.type)
                }
            }
//            clockInOutViewModel.submitTime(submitTimeRequest)
        }.autoDispose()
    }
}