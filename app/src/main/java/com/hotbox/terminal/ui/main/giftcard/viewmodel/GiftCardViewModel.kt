package com.hotbox.terminal.ui.main.giftcard.viewmodel

import com.hotbox.terminal.api.checkout.CheckOutRepository
import com.hotbox.terminal.api.checkout.model.*
import com.hotbox.terminal.api.giftcard.GiftCardRepository
import com.hotbox.terminal.api.giftcard.model.BuyPhysicalCardRequest
import com.hotbox.terminal.api.giftcard.model.BuyVirtualCardRequest
import com.hotbox.terminal.api.stripe.StripeRepository
import com.hotbox.terminal.api.stripe.model.CaptureNewPaymentRequest
import com.hotbox.terminal.api.stripe.model.ResponseItem
import com.hotbox.terminal.base.BaseViewModel
import com.hotbox.terminal.base.extension.subscribeOnIoAndObserveOnMainThread
import com.hotbox.terminal.base.extension.subscribeWithErrorParsing
import com.hotbox.terminal.base.network.model.ErrorResult
import com.hotbox.terminal.base.network.model.HotBoxError
import com.hotbox.terminal.base.network.parseRetrofitException
import com.hotbox.terminal.ui.userstore.checkout.viewmodel.CheckOutState
import com.hotbox.terminal.ui.userstore.viewmodel.UserStoreState
import com.hotbox.terminal.utils.Constants
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class GiftCardViewModel(private val giftCardRepository: GiftCardRepository, private val stripeRepository: StripeRepository) : BaseViewModel() {

    private val giftCardStateSubject: PublishSubject<GiftCardState> = PublishSubject.create()
    val giftCardState: Observable<GiftCardState> = giftCardStateSubject.hide()

    fun giftCardQRCode(data: String) {
        giftCardRepository.giftCardQRCode(data).doOnSubscribe {
            giftCardStateSubject.onNext(GiftCardState.LoadingState(true))
        }.doAfterTerminate {
            giftCardStateSubject.onNext(GiftCardState.LoadingState(false))
        }.subscribeWithErrorParsing<GiftCardResponse, HotBoxError>({
            giftCardStateSubject.onNext(GiftCardState.GiftCardQrResponse(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    giftCardStateSubject.onNext(GiftCardState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    giftCardStateSubject.onNext(
                        GiftCardState.ErrorMessage(
                            it.throwable.parseRetrofitException()?.safeErrorMessage ?: "Invalid Gift Card QR"
                        )
                    )
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun applyGiftCard(cardNumber: String) {
        giftCardRepository.applyGiftCard(cardNumber).subscribeWithErrorParsing<GiftCardData, HotBoxError>({
            giftCardStateSubject.onNext(GiftCardState.GiftCard(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    giftCardStateSubject.onNext(GiftCardState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    giftCardStateSubject.onNext(GiftCardState.ErrorMessage(it.throwable.localizedMessage))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun buyVirtualGiftCard(request: BuyVirtualCardRequest) {
        giftCardRepository.buyVirtualGiftCard(request).subscribeWithErrorParsing<GiftCardData, HotBoxError>({
            giftCardStateSubject.onNext(GiftCardState.BuyVirtualGiftCard(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    giftCardStateSubject.onNext(GiftCardState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    giftCardStateSubject.onNext(GiftCardState.ErrorMessage(it.throwable.localizedMessage))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun buyPhysicalGiftCard(request: BuyPhysicalCardRequest) {
        giftCardRepository.buyPhysicalGiftCard(request).subscribeWithErrorParsing<GiftCardData, HotBoxError>({
            giftCardStateSubject.onNext(GiftCardState.BuyPhysicalGiftCard(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    giftCardStateSubject.onNext(GiftCardState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    giftCardStateSubject.onNext(GiftCardState.ErrorMessage(it.throwable.parseRetrofitException()?.safeErrorMessage ?: ""))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun captureNewPayment(newPaymentRequest: CaptureNewPaymentRequest) {
        stripeRepository.captureNewPayment(newPaymentRequest).subscribeWithErrorParsing<List<ResponseItem>, HotBoxError>({
            giftCardStateSubject.onNext(GiftCardState.CaptureNewPaymentIntent(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    giftCardStateSubject.onNext(GiftCardState.NewPaymentErrorMessage(it.errorResponse.message.toString()))
                }
                is ErrorResult.ErrorThrowable -> {
                    giftCardStateSubject.onNext(GiftCardState.NewPaymentErrorMessage(Constants.CAPTURE_ERROR))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

}

sealed class GiftCardState {
    data class ErrorMessage(val errorMessage: String) : GiftCardState()
    data class NewPaymentErrorMessage(val errorMessage: String) : GiftCardState()
    data class LoadingState(val isLoading: Boolean) : GiftCardState()
    data class GiftCardQrResponse(val data: GiftCardResponse) : GiftCardState()
    data class GiftCard(val data: GiftCardData) : GiftCardState()
    data class BuyVirtualGiftCard(val data: GiftCardData) : GiftCardState()
    data class BuyPhysicalGiftCard(val data: GiftCardData) : GiftCardState()
    data class QrCodeScanError(val errorType: String) : GiftCardState()
    data class CaptureNewPaymentIntent(val createPaymentIntentResponse: List<ResponseItem>?) : GiftCardState()
}
