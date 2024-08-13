package com.hotbox.terminal.ui.main.orderdetail.viewmodel

import com.hotbox.terminal.api.authentication.model.HealthNutUser
import com.hotbox.terminal.api.authentication.model.HotBoxUser
import com.hotbox.terminal.api.order.OrderRepository
import com.hotbox.terminal.api.order.model.*
import com.hotbox.terminal.api.stripe.StripeRepository
import com.hotbox.terminal.api.stripe.model.CaptureNewPaymentRequest
import com.hotbox.terminal.api.stripe.model.ResponseItem
import com.hotbox.terminal.base.BaseViewModel
import com.hotbox.terminal.base.extension.subscribeWithErrorParsing
import com.hotbox.terminal.base.network.model.*
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.json.JSONObject
import retrofit2.adapter.rxjava2.HttpException
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader

class OrderDetailsViewModel(private val orderRepository: OrderRepository, private val stripeRepository: StripeRepository) : BaseViewModel() {
    private val orderDetailsStateSubject: PublishSubject<OrderDetailsViewState> = PublishSubject.create()
    val orderDetailsState: Observable<OrderDetailsViewState> = orderDetailsStateSubject.hide()

    fun loadStatusDetails(orderId: Int) {
        orderRepository.getStatusLogData(orderId)
            .doOnSubscribe {
                orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(true))
            }.doAfterTerminate {
                orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(false))
            }.subscribeWithErrorParsing<StatusLogInfo, HotBoxError>({
                orderDetailsStateSubject.onNext(OrderDetailsViewState.StatusResponse(it))
            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        orderDetailsStateSubject.onNext(OrderDetailsViewState.ErrorMessage(it.errorResponse.safeErrorMessage))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            }).autoDispose()
    }

    fun loadCartGroupDetail(cartGroupId: Int) {
        orderRepository.getCartGroupDetail(cartGroupId)
            .doOnSubscribe {
                orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(true))
            }.doAfterTerminate {
                orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(false))
            }.subscribeWithErrorParsing<StatusLogInfo, HotBoxError>({
                orderDetailsStateSubject.onNext(OrderDetailsViewState.StatusResponse(it))
            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        orderDetailsStateSubject.onNext(OrderDetailsViewState.ErrorMessage(it.errorResponse.safeErrorMessage))
                        orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(false))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            }).autoDispose()
    }

    fun loadUserDetails(userId: Int) {
        orderRepository.getUserDetails(userId)
            .doOnSubscribe {
                orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(true))
            }.doAfterTerminate {
                orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(false))
            }.subscribeWithErrorParsing<HotBoxUser, HotBoxError>({
                orderDetailsStateSubject.onNext(OrderDetailsViewState.CustomerDetails(it))
            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        orderDetailsStateSubject.onNext(OrderDetailsViewState.ErrorMessage(it.errorResponse.safeErrorMessage))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            }).autoDispose()
    }

    fun loadOrderDetailsItem(OrderId: Int) {
        orderRepository.getOrderDetailsData(OrderId)
            .doOnSubscribe {
                orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(true))
            }.doAfterTerminate {
                orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(false))
            }.subscribeWithErrorParsing<OrderDetailsResponse, HotBoxError>({
                orderDetailsStateSubject.onNext(OrderDetailsViewState.OrderDetailItemResponse(it))
            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        orderDetailsStateSubject.onNext(OrderDetailsViewState.ErrorMessage(it.errorResponse.safeErrorMessage))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            }).autoDispose()
    }

    fun refundOrderPayment(OrderId: Int, amount: Int) {
        orderRepository.refundPayment(OrderId, amount)
            .doOnSubscribe {
                orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(true))
            }.doAfterTerminate {
                orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(false))
            }.subscribeWithErrorParsing<HotBoxResponses, HotBoxError>({
                orderDetailsStateSubject.onNext(OrderDetailsViewState.RefundResponse(it.message.toString()))
            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        orderDetailsStateSubject.onNext(OrderDetailsViewState.ErrorMessage(it.errorResponse.safeErrorMessage))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        val message = (it.throwable as HttpException).response()?.errorBody()?.byteStream()
                        val responseString = BufferedReader(InputStreamReader(message)).use { it.readText() }
                        val responseJson = JSONObject(responseString)
                        val messageInfo = responseJson.getString("message")
                        orderDetailsStateSubject.onNext(OrderDetailsViewState.ErrorMessage(messageInfo))
                        Timber.e(it.throwable)
                    }
                }
            }).autoDispose()
    }

    fun updateOrderStatusDetails(orderStatus: String, orderId: Int) {
        orderRepository.updateOrderStatus(orderStatus, orderId).doOnSubscribe {
            orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(true))
        }.doAfterTerminate {
            orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(false))
        }.subscribeWithErrorParsing<UpdatedOrderStatusResponse, HotBoxError>({
            orderDetailsStateSubject.onNext(OrderDetailsViewState.UpdateStatusResponse(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    orderDetailsStateSubject.onNext(OrderDetailsViewState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }


    fun sendReceipt(orderId: Int, type: String, email: String?, phone: String?) {
        orderRepository.sendReceipt(orderId, type, email, phone)
            .doOnSubscribe {
                orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(true))
            }.doAfterTerminate {
                orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(false))
            }.subscribeWithErrorParsing<HotBoxResponse<HealthNutUser>, HotBoxError>({
                orderDetailsStateSubject.onNext(OrderDetailsViewState.SendReceiptSuccessMessage(it.message.toString()))
            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        orderDetailsStateSubject.onNext(OrderDetailsViewState.ErrorMessage(it.errorResponse.message.toString()))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            }).autoDispose()
    }


    fun refundPayment(newPaymentRequest: CaptureNewPaymentRequest) {
        stripeRepository.captureNewPayment(newPaymentRequest).doOnSubscribe {
            orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(true))
        }.doAfterTerminate {
            orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(false))
        }.subscribeWithErrorParsing<List<ResponseItem>, HotBoxError>({
            orderDetailsStateSubject.onNext(OrderDetailsViewState.CaptureNewPaymentIntent(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    orderDetailsStateSubject.onNext(OrderDetailsViewState.ErrorMessage(it.errorResponse.message.toString()))
                }
                is ErrorResult.ErrorThrowable -> {
                    orderDetailsStateSubject.onNext(OrderDetailsViewState.ErrorMessage(it.throwable.localizedMessage))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

}

sealed class OrderDetailsViewState {
    data class ErrorMessage(val errorMessage: String) : OrderDetailsViewState()
    data class SuccessMessage(val successMessage: String) : OrderDetailsViewState()
    data class SendReceiptSuccessMessage(val successMessage: String) : OrderDetailsViewState()
    data class LoadingState(val isLoading: Boolean) : OrderDetailsViewState()
    data class RefundResponse(val message :String) : OrderDetailsViewState()
    data class CaptureNewPaymentIntent(val createPaymentIntentResponse: List<ResponseItem>?) : OrderDetailsViewState()
    data class StatusResponse(val statusLogInfo: StatusLogInfo) : OrderDetailsViewState()
    data class UpdateStatusResponse(val updatedOrderStatusResponse: UpdatedOrderStatusResponse) : OrderDetailsViewState()
    data class OrderDetailItemResponse(val orderDetails: OrderDetailsResponse) : OrderDetailsViewState()
    data class CustomerDetails(val customerDetails: HotBoxUser) : OrderDetailsViewState()
}