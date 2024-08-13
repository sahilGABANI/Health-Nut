package com.hotbox.terminal.ui.main.orderdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.core.view.isVisible
import com.google.gson.Gson
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.order.model.OrderDetailsResponse
import com.hotbox.terminal.api.order.model.RefundDialogStates
import com.hotbox.terminal.api.stripe.model.CaptureNewPaymentRequest
import com.hotbox.terminal.api.stripe.model.PaymentStatus
import com.hotbox.terminal.api.stripe.model.Resource
import com.hotbox.terminal.api.userstore.model.CreateOrderRequest
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseDialogFragment
import com.hotbox.terminal.base.RxBus
import com.hotbox.terminal.base.RxEvent
import com.hotbox.terminal.base.ViewModelFactory
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.FragmentRefundDialogBinding
import com.hotbox.terminal.ui.main.orderdetail.viewmodel.OrderDetailsViewModel
import com.hotbox.terminal.ui.main.orderdetail.viewmodel.OrderDetailsViewState
import com.hotbox.terminal.utils.Constants
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class RefundFragmentDialog : BaseDialogFragment(){

    private var _binding: FragmentRefundDialogBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    lateinit var orderDetails: OrderDetailsResponse
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<OrderDetailsViewModel>
    private lateinit var orderDetailsViewModel: OrderDetailsViewModel

    private val refundDialogSubject: PublishSubject<RefundDialogStates> = PublishSubject.create()
    val refundDialogState: Observable<RefundDialogStates> = refundDialogSubject.hide()


    companion object {
        const val INTENT_CART_GROUP = "Intent Cart Group"
        fun newInstance(orderDetailsInfo: OrderDetailsResponse?): RefundFragmentDialog {
            val args = Bundle()
            val gson = Gson()
            val json: String = gson.toJson(orderDetailsInfo)
            json.let { args.putString(INTENT_CART_GROUP, it) }
            val fragment = RefundFragmentDialog()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        orderDetailsViewModel = getViewModelFromFactory(viewModelFactory)
        setStyle(STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRefundDialogBinding.inflate(inflater, container, false)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.decorView?.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewEvent()
        listenToViewModel()
    }

    private fun listenToViewModel() {
        orderDetailsViewModel.orderDetailsState.subscribeAndObserveOnMainThread {
            when (it) {
                is OrderDetailsViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                    buttonVisibility(false)
                }
                is OrderDetailsViewState.LoadingState -> {
                    buttonVisibility(it.isLoading)
                }

                is OrderDetailsViewState.CaptureNewPaymentIntent -> {
                    when (it.createPaymentIntentResponse?.firstOrNull()?.getPaymentStatus()) {
                        PaymentStatus.Success -> {
                            refundDialogSubject.onNext(RefundDialogStates.GetRefund(orderDetails))
                        }
                        else -> {
                            showToast(it.createPaymentIntentResponse?.firstOrNull()?.status.toString())
                        }
                    }
                }
                is OrderDetailsViewState.RefundResponse -> {
                    refundDialogSubject.onNext(RefundDialogStates.GetRefund(orderDetails))
                }
                else -> {

                }
            }
        }.autoDispose()
    }

    private fun buttonVisibility(loading: Boolean) {
        binding.progressBar.isVisible = loading
        binding.refundButton.isVisible = !loading
    }

    private fun listenToViewEvent() {
        val productsDetails = arguments?.getString(PrintReceiptDialog.INTENT_CART_GROUP)
        val gson = Gson()
        orderDetails = gson.fromJson(productsDetails, OrderDetailsResponse::class.java)
        binding.amountEditText.setText("$")
        binding.transactionIdTextView.text = getString(R.string.transaction_id, orderDetails.transaction?.transactionIdOfProcessor.toString())
        orderDetails.orderTotal?.let {
            binding.amountEditText.setText((it).div(100).toDollar())
            binding.totalAmount.text = getString(R.string.order_total, (it).div(100).toDollar())
        }
        binding.cancelButton.throttleClicks().subscribeAndObserveOnMainThread {
            refundDialogSubject.onNext(RefundDialogStates.DismissedRefundDialog(orderDetails))
        }.autoDispose()
        binding.closeButtonMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            refundDialogSubject.onNext(RefundDialogStates.DismissedRefundDialog(orderDetails))
        }.autoDispose()
        binding.refundButton.throttleClicks().subscribeAndObserveOnMainThread {
            if(isAmountValidate()) {
                requireActivity().hideKeyboard(binding.refundButton.rootView)
                val giftCard = binding.amountEditText.text.toString()
                binding.amountEditText.setText("$")
                val enterGiftCardPrize = (giftCard.removePrefix("$").toDouble().times(100)).toInt()
                orderDetails.id?.let { it1 -> orderDetailsViewModel.refundOrderPayment(it1,enterGiftCardPrize) }
            }
        }.autoDispose()
    }

    private fun isAmountValidate(): Boolean {
        return when {
            binding.amountEditText.text.toString().removePrefix("$").trim().isEmpty() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_amount), Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }
}