package com.hotbox.terminal.ui.userstore.checkout.viewmodel

import com.hotbox.terminal.api.authentication.model.HealthNutUser
import com.hotbox.terminal.api.checkout.CheckOutRepository
import com.hotbox.terminal.api.checkout.model.*
import com.hotbox.terminal.base.BaseViewModel
import com.hotbox.terminal.base.extension.subscribeOnIoAndObserveOnIoThread
import com.hotbox.terminal.base.extension.subscribeWithErrorParsing
import com.hotbox.terminal.base.network.model.ErrorResult
import com.hotbox.terminal.base.network.model.HotBoxError
import com.hotbox.terminal.base.network.parseRetrofitException
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.json.JSONObject
import retrofit2.adapter.rxjava2.HttpException
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader

class CheckOutViewModel(private val checkOutRepository: CheckOutRepository) : BaseViewModel() {

    private val checkOutStateSubject: PublishSubject<CheckOutState> = PublishSubject.create()
    val checkOutState: Observable<CheckOutState> = checkOutStateSubject.hide()

    fun getQRData(data: String) {
        checkOutRepository.getDataFromQRCode(data).subscribeWithErrorParsing<QRScanResponse, HotBoxError>({
            checkOutStateSubject.onNext(CheckOutState.QrCodeData(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    checkOutStateSubject.onNext(CheckOutState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    checkOutStateSubject.onNext(CheckOutState.ErrorMessage(it.throwable.parseRetrofitException()?.message ?: "Invalid Loyalty QR"))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }
    fun getPhoneLoyaltyData(data: String) {
        checkOutRepository.getPhoneLoyaltyData(data).subscribeWithErrorParsing<LoyaltyWithPhoneResponse, HotBoxError>({
//            checkOutStateSubject.onNext(CheckOutState.QrCodeData(it))
            checkOutStateSubject.onNext(CheckOutState.PhoneLoyaltyData(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    checkOutStateSubject.onNext(CheckOutState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    checkOutStateSubject.onNext(CheckOutState.ErrorMessage(it.throwable.parseRetrofitException()?.message ?: "Invalid Loyalty QR"))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun giftCardQRCode(data: String) {
        checkOutRepository.giftCardQRCode(data).doOnSubscribe {
            checkOutStateSubject.onNext(CheckOutState.LoadingState(true))
        }.doAfterTerminate {
            checkOutStateSubject.onNext(CheckOutState.LoadingState(false))
        }.subscribeWithErrorParsing<GiftCardResponse, HotBoxError>({
            checkOutStateSubject.onNext(CheckOutState.GiftCardQrResponse(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    checkOutStateSubject.onNext(CheckOutState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    checkOutStateSubject.onNext(CheckOutState.ErrorMessage(it.throwable.parseRetrofitException()?.safeErrorMessage ?: "Invalid Gift Card QR"))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun getLoyaltyPointDetails(userId: String?) {
        checkOutRepository.getLoyaltyPoints(userId = userId).subscribeWithErrorParsing<UserLoyaltyPointResponse, HotBoxError>({
            checkOutStateSubject.onNext(CheckOutState.UserLoyaltyPoint(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    checkOutStateSubject.onNext(CheckOutState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    checkOutStateSubject.onNext(CheckOutState.ErrorMessage(it.throwable.localizedMessage))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun getUser(userId: String?) {
        checkOutRepository.getUser(userId = userId).subscribeWithErrorParsing<HealthNutUser, HotBoxError>({
            checkOutStateSubject.onNext(CheckOutState.UserCreditPoint(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    checkOutStateSubject.onNext(CheckOutState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    checkOutStateSubject.onNext(CheckOutState.ErrorMessage(it.throwable.localizedMessage))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun applyGiftCard(cardNumber: String) {
        checkOutRepository.applyGiftCard(cardNumber).subscribeWithErrorParsing<GiftCardData, HotBoxError>({
            checkOutStateSubject.onNext(CheckOutState.GiftCard(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    checkOutStateSubject.onNext(CheckOutState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    checkOutStateSubject.onNext(CheckOutState.ErrorMessage(it.throwable.localizedMessage))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun applyPromocode(request: PromoCodeRequest) {
        checkOutRepository.applyPromocode(request).subscribeWithErrorParsing<PromoCodeResponse, HotBoxError>({
            checkOutStateSubject.onNext(CheckOutState.PromocodeResponse(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    checkOutStateSubject.onNext(CheckOutState.ErrorMessage("Coupon code not available for selected day."))
                }
                is ErrorResult.ErrorThrowable -> {
                    checkOutStateSubject.onNext(CheckOutState.ErrorMessage(it.throwable.parseRetrofitException()?.message ?: "Coupon code not available for selected day."))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun createUser(request: CreateUserRequest) {
        checkOutRepository.createUser(request)
            .subscribeOnIoAndObserveOnIoThread({
                it.data?.let {
                    checkOutStateSubject.onNext(CheckOutState.CreateUserInformation(it))
                }
                checkOutStateSubject.onNext(CheckOutState.ErrorMessage("Loyalty user registration successfully"))
            }, {throwable  ->
                val message = (throwable as HttpException).response()?.errorBody()?.byteStream()
                val responseString = BufferedReader(InputStreamReader(message)).use { it.readText() }
                val responseJson = JSONObject(responseString)
                val messageInfo = responseJson.getString("message")
                checkOutStateSubject.onNext(CheckOutState.ErrorMessage(messageInfo))
                Timber.e(throwable)
            })
    }


}

sealed class CheckOutState {
    data class ErrorMessage(val errorMessage: String) : CheckOutState()
    data class LoadingState(val isLoading: Boolean) : CheckOutState()
    data class QrCodeData(val data: QRScanResponse) : CheckOutState()
    data class PhoneLoyaltyData(val data: LoyaltyWithPhoneResponse) : CheckOutState()
    data class GiftCardQrResponse(val data: GiftCardResponse) : CheckOutState()
    data class UserLoyaltyPoint(val data: UserLoyaltyPointResponse) : CheckOutState()
    data class UserCreditPoint(val data: HealthNutUser) : CheckOutState()
    data class GiftCard(val data: GiftCardData) : CheckOutState()
    data class QrCodeScanError(val errorType: String) : CheckOutState()
    data class PromocodeResponse(val promocode: PromoCodeResponse) : CheckOutState()
    data class CreateUserInformation(val createUserResponse: CreateUserResponse) : CheckOutState()
}