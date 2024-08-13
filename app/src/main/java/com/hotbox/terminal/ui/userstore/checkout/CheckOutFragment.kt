package com.hotbox.terminal.ui.userstore.checkout

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentTransaction
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.checkout.model.*
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseFragment
import com.hotbox.terminal.base.RxBus
import com.hotbox.terminal.base.RxEvent
import com.hotbox.terminal.base.ViewModelFactory
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.FragmentCheckOutBinding
import com.hotbox.terminal.helper.formatTo
import com.hotbox.terminal.helper.toDate
import com.hotbox.terminal.ui.main.deliveries.DeliveriesOrderDetailsFragment
import com.hotbox.terminal.ui.main.orderdetail.PrintReceiptDialog
import com.hotbox.terminal.ui.userstore.checkout.viewmodel.CheckOutState
import com.hotbox.terminal.ui.userstore.checkout.viewmodel.CheckOutViewModel
import com.hotbox.terminal.ui.userstore.cookies.RedeemProductFragment
import com.hotbox.terminal.ui.userstore.loyaltycard.JoinLoyaltyProgramDialog
import com.hotbox.terminal.utils.Constants
import java.util.*
import javax.inject.Inject

class CheckOutFragment : BaseFragment() {

    private var subtotal1: Int = 0
    private var giftCardId: String = ""
    private var enterGiftCardPrize: Int = 0
    private var couponCodeId: Int = 0
    private var redeemPoint: Int = 0
    private var subTotal: Double? = null
    private var orderTotal: Double = 0.0
    private var _binding: FragmentCheckOutBinding? = null
    private val binding get() = _binding!!
    private val CAMERA_PERMISSION_REQUEST_CODE = 2323

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<CheckOutViewModel>
    private lateinit var checkOutViewModel: CheckOutViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var qrScanResponse = QRScanResponse()
    private var giftCardResponse = GiftCardResponse()
    private var usersCreditPrice: Int = 0
    var qrCodeType: String = Constants.QR_CODE_TYPE_LOYALTY
    var qrUserId: String? = null

    companion object {
        @JvmStatic
        fun newInstance() = CheckOutFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        checkOutViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckOutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewModel()
        listenToViewEvent()
    }

