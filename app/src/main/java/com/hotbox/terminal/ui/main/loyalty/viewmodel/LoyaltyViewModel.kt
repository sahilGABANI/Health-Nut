package com.hotbox.terminal.ui.main.loyalty.viewmodel

import com.hotbox.terminal.api.authentication.model.HealthNutUser
import com.hotbox.terminal.api.checkout.model.LoyaltyWithPhoneResponse
import com.hotbox.terminal.api.loyalty.LoyaltyRepository
import com.hotbox.terminal.api.loyalty.model.AddLoyaltyPointRequest
import com.hotbox.terminal.api.loyalty.model.AddLoyaltyPointResponse
import com.hotbox.terminal.api.loyalty.model.OrderLoyaltyInfo
import com.hotbox.terminal.api.order.model.OrderDetailsResponse
import com.hotbox.terminal.base.BaseViewModel
import com.hotbox.terminal.base.extension.subscribeWithErrorParsing
import com.hotbox.terminal.base.network.model.ErrorResult
import com.hotbox.terminal.base.network.model.HotBoxError
import com.hotbox.terminal.base.network.model.HotBoxResponse
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class LoyaltyViewModel(private val loyaltyRepository: LoyaltyRepository) : BaseViewModel() {

    private val loyaltyStateSubject: PublishSubject<LoyaltyState> = PublishSubject.create()
    val loyaltyState: Observable<LoyaltyState> = loyaltyStateSubject.hide()

    fun getPhoneLoyaltyData(data: String) {
        loyaltyRepository.getPhoneLoyaltyData(data).doOnSubscribe {
                loyaltyStateSubject.onNext(LoyaltyState.LoadingState(true))
            }.doAfterTerminate {
                loyaltyStateSubject.onNext(LoyaltyState.LoadingState(false))
            }.subscribeWithErrorParsing<HotBoxResponse<LoyaltyWithPhoneResponse>, HotBoxError>({
                loyaltyStateSubject.onNext(LoyaltyState.PhoneLoyaltyData(it.data))

                loyaltyStateSubject.onNext(LoyaltyState.SuccessMessage(it.message.toString()))
            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        loyaltyStateSubject.onNext(LoyaltyState.ErrorMessage(it.errorResponse.safeErrorMessage))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            }).autoDispose()
    }


    fun getPhoneUserData(data: String) {
        loyaltyRepository.getPhoneLoyaltyData(data).doOnSubscribe {
                loyaltyStateSubject.onNext(LoyaltyState.AddScreenPhoneLoadingState(true))
            }.doAfterTerminate {
                loyaltyStateSubject.onNext(LoyaltyState.AddScreenPhoneLoadingState(false))
            }.subscribeWithErrorParsing<HotBoxResponse<LoyaltyWithPhoneResponse>, HotBoxError>({
                loyaltyStateSubject.onNext(LoyaltyState.PhoneUserData(it.data))
            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        loyaltyStateSubject.onNext(LoyaltyState.ErrorMessage(it.errorResponse.safeErrorMessage))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            }).autoDispose()
    }

    fun loadOrderDetailsItem(OrderId: Long) {
        loyaltyRepository.getOrderDetailsData(OrderId).doOnSubscribe {
                loyaltyStateSubject.onNext(LoyaltyState.OrderLoadingState(true))
            }.doAfterTerminate {
                loyaltyStateSubject.onNext(LoyaltyState.OrderLoadingState(false))
            }.subscribeWithErrorParsing<OrderDetailsResponse, HotBoxError>({
                loyaltyStateSubject.onNext(LoyaltyState.OrderDetailItemResponse(it))
            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        loyaltyStateSubject.onNext(LoyaltyState.ErrorMessage(it.errorResponse.safeErrorMessage))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            }).autoDispose()
    }


    fun getUser(userId: String?) {
        loyaltyRepository.getUserDetails(id = userId).subscribeWithErrorParsing<HealthNutUser, HotBoxError>({
            loyaltyStateSubject.onNext(LoyaltyState.UserDetails(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    loyaltyStateSubject.onNext(LoyaltyState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    loyaltyStateSubject.onNext(LoyaltyState.ErrorMessage(it.throwable.localizedMessage))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun addLoyaltyPoint(request: AddLoyaltyPointRequest) {
        loyaltyRepository.addLoyaltyPoint(request = request).subscribeWithErrorParsing<HotBoxResponse<AddLoyaltyPointResponse>, HotBoxError>({
            it.data?.let {
                loyaltyStateSubject.onNext(LoyaltyState.AddLoyaltyPointInfo(it))
            }
            loyaltyStateSubject.onNext(LoyaltyState.SuccessMessage(it.message.toString()))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    loyaltyStateSubject.onNext(LoyaltyState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun getOrderLoyalty(orderId: Long) {
        loyaltyRepository.getOrderLoyalty(orderId).doOnSubscribe {
                loyaltyStateSubject.onNext(LoyaltyState.OrderLoadingState(true))
            }.doAfterTerminate {
                loyaltyStateSubject.onNext(LoyaltyState.OrderLoadingState(false))
            }.subscribeWithErrorParsing<HotBoxResponse<OrderLoyaltyInfo>, HotBoxError>({
                it.data?.let {
                    loyaltyStateSubject.onNext(LoyaltyState.OrdersLoyaltyInfo(it))
                }
            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        loyaltyStateSubject.onNext(LoyaltyState.ErrorMessage(it.errorResponse.safeErrorMessage))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            }).autoDispose()
    }

}


sealed class LoyaltyState {
    data class ErrorMessage(val errorMessage: String) : LoyaltyState()
    data class SuccessMessage(val successMessage: String) : LoyaltyState()
    data class LoadingState(val isLoading: Boolean) : LoyaltyState()
    data class AddScreenPhoneLoadingState(val isLoading: Boolean) : LoyaltyState()
    data class OrderLoadingState(val isLoading: Boolean) : LoyaltyState()
    data class PhoneLoyaltyData(val data: LoyaltyWithPhoneResponse?) : LoyaltyState()
    data class PhoneUserData(val data: LoyaltyWithPhoneResponse?) : LoyaltyState()
    data class UserDetails(val data: HealthNutUser) : LoyaltyState()
    data class AddLoyaltyPointInfo(val data: AddLoyaltyPointResponse) : LoyaltyState()
    data class OrdersLoyaltyInfo(val data: OrderLoyaltyInfo) : LoyaltyState()
    data class OrderDetailItemResponse(val orderDetails: OrderDetailsResponse) : LoyaltyState()
}
