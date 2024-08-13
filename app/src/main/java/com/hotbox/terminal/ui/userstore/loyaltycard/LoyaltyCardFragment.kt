package com.hotbox.terminal.ui.userstore.loyaltycard

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.checkout.model.LoyaltyWithPhoneResponse
import com.hotbox.terminal.api.checkout.model.QRScanResponse
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseDialogFragment
import com.hotbox.terminal.base.ViewModelFactory
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.FragmentLoyaltyCardBinding
import com.hotbox.terminal.ui.userstore.AddToCartDialogFragment
import com.hotbox.terminal.ui.userstore.checkout.PhoneLoyaltyFragment
import com.hotbox.terminal.ui.userstore.checkout.QrScannerFragment
import com.hotbox.terminal.ui.userstore.viewmodel.UserStoreState
import com.hotbox.terminal.ui.userstore.viewmodel.UserStoreViewModel
import com.hotbox.terminal.utils.Constants
import com.hotbox.terminal.utils.UserInteractionInterceptor
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class LoyaltyCardFragment : BaseDialogFragment() {

    private var _binding: FragmentLoyaltyCardBinding? = null
    private val binding get() = _binding!!
    private lateinit var qrScanResponse: LoyaltyWithPhoneResponse
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<UserStoreViewModel>
    private lateinit var checkOutViewModel: UserStoreViewModel

    private val qrScanSubject: PublishSubject<String> = PublishSubject.create()
    val qrScan: Observable<String> = qrScanSubject.hide()

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        checkOutViewModel = getViewModelFromFactory(viewModelFactory)
        setStyle(STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoyaltyCardBinding.inflate(inflater, container, false)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.decorView?.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        loggedInUserCache.setLoyaltyQrResponse(QRScanResponse(null, null, null, null, null, null))
        binding.joinLoyaltyProgramLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
            val joinLoyaltyProgramDialog = JoinLoyaltyProgramDialog()
            joinLoyaltyProgramDialog.show(parentFragmentManager, LoyaltyCardFragment::class.java.name)
            dismiss()
        }.autoDispose()
        binding.tvSkip.throttleClicks().subscribeAndObserveOnMainThread {
            loggedInUserCache.setLoyaltyQrResponse(QRScanResponse(null, null, null, null, null, null))
            qrScanSubject.onNext("")
        }.autoDispose()
        binding.btnWithPhone.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            val phoneLoyaltyFragment = PhoneLoyaltyFragment().apply {
                checkWithPhoneClick.subscribeAndObserveOnMainThread {
                    checkOutViewModel.getPhoneLoyaltyData(it)
                }.autoDispose()
            }
            phoneLoyaltyFragment.show(parentFragmentManager, LoyaltyCardFragment::class.java.name)
        }.autoDispose()
        binding.scanCardButton.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            checkPermission()
        }.autoDispose()
        binding.closeButtonMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()
    }

    private fun checkPermission() {
        XXPermissions.with(this).permission(Permission.CAMERA).request(object : OnPermissionCallback {
            override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                if (all) {
                    val qrScannerFragment = QrScannerFragment().apply {
                        qrData.subscribeAndObserveOnMainThread {
                            dismiss()
                            checkOutViewModel.getQRData(it)
                            requireActivity().hideKeyboard()
                        }.autoDispose()
                    }
                    qrScannerFragment.show(requireFragmentManager(), "")
                    return
                }
            }
            override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                if (never) {
                    XXPermissions.startPermissionActivity(
                        requireActivity(), permissions
                    )
                } else {
                    showToast("Please enable permission")
                }
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun listenToViewModel() {
        checkOutViewModel.userStoreState.subscribeAndObserveOnMainThread {
            when (it) {
                is UserStoreState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is UserStoreState.LoadingState -> {

                }
                is UserStoreState.UserCreditPoint -> {
                    loggedInUserCache.setLoyaltyQrResponse(
                        QRScanResponse(
                            qrScanResponse.userPhone, it.data.fullName(), qrScanResponse.userId, qrScanResponse.userEmail, qrScanResponse.points
                        )
                    )
                    qrScanSubject.onNext("")
                }
                is UserStoreState.QrCodeData -> {
                    loggedInUserCache.setLoyaltyQrResponse(QRScanResponse(it.data.phone, it.data.fullName, it.data.id, it.data.email, it.data.points))
                    qrScanSubject.onNext("")
                }
                is UserStoreState.PhoneLoyaltyData -> {
                    if (it.data.userId != "" && it.data.userId != null) {
                        checkOutViewModel.getUser(it.data.userId)
                        qrScanResponse = it.data
                    } else {
                        showToast("Unable to find your loyalty account")
                    }
                }
                is UserStoreState.QrCodeScanError -> {
                    showToast(it.errorType)
                }
                else -> {

                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        UserInteractionInterceptor.wrapWindowCallback(requireActivity().window, activity)
    }
}