    @SuppressLint("SetTextI18n")
    private fun listenToViewModel() {
        checkOutViewModel.checkOutState.subscribeAndObserveOnMainThread {
            when (it) {
                is CheckOutState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is CheckOutState.LoadingState -> {

                }
                is CheckOutState.QrCodeData -> {
                    loggedInUserCache.setLoyaltyQrResponse(QRScanResponse(it.data.phone, it.data.fullName, it.data.id, it.data.email, it.data.points))
                    if (it.data.id != null) {
                        qrUserId = it.data.id
                        checkOutViewModel.getLoyaltyPointDetails(it.data.id)
                    }
                    println("loggedInUserCache: ${loggedInUserCache.getLoyaltyQrResponse()?.email}")
                    qrScanResponse = it.data
                    dialogVisibility()
                    userDetailsEditTextVisibility(true)
                    RxBus.publish(RxEvent.EventPaymentButtonEnabled(true))
                    userDetailsSet()
                }
                is CheckOutState.PhoneLoyaltyData -> {
                    if (it.data.userId != null) {
                        qrUserId = it.data.userId
                        checkOutViewModel.getLoyaltyPointDetails(it.data.userId)
                        checkOutViewModel.getUser(it.data.userId)
                    }
                    println("loggedInUserCache: ${loggedInUserCache.getLoyaltyQrResponse()?.email}")
                    qrScanResponse = QRScanResponse(it.data.userPhone, "", it.data.userId, it.data.userEmail, it.data.points)
                    dialogVisibility()
                    userDetailsEditTextVisibility(true)
                    RxBus.publish(RxEvent.EventPaymentButtonEnabled(true))
                    binding.loyaltyCardLinearLayout.isVisible = true
                    binding.loyaltyCardLayout.tvLoyaltyPoint.isVisible = true
                    binding.loyaltyCardLayout.tvLoyaltyPoint.text = it.data.points.toString()
                    binding.loyaltyCardLayout.tvPoint.isVisible = true
                    redeemPoint = it.data.points!!
                    binding.loyaltyCardLayout.llHistory.removeAllViews()
                    binding.loyaltyCardLayout.llHistory.isVisible = true
                    it.data.data?.forEach { item ->
                        item.let { item1 ->
                            val v: View = View.inflate(context, R.layout.loyalty_history, null)
                            v.findViewById<AppCompatTextView>(R.id.productTextview).text = "#${item.order?.id}".plus(" - (").plus(item1.appliedPoints.toString()).plus(" leaves)")
                            v.findViewById<AppCompatTextView>(R.id.productTextDescription).text =
                                item1.dateCreated?.toDate()?.formatTo("MM/dd/yyyy, hh:mm a")
                            binding.loyaltyCardLayout.llHistory.addView(v)
                        }
                    }
                    userDetailsSet()
                }
                is CheckOutState.QrCodeScanError -> {
                    showToast(it.errorType)
                }
                is CheckOutState.GiftCard -> {
                    giftCardId = it.data.id ?: ""
                    giftCardResponse =
                        GiftCardResponse(giftCardAmount = it.data.giftCardAmout, giftCardRedemption = it.data.giftCardRedemption, id = it.data.id)
                    binding.addGiftCardLayout.rlHeader.visibility = View.GONE
                    binding.addGiftCardLayout.rlFooter.visibility = View.VISIBLE
                    binding.addGiftCardLayout.expandable.collapse()
                    binding.addGiftCardLayout.expandable.expand()
                    binding.addGiftCardLayout.giftCardBalanceTextView.text = it.data.giftCardAmout?.div(100).toDollar()
                }
                is CheckOutState.GiftCardQrResponse -> {
                    giftCardId = it.data.id ?: ""
                    giftCardResponse = it.data
                    binding.addGiftCardLayout.rlHeader.visibility = View.GONE
                    binding.addGiftCardLayout.rlFooter.visibility = View.VISIBLE
                    binding.addGiftCardLayout.expandable.collapse()
                    binding.addGiftCardLayout.expandable.expand()
                    binding.addGiftCardLayout.giftCardBalanceTextView.text = it.data.giftCardAmount?.div(100).toDollar()
                }
                is CheckOutState.PromocodeResponse -> {
                    couponCodeId = it.promocode.couponCodeId!!
                    binding.addPromocodeLayout.expandable.collapse()
                    requireActivity().hideKeyboard()
                    binding.addPromocodeLayout.promoCodeEditText.text?.clear()
                    binding.addPromocodeLayout.downArrowMaterialCardView.isVisible = false
                    binding.addPromocodeLayout.ivClose.isVisible = true
                    binding.addPromocodeLayout.giftCardDiscountPrizeTextView.isVisible = true
                    val discountPrice = it.promocode.discount.toString().removePrefix("-").toDouble()
                    binding.addPromocodeLayout.giftCardDiscountPrizeTextView.text = "-${discountPrice.div(100).toDollar()}"
                    RxBus.publish(RxEvent.AddPromoCode(discountPrice))
                }
                is CheckOutState.UserLoyaltyPoint -> {
                    binding.loyaltyCardLinearLayout.isVisible = true
                    binding.loyaltyCardLayout.tvLoyaltyPoint.isVisible = true
                    binding.loyaltyCardLayout.tvLoyaltyPoint.text = (it.data.points ?: 0).toString()
                    binding.loyaltyCardLayout.tvPoint.isVisible = true
                    redeemPoint = it.data.points ?: 0
                    binding.loyaltyCardLayout.llHistory.removeAllViews()
                    binding.loyaltyCardLayout.llHistory.isVisible = true
                    it.data.history?.forEach { item ->
                        item.let { item1 ->
                            val v: View = View.inflate(context, R.layout.loyalty_history, null)
                            v.findViewById<AppCompatTextView>(R.id.productTextview).text = "#${item.order?.id}".plus(" - (").plus(item1.appliedPoints.toString()).plus(" leaves)")
                            v.findViewById<AppCompatTextView>(R.id.productTextDescription).text =
                                item1.dateCreated?.toDate()?.formatTo("MM/dd/yyyy, hh:mm a")
                            binding.loyaltyCardLayout.llHistory.addView(v)
                        }
                    }
                }
                is CheckOutState.UserCreditPoint -> {
                    loggedInUserCache.setLoyaltyQrResponse(QRScanResponse(qrScanResponse.phone, it.data.fullName(), qrScanResponse.id,qrScanResponse.email, qrScanResponse.points))
                }
                else -> {}
            }
        }
    }

