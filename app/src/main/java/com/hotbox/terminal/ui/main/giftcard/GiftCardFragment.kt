package com.hotbox.terminal.ui.main.giftcard

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.giftcard.model.*
import com.hotbox.terminal.api.stripe.model.CaptureNewPaymentRequest
import com.hotbox.terminal.api.stripe.model.PaymentStatus
import com.hotbox.terminal.api.stripe.model.Resource
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseFragment
import com.hotbox.terminal.base.RxBus
import com.hotbox.terminal.base.RxEvent
import com.hotbox.terminal.base.ViewModelFactory
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.FragmentGiftCardBinding
import com.hotbox.terminal.ui.main.giftcard.viewmodel.GiftCardState
import com.hotbox.terminal.ui.main.giftcard.viewmodel.GiftCardViewModel
import com.hotbox.terminal.ui.userstore.checkout.QrScannerFragment
import com.hotbox.terminal.utils.Constants
import com.hotbox.terminal.utils.Constants.TRANSACTION_CHARGE_ID
import com.hotbox.terminal.utils.Constants.TRANSACTION_ID_OF_PROCESSOR
import io.reactivex.Observable
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class GiftCardFragment : BaseFragment() {
    companion object {
        @JvmStatic
        fun newInstance() = GiftCardFragment()
    }

    private var buyPhysicalCardRequest: BuyPhysicalCardRequest? = null
    private var buyVirtualCardRequest: BuyVirtualCardRequest? = null
    private var qrCodeType: String = Constants.QR_CODE_TYPE_GIFT_CARD
    private var _binding: FragmentGiftCardBinding? = null
    private val binding get() = _binding!!
    private val CAMERA_PERMISSION_REQUEST_CODE = 2323

    private var isFVisible = true
    private lateinit var paymentIntentId: String
    private var paymentErrorType: String? = null

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<GiftCardViewModel>
    private lateinit var giftCardViewModel: GiftCardViewModel

    @Inject
    lateinit var loggedInUserCache :LoggedInUserCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        giftCardViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGiftCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewModel()
        listenToViewEvent()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun listenToViewEvent() {
        val params = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        val virtualParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        virtualParams.setMargins(0, 290, 0, 0)
        params.setMargins(0, 0, 0, 0)
        binding.cardDetailsPart.cardTypeRadioGroup.layoutParams = params
        requireActivity().hideKeyboard(binding.clGiftCard.rootView)
        binding.clGiftCard.setOnTouchListener { v, event ->
            requireActivity().hideKeyboard(v)
        }
        binding.cardDetailsPart.virtualRadioButton.isEnabled = false
        physicalOrVirtualSelection(false)
        binding.cardDetailsPart.amountEditText.imeOptions = EditorInfo.IME_ACTION_DONE
        binding.cardDetailsPart.physicalRadioButton.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.cardDetailsPart.physicalRadioButton.isChecked) {
                val params1 = binding.cardDetailsPart.registerCardButton.layoutParams as ConstraintLayout.LayoutParams
                params1.topToTop = ConstraintLayout.LayoutParams.UNSET
                params1.endToEnd = ConstraintLayout.LayoutParams.UNSET
                params1.startToEnd = ConstraintLayout.LayoutParams.UNSET
                params1.endToStart = ConstraintLayout.LayoutParams.UNSET
                params1.topToBottom = R.id.llCardDetails
                binding.cardDetailsPart.registerCardButton.requestLayout()
                binding.cardDetailsPart.llCardDetails.isVisible = true
                binding.cardDetailsPart.llAmount.isVisible = false
                binding.cardDetailsPart.userDetailsLayout.isVisible = false
                binding.cardDetailsPart.amountEditText.imeOptions = EditorInfo.IME_ACTION_DONE
                binding.cardDetailsPart.cardTypeRadioGroup.layoutParams = params
            }
            requireActivity().hideKeyboard(binding.cardDetailsPart.physicalRadioButton.rootView)
        }.autoDispose()

        binding.pendingPayment.tryAgainMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            binding.pendingPayment.root.isVisible = false
            binding.cardBalance.root.isVisible = false
            binding.constraintGiftCard.isVisible = true
            binding.cardDetailsPart.root.isVisible = true
        }.autoDispose()
        binding.cardDetailsPart.registerCardButton.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            if (binding.cardDetailsPart.physicalRadioButton.isChecked) {
                if (isPhysicalGiftCardValidate()) {
                    if (isPosKeyBYPASS()) {
                            buyPhysicalCardRequest = BuyPhysicalCardRequest(
                                giftCardAmout = binding.cardDetailsPart.amountEditText.text.toString().toDouble().times(100).toInt(),
                                giftCardCode = binding.cardDetailsPart.giftPackagingEditText.text.toString(),
                                transactionIdOfProcessor = generateTransactionId() ,
                                transactionChargeId = "4",
                            )
                            buyPhysicalCardRequest?.let { item -> giftCardViewModel.buyPhysicalGiftCard(item) }

                    } else {
                        val newPaymentRequest = CaptureNewPaymentRequest(
                            resource = Resource(binding.cardDetailsPart.amountEditText.text.toString().toDouble().times(100).toInt())
                        )
                        giftCardViewModel.captureNewPayment(newPaymentRequest)
                    }
                    binding.constraintGiftCard.isVisible = false
                    binding.pendingPayment.root.isVisible = true
                    binding.pendingPayment.ivPaymentError.setImageResource(R.drawable.ic_scan_loyalty_card)
                    binding.pendingPayment.dialogHeading.text = resources.getString(R.string.pending_payment)
                    binding.pendingPayment.tvDescription.text = resources.getString(R.string.complete_your_payment_on_the_payment_terminal)
                    binding.pendingPayment.tryAgainMaterialButton.isVisible = false
                    binding.pendingPayment.tvTotalPrizeNumber.text = binding.cardDetailsPart.amountEditText.text.toString().toDouble().toDollar()
                }
            } else {
                if (isVirtualGiftCardValidate()) {
                    val giftCardPurchaserFirstName = binding.cardDetailsPart.edtPurchaserNameEditText.text.toString()
                    val giftCardPurchaserLastName = binding.cardDetailsPart.edtPurchaserSurNameEditText.text.toString()
                    val giftCardRecipientFirstName = binding.cardDetailsPart.edtRecipientNameEditText.text.toString()
                    val giftCardRecipientLastName = binding.cardDetailsPart.edtRecipientSurNameEditText.text.toString()
                    if (isPosKeyBYPASS()) {
                        buyVirtualCardRequest = BuyVirtualCardRequest(
                            transactionId = generateTransactionId(),
                            giftCardPurchaserFirstName = giftCardPurchaserFirstName,
                            giftCardPurchaserLastName = giftCardPurchaserLastName,
                            giftCardPurchaserEmail = binding.cardDetailsPart.edtPurchaserEmailEditText.text.toString(),
                            giftCardRecipientFirstName = giftCardRecipientFirstName,
                            giftCardRecipientLastName = giftCardRecipientLastName,
                            giftCardRecipientEmail = binding.cardDetailsPart.edtRecipientEmailEditText.text.toString(),
                            giftCardAmout = binding.cardDetailsPart.edtAmount.text.toString().toDouble().times(100).toInt(),
                            giftCardPersonalMessage = binding.cardDetailsPart.edtPersonalMessage.text.toString().ifEmpty { null },
                            transactionIdOfProcessor = generateTransactionId() ,
                            transactionChargeId = "4",
                        )
                        buyVirtualCardRequest?.let { item -> giftCardViewModel.buyVirtualGiftCard(item) }

                    } else {
                        val newPaymentRequest = CaptureNewPaymentRequest(
                            resource = Resource(binding.cardDetailsPart.edtAmount.text.toString().toDouble().times(100).toInt())
                        )
                        giftCardViewModel.captureNewPayment(newPaymentRequest)
                    }
                    binding.constraintGiftCard.isVisible = false
                    binding.pendingPayment.root.isVisible = true
                    binding.pendingPayment.ivPaymentError.setImageResource(R.drawable.ic_scan_loyalty_card)
                    binding.pendingPayment.dialogHeading.text = resources.getString(R.string.pending_payment)
                    binding.pendingPayment.tvDescription.text = resources.getString(R.string.complete_your_payment_on_the_payment_terminal)
                    binding.pendingPayment.tryAgainMaterialButton.isVisible = false
                    binding.pendingPayment.tvTotalPrizeNumber.text = binding.cardDetailsPart.edtAmount.text.toString().toDouble().toDollar()
                }
            }
        }

        binding.cardDetailsPart.virtualRadioButton.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.cardDetailsPart.virtualRadioButton.isChecked) {
                binding.cardDetailsPart.llCardDetails.isVisible = false
                binding.cardDetailsPart.llAmount.isVisible = true
                binding.cardDetailsPart.userDetailsLayout.isVisible = true
                val params1 = binding.cardDetailsPart.registerCardButton.layoutParams as ConstraintLayout.LayoutParams
                params1.topToTop = ConstraintLayout.LayoutParams.UNSET
                params1.endToEnd = ConstraintLayout.LayoutParams.UNSET
                params1.startToEnd = ConstraintLayout.LayoutParams.UNSET
                params1.endToStart = ConstraintLayout.LayoutParams.UNSET
                params1.topToBottom = R.id.userDetailsLayout
                binding.cardDetailsPart.registerCardButton.requestLayout()
                binding.cardDetailsPart.amountEditText.imeOptions = EditorInfo.IME_ACTION_NEXT
                binding.cardDetailsPart.cardTypeRadioGroup.layoutParams = virtualParams
            }
            requireActivity().hideKeyboard(binding.cardDetailsPart.virtualRadioButton.rootView)
        }.autoDispose()

        binding.newCardSelectLinear.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            physicalOrVirtualSelection(false)
        }.autoDispose()

        binding.checkBalanceSelectLinear.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            physicalOrVirtualSelection(true)
        }.autoDispose()
        binding.checkCardBalance.scanGiftCard.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            qrCodeType = Constants.QR_CODE_TYPE_GIFT_CARD
            checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_REQUEST_CODE)
        }.autoDispose()
        binding.cardBalance.okThanksButton.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            if (binding.newCardSelectLinear.isSelected) {
                binding.cardDetailsPart.root.isVisible = true
                binding.cardBalance.root.isVisible = false
            } else {
                binding.cardBalance.root.isVisible = false
                binding.checkCardBalance.root.isVisible = true
            }
        }.autoDispose()
        binding.checkCardBalance.checkBalanceMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            giftCardViewModel.applyGiftCard(binding.checkCardBalance.cardIdEditText.text.toString())
        }.autoDispose()
        binding.cardBalance.TryAgainButton.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.cardDetailsPart.physicalRadioButton.isChecked) {
                buyPhysicalCardRequest?.let { giftCardViewModel.buyPhysicalGiftCard(it) }
            } else {
                buyVirtualCardRequest?.let { giftCardViewModel.buyVirtualGiftCard(it) }
            }
        }.autoDispose()
        binding.cardSuccessPart.backToCheckoutButton.throttleClicks().subscribeAndObserveOnMainThread {
            binding.cardSuccessPart.root.isVisible = false
            binding.cardBalance.root.isVisible = false
            binding.constraintGiftCard.isVisible = true
            binding.cardDetailsPart.root.isVisible = true
        }.autoDispose()
    }

    private fun listenToViewModel() {
        giftCardViewModel.giftCardState.subscribeAndObserveOnMainThread {
            when (it) {
                is GiftCardState.ErrorMessage -> {
                    showToast(it.errorMessage)
//                    binding.constraintGiftCard.isVisible = true
//                    binding.checkCardBalance.root.isVisible = false
//                    binding.cardDetailsPart.root.isVisible = false
//                    binding.cardSuccessPart.root.isVisible = false
//                    binding.cardBalance.root.isVisible = true
                    if (binding.newCardSelectLinear.isSelected) {
                        if (binding.cardDetailsPart.physicalRadioButton.isChecked) {
                            binding.cardBalance.tvCardBalance.isAllCaps = false
                            binding.cardBalance.tvCardBalance.text = getString(R.string.failed_to_reload_gift_card)
                            binding.cardBalance.tvCardId.text = getString(R.string.card_error_message)
                            binding.cardBalance.cardBalanceImage.setImageResource(R.drawable.ic_error)
                            binding.cardBalance.TryAgainButton.isVisible = true
                            binding.cardBalance.okThanksButton.isVisible = false
                        } else {
                            binding.cardBalance.tvCardBalance.isAllCaps = false
                            binding.cardBalance.tvCardBalance.text = getString(R.string.failed_to_create_gift_card)
                            binding.cardBalance.tvCardId.text = getString(R.string.card_error_message)
                            binding.cardBalance.cardBalanceImage.setImageResource(R.drawable.ic_error)
                            binding.cardBalance.TryAgainButton.isVisible = true
                            binding.cardBalance.okThanksButton.isVisible = false
                        }
                    } else {
                        binding.cardBalance.tvCardBalance.text = getString(R.string.card_not_found)
                        binding.cardBalance.tvCardBalance.isAllCaps = true
                        binding.cardBalance.tvCardId.text = getString(R.string.card_error_message)
                        binding.cardBalance.cardBalanceImage.setImageResource(R.drawable.ic_error)
                    }
                }
                is GiftCardState.NewPaymentErrorMessage -> {
                    binding.pendingPayment.ivPaymentError.setImageResource(R.drawable.ic_error)
                    binding.pendingPayment.dialogHeading.text = resources.getString(R.string.payment_error)
                    binding.pendingPayment.tvDescription.text = resources.getString(R.string.payment_failed)
                    binding.pendingPayment.tryAgainMaterialButton.isVisible = true

                }
                is GiftCardState.LoadingState -> {

                }
                is GiftCardState.QrCodeScanError -> {
                    showToast(it.errorType)
                }
                is GiftCardState.GiftCardQrResponse -> {
                    binding.checkCardBalance.root.isVisible = false
                    binding.cardDetailsPart.root.isVisible = false
                    binding.cardSuccessPart.root.isVisible = false
                    binding.cardBalance.root.isVisible = true
                    binding.cardBalance.cardBalanceImage.setImageResource(R.drawable.ic_scan_gift_card)
                    val amount = getColoredSpanned(it.data.giftCardAmount?.div(100).toDollar(), ContextCompat.getColor(requireContext(), R.color.orange))
                    binding.cardBalance.tvCardBalance.text = Html.fromHtml("${getString(R.string.card_balance_100)} $amount")
                    binding.cardBalance.tvCardId.text = getString(R.string.card_id_, "${it.data.id}")
                }
                is GiftCardState.GiftCard -> {
                    binding.checkCardBalance.root.isVisible = false
                    binding.cardDetailsPart.root.isVisible = false
                    binding.cardSuccessPart.root.isVisible = false
                    binding.cardBalance.root.isVisible = true
                    binding.checkCardBalance.cardIdEditText.text?.clear()
                    binding.cardBalance.cardBalanceImage.setImageResource(R.drawable.ic_scan_gift_card)
                    val amount = getColoredSpanned(it.data.giftCardAmout?.div(100).toDollar(), ContextCompat.getColor(requireContext(), R.color.orange))
                    binding.cardBalance.tvCardBalance.text = Html.fromHtml("${getString(R.string.card_balance_100)} $amount")
                    binding.cardBalance.tvCardId.text = getString(R.string.card_id_, "${it.data.id}")
                }
                is GiftCardState.CaptureNewPaymentIntent -> {
                    when (it.createPaymentIntentResponse?.firstOrNull()?.getPaymentStatus()) {
                        PaymentStatus.CancelledByUser -> {
                            binding.constraintGiftCard.isVisible = false
                            binding.pendingPayment.ivPaymentError.setImageResource(R.drawable.ic_error)
                            binding.pendingPayment.dialogHeading.text = resources.getString(R.string.payment_error)
                            binding.pendingPayment.tvDescription.text = it.createPaymentIntentResponse.firstOrNull()?.status
                            binding.pendingPayment.tryAgainMaterialButton.isVisible = true

                        }
                        PaymentStatus.InProgress -> {
                            binding.constraintGiftCard.isVisible = false
                            binding.pendingPayment.root.isVisible = true
                        }
                        PaymentStatus.Success -> {
                            binding.pendingPayment.root.isVisible = false
                            binding.constraintGiftCard.isVisible = false
                            binding.cardSuccessPart.root.isVisible = true
                            binding.cardBalance.tvCardBalance.text = getString(R.string.gift_card_purchased_successfully)
                            if (binding.cardDetailsPart.physicalRadioButton.isChecked) {
                                buyPhysicalCardRequest = BuyPhysicalCardRequest(
                                    transactionId = it.createPaymentIntentResponse.firstOrNull()?.hostTransactionReference,
                                    giftCardAmout = binding.cardDetailsPart.amountEditText.text.toString().toDouble().times(100).toInt(),
                                    giftCardCode = binding.cardDetailsPart.giftPackagingEditText.text.toString(),
                                    transactionIdOfProcessor = if(isPosKeyBYPASS()) generateTransactionId() else it.createPaymentIntentResponse.firstOrNull()?.hostTransactionReference,
                                    transactionChargeId = it.createPaymentIntentResponse.firstOrNull()?.referenceNo,
                                )
                                buyPhysicalCardRequest?.let { item -> giftCardViewModel.buyPhysicalGiftCard(item) }
                            } else {
                                val giftCardPurchaserFirstName = binding.cardDetailsPart.edtPurchaserNameEditText.text.toString()
                                val giftCardPurchaserLastName = binding.cardDetailsPart.edtPurchaserSurNameEditText.text.toString()
                                val giftCardRecipientFirstName = binding.cardDetailsPart.edtRecipientNameEditText.text.toString()
                                val giftCardRecipientLastName = binding.cardDetailsPart.edtRecipientSurNameEditText.text.toString()
                                buyVirtualCardRequest = BuyVirtualCardRequest(
                                    transactionId = it.createPaymentIntentResponse.firstOrNull()?.hostTransactionReference,
                                    giftCardPurchaserFirstName = giftCardPurchaserFirstName,
                                    giftCardPurchaserLastName = giftCardPurchaserLastName,
                                    giftCardPurchaserEmail = binding.cardDetailsPart.edtPurchaserEmailEditText.text.toString(),
                                    giftCardRecipientFirstName = giftCardRecipientFirstName,
                                    giftCardRecipientLastName = giftCardRecipientLastName,
                                    giftCardRecipientEmail = binding.cardDetailsPart.edtRecipientEmailEditText.text.toString(),
                                    giftCardAmout = binding.cardDetailsPart.edtAmount.text.toString().toDouble().times(100).toInt(),
                                    giftCardPersonalMessage = binding.cardDetailsPart.edtPersonalMessage.text.toString().ifEmpty { null },
                                    transactionIdOfProcessor =if(isPosKeyBYPASS()) generateTransactionId() else it.createPaymentIntentResponse.firstOrNull()?.hostTransactionReference,
                                    transactionChargeId = it.createPaymentIntentResponse.firstOrNull()?.referenceNo,
                                )
                                buyVirtualCardRequest?.let { item -> giftCardViewModel.buyVirtualGiftCard(item) }
                            }
                            binding.cardDetailsPart.edtPurchaserNameEditText.text?.clear()
                            binding.cardDetailsPart.edtRecipientSurNameEditText.text?.clear()
                            binding.cardDetailsPart.edtPurchaserSurNameEditText.text?.clear()
                            binding.cardDetailsPart.edtRecipientNameEditText.text?.clear()
                            binding.cardDetailsPart.emailEditText.text?.clear()
                            binding.cardDetailsPart.amountEditText.text?.clear()
                            binding.cardDetailsPart.edtRecipientEmailEditText.text?.clear()
                            binding.cardDetailsPart.edtAmount.text?.clear()
                            binding.cardDetailsPart.edtPurchaserEmailEditText.text?.clear()
                            binding.cardDetailsPart.edtPersonalMessage.text?.clear()
                            binding.cardDetailsPart.giftPackagingEditText.text?.clear()
                            binding.cardDetailsPart.amountEditText.text?.clear()
                        }
                        PaymentStatus.Failed -> {
                            binding.constraintGiftCard.isVisible = false
                            binding.pendingPayment.ivPaymentError.setImageResource(R.drawable.ic_error)
                            binding.pendingPayment.dialogHeading.text = resources.getString(R.string.payment_error)
                            binding.pendingPayment.tvDescription.text = it.createPaymentIntentResponse.firstOrNull()?.status
                            binding.pendingPayment.tryAgainMaterialButton.isVisible = true
                        }
                        PaymentStatus.DeclineByHostOrCard -> {
                            binding.constraintGiftCard.isVisible = false
                            binding.pendingPayment.ivPaymentError.setImageResource(R.drawable.ic_error)
                            binding.pendingPayment.dialogHeading.text = resources.getString(R.string.payment_error)
                            binding.pendingPayment.tvDescription.text = it.createPaymentIntentResponse.firstOrNull()?.status
                            binding.pendingPayment.tryAgainMaterialButton.isVisible = true
                        }
                        PaymentStatus.TimeoutOnUserInput -> {
                            binding.constraintGiftCard.isVisible = false
                            binding.pendingPayment.ivPaymentError.setImageResource(R.drawable.ic_error)
                            binding.pendingPayment.dialogHeading.text = resources.getString(R.string.payment_error)
                            binding.pendingPayment.tvDescription.text = it.createPaymentIntentResponse.firstOrNull()?.status
                            binding.pendingPayment.tryAgainMaterialButton.isVisible = true
                        }
                        else -> {
                            binding.pendingPayment.ivPaymentError.setImageResource(R.drawable.ic_error)
                            binding.pendingPayment.dialogHeading.text = resources.getString(R.string.payment_error)
                            binding.pendingPayment.tvDescription.text = it.createPaymentIntentResponse?.firstOrNull()?.status
                            binding.pendingPayment.tryAgainMaterialButton.isVisible = true
                        }
                    }
                }
                else -> {}
            }
        }
    }

    private fun isPosKeyBYPASS() :Boolean {
        if (loggedInUserCache.getLocationInfo()?.poskey == "BYPASS"  && loggedInUserCache.getLocationInfo()?.terminalkey == "BYPASS"){
            return true
        }
        return false
    }

    private fun generateTransactionId(): String {
        val random = Random()
        val transactionId = StringBuilder()

        repeat(6) {
            val digit = random.nextInt(10)
            transactionId.append(digit)
        }

        return transactionId.toString()
    }

    private fun physicalOrVirtualSelection(isPhysical: Boolean) {
        binding.newCardImageview.isSelected = !isPhysical
        binding.newCardSelectLinear.isSelected = !isPhysical
        binding.checkBalanceImageView.isSelected = isPhysical
        binding.newCardTextview.isSelected = !isPhysical
        binding.cardDetailsPart.root.isVisible = !isPhysical
        binding.checkBalanceTextview.isSelected = isPhysical
        binding.newCardSelectLinear.isSelected = !isPhysical
        binding.checkCardBalance.root.isVisible = isPhysical
        binding.checkBalanceSelectLinear.isSelected = isPhysical
        binding.cardBalance.root.isVisible = false
    }

    private fun checkPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(permission), requestCode)
        } else {
            val qrScannerFragment = QrScannerFragment().apply {
                qrData.subscribeAndObserveOnMainThread {
                    dismiss()
                    requireActivity().hideKeyboard()
                    if (isVisible) {
                        if (qrCodeType == Constants.QR_CODE_TYPE_GIFT_CARD) {
                            giftCardViewModel.giftCardQRCode(it)
                        }
                    }
                }.autoDispose()
            }
            qrScannerFragment.show(parentFragmentManager, "")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {

        }
    }


    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] == CAMERA_PERMISSION_REQUEST_CODE) {

        } else {
            Toast.makeText(requireContext(), "Camera Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isPhysicalGiftCardValidate(): Boolean {
        return when {
            binding.cardDetailsPart.giftPackagingEditText.isFieldBlank() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_gift_card_id), Toast.LENGTH_SHORT).show()
                false
            }

            binding.cardDetailsPart.amountEditText.isFieldBlank() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_lastName), Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun isVirtualGiftCardValidate(): Boolean {
        return when {
            binding.cardDetailsPart.edtAmount.isFieldBlank() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_amount), Toast.LENGTH_SHORT).show()
                false
            }
            binding.cardDetailsPart.edtPurchaserNameEditText.isFieldBlank() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_name), Toast.LENGTH_SHORT).show()
                false
            }
            binding.cardDetailsPart.edtPurchaserSurNameEditText.isFieldBlank() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_surname), Toast.LENGTH_SHORT).show()
                false
            }

            binding.cardDetailsPart.edtPurchaserEmailEditText.isNotValidEmail() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_email), Toast.LENGTH_SHORT).show()
                false
            }
            binding.cardDetailsPart.edtRecipientNameEditText.isFieldBlank() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_name), Toast.LENGTH_SHORT).show()
                false
            }
            binding.cardDetailsPart.edtRecipientSurNameEditText.isFieldBlank() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_surname), Toast.LENGTH_SHORT).show()
                false
            }
            binding.cardDetailsPart.edtRecipientEmailEditText.isNotValidEmail() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_email), Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun getColoredSpanned(text: String, color: Int): String {
        return "<font color=$color>$text</font>"
    }

    override fun onResume() {
        super.onResume()
        RxBus.publish(RxEvent.HideShowEditTextMainActivity(false))
    }

}