    private fun userDetailsSet() {
        binding.personalInformationLayout.userNameAppCompatTextView.text = qrScanResponse.fullName
        binding.personalInformationLayout.userPhoneNumberAppCompatTextView.text = qrScanResponse.phone
        binding.personalInformationLayout.userEmailAppCompatTextView.text = qrScanResponse.email
        binding.loyaltyCardLayout.tvLoyaltyPoint.isVisible = true
        binding.loyaltyCardLayout.tvLoyaltyPoint.text = qrScanResponse.points.toString()
        binding.loyaltyCardLayout.tvPoint.isVisible = true
    }

    private fun dialogVisibility() {
        binding.loyaltyCardDialogLayout.isVisible = false
        binding.container.isVisible = false
        binding.nestedScrollView.isVisible = true
        binding.checkoutPartLinearLayout.isVisible = true
    }

    private fun listenToViewEvent() {
        if (loggedInUserCache.getIsEmployeeMeal() == true) {
            binding.llCheckOut.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green_light_50))
        } else{
            binding.llCheckOut.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_F0F4F6))
        }
        if(loggedInUserCache.getLoyaltyQrResponse()?.id !=  "" && loggedInUserCache.getLoyaltyQrResponse()?.id !=  null) {
            checkOutViewModel.getLoyaltyPointDetails(loggedInUserCache.getLoyaltyQrResponse()?.id)
            binding.personalInformationLayout.userDetailsLayout.isVisible = false
            RxBus.publish(RxEvent.EventPaymentButtonEnabled(true))
            binding.personalInformationLayout.userNameAppCompatTextView.text = loggedInUserCache.getLoyaltyQrResponse()?.fullName
            binding.personalInformationLayout.userEmailAppCompatTextView.text = loggedInUserCache.getLoyaltyQrResponse()?.email
            binding.personalInformationLayout.userPhoneNumberAppCompatTextView.text = loggedInUserCache.getLoyaltyQrResponse()?.phone
            RxBus.publish(RxEvent.EventPaymentButtonEnabled(true))
        } else {
            RxBus.publish(RxEvent.EventPaymentButtonEnabled(true))
            binding.personalInformationLayout.userDetailsLayout.isVisible = true
            binding.personalInformationLayout.userNameAppCompatTextView.isVisible = false
            binding.personalInformationLayout.userEmailAppCompatTextView.isVisible = false
            binding.loyaltyCardLayout.root.isVisible = false
            binding.personalInformationLayout.userPhoneNumberAppCompatTextView.isVisible = false
            binding.personalInformationLayout.customerBirthDateCompatTextView.isVisible = false
        }
        binding.tvSkip.throttleClicks().subscribeAndObserveOnMainThread {
            userDetailsEditTextVisibility(false)
            binding.loyaltyCardDialogLayout.isVisible = false
            binding.loyaltyCardLinearLayout.isVisible = false
            binding.nestedScrollView.isVisible = true
            binding.checkoutPartLinearLayout.isVisible = true
            RxBus.publish(RxEvent.EventPaymentButtonEnabled(true))
        }.autoDispose()
        binding.joinLoyaltyProgramLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
            val joinLoyaltyProgramDialog = JoinLoyaltyProgramDialog()
            joinLoyaltyProgramDialog.show(requireFragmentManager(), CheckOutFragment::class.java.name)
        }.autoDispose()
        binding.scanCardButton.throttleClicks().subscribeAndObserveOnMainThread {
            qrCodeType = Constants.QR_CODE_TYPE_LOYALTY
            checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_REQUEST_CODE)
        }.autoDispose()
        binding.addGiftCardLayout.scanGiftCard.throttleClicks().subscribeAndObserveOnMainThread {
            qrCodeType = Constants.QR_CODE_TYPE_GIFT_CARD
            checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_REQUEST_CODE)
        }.autoDispose()
        binding.tipTheCrewLayout.applyButton.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
        }.autoDispose()

        binding.btnWithPhone.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            val phoneLoyaltyFragment = PhoneLoyaltyFragment().apply {
                checkWithPhoneClick.subscribeAndObserveOnMainThread {
                    checkOutViewModel.getPhoneLoyaltyData(it)
                    this.dismiss()
                }.autoDispose()
            }
            phoneLoyaltyFragment.show(parentFragmentManager, PhoneLoyaltyFragment::class.java.name)
        }.autoDispose()
        binding.addGiftCardLayout.applyButton.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            if (isGiftCardValidate()) {
                checkOutViewModel.applyGiftCard(binding.addGiftCardLayout.giftPackagingEditText.text.toString())
            }
        }.autoDispose()

        RxBus.listen(RxEvent.PassTotal::class.java).subscribeAndObserveOnMainThread {
            orderTotal = it.redeemPoint
        }.autoDispose()
        RxBus.listen(RxEvent.EventTotalCheckOut::class.java).subscribeAndObserveOnMainThread {
            subTotal = it.orderPrice.orderSubtotal!!
            it.orderPrice.orderTotal?.let { it ->
                orderTotal = it
            }
            qrUserId = loggedInUserCache.getLoyaltyQrResponse()?.id
            if (!qrUserId.isNullOrEmpty()) {
                checkOutViewModel.getLoyaltyPointDetails(qrUserId)
            }
            if (orderTotal == 0.00) {
                if (loggedInUserCache.getIsEmployeeMeal() == true) {
                    binding.loyaltyCardDialogLayout.isVisible = false
                    binding.loyaltyCardLinearLayout.isVisible = false
                    binding.nestedScrollView.isVisible = true
                    binding.checkoutPartLinearLayout.isVisible = true
                    RxBus.publish(RxEvent.EventCreateOrderMaterialButton(true))
                    userDetailsEditTextVisibility(true)
                    binding.personalInformationLayout.userNameAppCompatTextView.text = loggedInUserCache.getLoyaltyQrResponse()?.fullName
                    binding.personalInformationLayout.customerBirthDateCompatTextView.text = "-".toString()
                    binding.personalInformationLayout.userPhoneNumberAppCompatTextView.text = loggedInUserCache.getLoyaltyQrResponse()?.phone
                    binding.personalInformationLayout.userEmailAppCompatTextView.text = loggedInUserCache.getLoyaltyQrResponse()?.email
                    binding.addGiftCardLayout.downArrowMaterialCardView.isClickable = false
                    binding.addPromocodeLayout.downArrowMaterialCardView.isEnabled = false
                    binding.addGiftCardLayout.expandable.collapse()
                    binding.addPromocodeLayout.expandable.collapse()
                } else {
                    RxBus.publish(RxEvent.EventCreateOrderMaterialButton(true))
                    if (loggedInUserCache.getLoyaltyQrResponse()?.id != "" && loggedInUserCache.getLoyaltyQrResponse()?.id != null){
                        userDetailsEditTextVisibility(true)
                        binding.personalInformationLayout.userNameAppCompatTextView.text = loggedInUserCache.getLoyaltyQrResponse()?.fullName
                        if (loggedInUserCache.getLoyaltyQrResponse()?.phone != "") binding.personalInformationLayout.userPhoneNumberAppCompatTextView.text =
                            loggedInUserCache.getLoyaltyQrResponse()?.phone
                        if (loggedInUserCache.getLoyaltyQrResponse()?.email != "") binding.personalInformationLayout.userEmailAppCompatTextView.text =
                            loggedInUserCache.getLoyaltyQrResponse()?.email
                    }
                    binding.loyaltyCardDialogLayout.isVisible = false
                    binding.loyaltyCardLinearLayout.isVisible = false
                    binding.nestedScrollView.isVisible = true
                    binding.checkoutPartLinearLayout.isVisible = true
                    binding.addGiftCardLayout.expandable.collapse()
                    binding.addPromocodeLayout.expandable.collapse()
                    binding.addGiftCardLayout.downArrowMaterialCardView.isClickable = false
                    binding.addPromocodeLayout.downArrowMaterialCardView.isEnabled = false
                }
            } else {
                binding.addGiftCardLayout.downArrowMaterialCardView.isClickable = true
                binding.addPromocodeLayout.downArrowMaterialCardView.isClickable = true
                binding.addPromocodeLayout.downArrowMaterialCardView.isEnabled = true
            }
        }.autoDispose()
        RxBus.listen(RxEvent.EventGotoStartButton::class.java).subscribeAndObserveOnMainThread {
            removeGiftCardAndPromoCode()
            binding.personalInformationLayout.nameEditText.text = null
            binding.personalInformationLayout.surNameEditText.text = null
            binding.personalInformationLayout.phoneEditText.text = null
            binding.personalInformationLayout.emailEditText.text = null
        }.autoDispose()
        binding.addPromocodeLayout.applyButton.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            if (isPromocodeValidate() && orderTotal != 0.00) {
                checkOutViewModel.applyPromocode(
                    PromoCodeRequest(
                        0,
                        binding.addPromocodeLayout.promoCodeEditText.text.toString(),
                        subTotal?.times(100),
                        loggedInUserCache.getLoggedInUserCartGroupId()
                    )
                )
            }
        }.autoDispose()
        binding.loyaltyCardLayout.downArrowMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.loyaltyCardLayout.expandable.isExpanded) {
                binding.loyaltyCardLayout.downArrowImageView.isSelected = false
                binding.loyaltyCardLayout.expandable.collapse()
            } else {
                binding.loyaltyCardLayout.downArrowImageView.isSelected = true
                binding.loyaltyCardLayout.expandable.expand()
            }
        }.autoDispose()
        binding.addGiftCardLayout.downArrowMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.addGiftCardLayout.expandable.isExpanded) {
                binding.addGiftCardLayout.downArrowImageView.isSelected = false
                binding.addGiftCardLayout.expandable.collapse()
            } else {
                binding.addGiftCardLayout.downArrowImageView.isSelected = true
                binding.addGiftCardLayout.expandable.expand()
            }
        }.autoDispose()
        binding.addPromocodeLayout.downArrowMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.addPromocodeLayout.expandable.isExpanded) {
                binding.addPromocodeLayout.downArrowImageView.isSelected = false
                binding.addPromocodeLayout.expandable.collapse()
            } else {
                binding.addPromocodeLayout.downArrowImageView.isSelected = true
                binding.addPromocodeLayout.expandable.expand()
            }
        }.autoDispose()
        binding.userCreditsLayout.downArrowMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.userCreditsLayout.expandable.isExpanded) {
                binding.userCreditsLayout.downArrowImageView.isSelected = false
                binding.userCreditsLayout.expandable.collapse()
            } else {
                binding.userCreditsLayout.downArrowImageView.isSelected = true
                binding.userCreditsLayout.expandable.expand()
            }
        }.autoDispose()
        binding.userCreditsLayout.applyButton.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            val subtotal = binding.userCreditsLayout.giftPackagingEditText.text.toString().removePrefix("$").toDouble().toConvertDecimalFormat()
            subtotal1 = binding.userCreditsLayout.giftPackagingEditText.text.toString().removePrefix("$").toInt() * 100
            if (subtotal1 <= usersCreditPrice && subtotal != 0.0) {
                if (subtotal > orderTotal) {
                    showToast("can't add credit more than your order price")
                } else {
                    binding.userCreditsLayout.downArrowImageView.isSelected = false
                    binding.userCreditsLayout.expandable.collapse()
                    RxBus.publish(RxEvent.AddCredit(subtotal.times(100)))
                    binding.userCreditsLayout.downArrowMaterialCardView.isVisible = false
                    binding.userCreditsLayout.ivClose.isVisible = true
                    binding.userCreditsLayout.creditPointTextView.text = "-".plus(subtotal.toDollar())
                    binding.userCreditsLayout.tvPoint.text = resources.getString(R.string.credits)
                    binding.userCreditsLayout.giftPackagingEditText.setText("$")
                }
            } else {
                if (usersCreditPrice != 0) {
                    if (subtotal == 0.0) {
                        showToast("can't give credit 0")
                    } else {
                        showToast("can't give credit more then your credit Point")
                    }
                } else {
                    showToast("your credit point is 0")
                }
            }
        }.autoDispose()
        binding.userCreditsLayout.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            binding.userCreditsLayout.downArrowMaterialCardView.isVisible = true
            binding.userCreditsLayout.ivClose.isVisible = false
            binding.userCreditsLayout.creditPointTextView.text = "$usersCreditPrice"
            binding.userCreditsLayout.tvPoint.text = resources.getString(R.string.credits_points)
            RxBus.publish(RxEvent.RemoveCredit(false))
        }.autoDispose()
        binding.addGiftCardLayout.edtAmountToPay.throttleClicks().subscribeAndObserveOnMainThread {
            binding.addGiftCardLayout.edtAmountToPay.requestFocus()
        }.autoDispose()
        binding.addGiftCardLayout.applyAmountButton.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.addGiftCardLayout.edtAmountToPay.text.toString() != "$" && binding.addGiftCardLayout.edtAmountToPay.text.toString() != "") {
                requireActivity().hideKeyboard()
                val giftCard = binding.addGiftCardLayout.edtAmountToPay.text.toString()
                binding.addGiftCardLayout.edtAmountToPay.setText("$")
                enterGiftCardPrize = (giftCard.removePrefix("$").toDouble() * 100).toInt()
                if (giftCardResponse.giftCardAmount!! >= enterGiftCardPrize && enterGiftCardPrize <= orderTotal.times(100)) {
                    binding.addGiftCardLayout.expandable.collapse()
                    binding.addGiftCardLayout.downArrowMaterialCardView.isVisible = false
                    binding.addGiftCardLayout.ivClose.isVisible = true
                    binding.addGiftCardLayout.giftCardDiscountPrizeTextView.isVisible = true
                    val giftCardAmount = enterGiftCardPrize.toDouble()
                    binding.addGiftCardLayout.giftCardDiscountPrizeTextView.text = "-"+giftCardAmount.div(100).toDollar()
                    RxBus.publish(RxEvent.AddGiftCart(enterGiftCardPrize.toDouble(),giftCardResponse.id ?: ""))
                } else {
                    showToast("please enter Valid Value")
                }
            } else {
                showToast("please enter GiftCard Amount")
            }
        }.autoDispose()
        binding.addGiftCardLayout.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            binding.addGiftCardLayout.tvAddGiftCard.text = getString(R.string.add_gift_card).toUpperCase(Locale.getDefault())
            binding.addGiftCardLayout.downArrowMaterialCardView.isVisible = true
            binding.addGiftCardLayout.ivClose.isVisible = false
            binding.addGiftCardLayout.giftCardDiscountPrizeTextView.isVisible = false
            binding.addPromocodeLayout.downArrowImageView.isSelected = false
            binding.addGiftCardLayout.rlHeader.isVisible = true
            binding.addGiftCardLayout.rlFooter.isVisible = false
            RxBus.publish(RxEvent.RemoveGiftCart(false))
        }.autoDispose()
        binding.addPromocodeLayout.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            binding.addPromocodeLayout.promoCodeEditText.setHint(R.string.xxxx_xxxx)
            binding.addPromocodeLayout.downArrowMaterialCardView.isVisible = true
            binding.addPromocodeLayout.ivClose.isVisible = false
            binding.addPromocodeLayout.giftCardDiscountPrizeTextView.isVisible = false
            binding.addPromocodeLayout.downArrowImageView.isSelected = false
            RxBus.publish(RxEvent.RemovePromoCode(false))
        }.autoDispose()
        RxBus.listen(RxEvent.EventCheckValidation::class.java).subscribeAndObserveOnMainThread {
            if (loggedInUserCache.getLoyaltyQrResponse()?.fullName == null || binding.personalInformationLayout.userDetailsLayout.isVisible) {
                if (isValidate()) {
                    val userName = binding.personalInformationLayout.nameEditText.text.toString()
                    val surName = binding.personalInformationLayout.surNameEditText.text.toString()
                    val phone = binding.personalInformationLayout.phoneEditText.text.toString()
                    val email = binding.personalInformationLayout.emailEditText.text.toString()
                    loggedInUserCache.setLoyaltyQrResponse(QRScanResponse(phone, userName, null, email, lastName = surName))
                    RxBus.publish(RxEvent.EventGoToPaymentScreen(true))
                    RxBus.publish(RxEvent.PassPromocodeAndGiftCard(couponCodeId, enterGiftCardPrize, giftCardId))
                    RxBus.publish(RxEvent.PassCreditAmount(subtotal1))
                } else {
                    RxBus.publish(RxEvent.CheckOutValidationFailed)
                }
            } else {
                if (binding.personalInformationLayout.userDetailsLayout.isVisible) {
                    if (isValidate()) {
                        val userName = binding.personalInformationLayout.nameEditText.text.toString()
                        val surName = binding.personalInformationLayout.surNameEditText.text.toString()
                        val phone = binding.personalInformationLayout.phoneEditText.text.toString()
                        val email = binding.personalInformationLayout.emailEditText.text.toString()
                        loggedInUserCache.setLoyaltyQrResponse(QRScanResponse(phone, userName, null, email, lastName = surName))
                        RxBus.publish(RxEvent.EventGoToPaymentScreen(true))
                        RxBus.publish(RxEvent.PassPromocodeAndGiftCard(couponCodeId, enterGiftCardPrize, giftCardId))
                        RxBus.publish(RxEvent.PassCreditAmount(subtotal1))
                    } else{
                        RxBus.publish(RxEvent.CheckOutValidationFailed)
                    }
                } else {
                    RxBus.publish(RxEvent.EventGoToPaymentScreen(true))
                    RxBus.publish(RxEvent.PassPromocodeAndGiftCard(couponCodeId, enterGiftCardPrize, giftCardId))
                    RxBus.publish(RxEvent.PassCreditAmount(subtotal1))
                    RxBus.publish(RxEvent.EventGoToPaymentScreen(true))
                }
            }
        }.autoDispose()
        RxBus.listen(RxEvent.EventValidation::class.java).subscribeAndObserveOnMainThread {
            if (binding.personalInformationLayout.userDetailsLayout.isVisible) {
                if (isValidate()) {
                    val userName = binding.personalInformationLayout.nameEditText.text.toString()
                    val surName = binding.personalInformationLayout.surNameEditText.text.toString()
                    val phone = binding.personalInformationLayout.phoneEditText.text.toString()
                    val email = binding.personalInformationLayout.emailEditText.text.toString()
                    loggedInUserCache.setLoyaltyQrResponse(QRScanResponse(phone, userName, null, email, lastName = surName))
                    RxBus.publish(RxEvent.PassPromocodeAndGiftCard(couponCodeId, enterGiftCardPrize, giftCardId))
                    RxBus.publish(RxEvent.PassCreditAmount(subtotal1))
                    RxBus.publish(RxEvent.EventGoToBack(true))
                }
            }
        }.autoDispose()
        RxBus.listen(RxEvent.EventDismissLoyaltyRegistrationSuccess::class.java).subscribeAndObserveOnMainThread {
            binding.loyaltyCardDialogLayout.isVisible = false
            binding.container.isVisible = false
            userDetailsEditTextVisibility(true)
            binding.personalInformationLayout.userNameAppCompatTextView.text = loggedInUserCache.getLoyaltyQrResponse()?.fullName
            if (loggedInUserCache.getLoyaltyQrResponse()?.phone != "") binding.personalInformationLayout.userPhoneNumberAppCompatTextView.text =
                loggedInUserCache.getLoyaltyQrResponse()?.phone
            if (loggedInUserCache.getLoyaltyQrResponse()?.email != "") binding.personalInformationLayout.userEmailAppCompatTextView.text =
                loggedInUserCache.getLoyaltyQrResponse()?.email
            checkOutViewModel.getLoyaltyPointDetails(loggedInUserCache.getLoyaltyQrResponse()?.id)
        }.autoDispose()

        binding.loyaltyCardLayout.redeemPointsButton.throttleClicks().subscribeAndObserveOnMainThread {
            binding.llCheckOut.isVisible = false
            binding.container.isVisible = true
            RxBus.publish(RxEvent.OpenRedeemPoint(redeemPoint))
            val trans: FragmentTransaction = parentFragmentManager.beginTransaction()
            trans.replace(R.id.container, RedeemProductFragment.newInstance(redeemPoint))
            trans.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            trans.commit()

        }.autoDispose()

        RxBus.listen(RxEvent.VisibleCheckOutScreen::class.java).subscribeAndObserveOnMainThread {
            binding.llCheckOut.isVisible = true
            binding.container.isVisible = false
            checkOutViewModel.getLoyaltyPointDetails(loggedInUserCache.getLoyaltyQrResponse()?.id)
        }.autoDispose()
    }


    private fun checkPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(permission), requestCode)
        } else {
            val qrScannerFragment = QrScannerFragment().apply {
                qrData.subscribeAndObserveOnMainThread {
                    dismiss()
                    if (qrCodeType == Constants.QR_CODE_TYPE_LOYALTY) {
                        checkOutViewModel.getQRData(it)
                    } else {
                        checkOutViewModel.giftCardQRCode(it)
                    }
                    requireActivity().hideKeyboard()
                }.autoDispose()
            }
            qrScannerFragment.show(requireFragmentManager(), "")

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == CAMERA_PERMISSION_REQUEST_CODE) {
            println("CAMERA_PERMISSION_REQUEST_CODE")
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        println("request Code :${grantResults}")
        if (grantResults[0] == CAMERA_PERMISSION_REQUEST_CODE) {
            println("Camera Permission Granted")
        } else {
            Toast.makeText(requireContext(), "Camera Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isValidate(): Boolean {
        return when {
            binding.personalInformationLayout.nameEditText.isFieldBlank() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_name), Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun isGiftCardValidate(): Boolean {
        return when {
            binding.addGiftCardLayout.giftPackagingEditText.isFieldBlank() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_GiftCard), Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun isPromocodeValidate(): Boolean {
        return when {
            binding.addPromocodeLayout.promoCodeEditText.isFieldBlank() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_Promocode), Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun userDetailsEditTextVisibility(b: Boolean) {
        binding.checkoutPartLinearLayout.isVisible = b
        binding.personalInformationLayout.userNameAppCompatTextView.isVisible = b
        binding.personalInformationLayout.customerBirthDateCompatTextView.isVisible = b
        binding.personalInformationLayout.userPhoneNumberAppCompatTextView.isVisible = b
        binding.personalInformationLayout.userEmailAppCompatTextView.isVisible = b
        binding.personalInformationLayout.userDetailsLayout.isVisible = !b
    }

    private fun removeGiftCardAndPromoCode() {
        binding.addGiftCardLayout.ivClose.isVisible = false
        binding.addGiftCardLayout.giftCardDiscountPrizeTextView.isVisible = false
        binding.addGiftCardLayout.downArrowMaterialCardView.isVisible = true
        binding.addGiftCardLayout.downArrowImageView.isSelected = false
        binding.addGiftCardLayout.rlHeader.isVisible = true
        binding.addGiftCardLayout.rlFooter.isVisible = false
        binding.addPromocodeLayout.ivClose.isVisible = false
        binding.addPromocodeLayout.giftCardDiscountPrizeTextView.isVisible = false
        binding.addPromocodeLayout.downArrowMaterialCardView.isVisible = true
        binding.addPromocodeLayout.downArrowImageView.isSelected = false
        binding.userCreditsLayout.creditPointTextView.isVisible = false
        binding.userCreditsLayout.tvPoint.isVisible = false
        binding.userCreditsLayout.downArrowMaterialCardView.isVisible = true
        binding.userCreditsLayout.downArrowImageView.isSelected = false
    }

    override fun onResume() {
        super.onResume()
        binding.personalInformationLayout.nameEditText.text = null
        binding.personalInformationLayout.surNameEditText.text = null
        binding.personalInformationLayout.phoneEditText.text = null
        binding.personalInformationLayout.emailEditText.text = null
        binding.llCheckOut.isVisible = true
        binding.container.isVisible = false
    }
}