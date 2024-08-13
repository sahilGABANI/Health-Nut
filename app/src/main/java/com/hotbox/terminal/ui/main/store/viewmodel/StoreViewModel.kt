package com.hotbox.terminal.ui.main.store.viewmodel

import com.hotbox.terminal.api.authentication.model.AvailableToPrintRequest
import com.hotbox.terminal.api.order.OrderRepository
import com.hotbox.terminal.api.order.model.GetPrintQueueItem
import com.hotbox.terminal.api.order.model.OrderDetailsResponse
import com.hotbox.terminal.api.order.model.OrdersInfo
import com.hotbox.terminal.api.order.model.UpdatedOrderStatusResponse
import com.hotbox.terminal.api.store.StoreRepository
import com.hotbox.terminal.api.store.model.BufferResponse
import com.hotbox.terminal.api.store.model.EmployeeInfo
import com.hotbox.terminal.api.store.model.StoreResponse
import com.hotbox.terminal.api.store.model.StoreShiftTime
import com.hotbox.terminal.api.store.model.UpdatePrintStatusRequest
import com.hotbox.terminal.base.BaseViewModel
import com.hotbox.terminal.base.extension.subscribeOnIoAndObserveOnMainThread
import com.hotbox.terminal.base.extension.subscribeWithErrorParsing
import com.hotbox.terminal.base.network.model.ErrorResult
import com.hotbox.terminal.base.network.model.HotBoxError
import com.hotbox.terminal.base.network.model.HotBoxResponses
import com.hotbox.terminal.utils.Constants
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class StoreViewModel(
    private val storeRepository: StoreRepository, private val orderRepository: OrderRepository
) : BaseViewModel() {

    private val storeStateSubject: PublishSubject<StoreState> = PublishSubject.create()
    val storeState: Observable<StoreState> = storeStateSubject.hide()
    private var orderDataDisposable: Disposable? = null
    private var printDisposable: Disposable? = null

    fun loadCurrentStoreResponse() {
        storeRepository.getCurrentStoreInformation().doOnSubscribe {
            storeStateSubject.onNext(StoreState.LoadingState(true))
        }.doAfterTerminate {
            storeStateSubject.onNext(StoreState.LoadingState(false))
        }.subscribeWithErrorParsing<StoreResponse, HotBoxError>({
            storeStateSubject.onNext(StoreState.StoreResponses(it))
            storeStateSubject.onNext(StoreState.LoadStoreShiftTime(it.getStoreShiftTime()))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    storeStateSubject.onNext(StoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }

                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun loadBufferTIme() {
        storeRepository.getBufferInformation().doOnSubscribe {
            storeStateSubject.onNext(StoreState.LoadingState(true))
        }.doAfterTerminate {
            storeStateSubject.onNext(StoreState.LoadingState(false))
        }.subscribeWithErrorParsing<BufferResponse, HotBoxError>({
            storeStateSubject.onNext(StoreState.BufferResponses(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    storeStateSubject.onNext(StoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }

                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun updateBufferTimeForPickUpOrDelivery(
        isBufferTimePlush: Boolean,
        isPickUpBufferTime: Boolean
    ) {
        storeRepository.updateBufferTimeForPickUpOrDelivery(isBufferTimePlush, isPickUpBufferTime)
            .doOnSubscribe {
                storeStateSubject.onNext(StoreState.LoadingState(true))
            }.doAfterTerminate {
            storeStateSubject.onNext(StoreState.LoadingState(true))
        }.subscribeWithErrorParsing<BufferResponse, HotBoxError>({
//            storeStateSubject.onNext(StoreState.BufferResponses(it))
            loadBufferTIme()
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    storeStateSubject.onNext(StoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }

                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun loadOrderData(random: Int) {
        orderDataDisposable?.dispose()
        orderDataDisposable = Observable.interval(random.toLong(), TimeUnit.SECONDS).startWith(0L)
            .flatMap { orderRepository.getOrderData("", Constants.CHECK_ALL, null).toObservable() }
            .doOnError {
                arrayListOf<OrdersInfo>()
            }
            .subscribeWithErrorParsing<List<OrdersInfo>, HotBoxError>({
                storeStateSubject.onNext(StoreState.OrderInfoSate(it))
            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        storeStateSubject.onNext(StoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                    }

                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            })
    }

    fun getPrintQueue(serialNumber: String) {
        printDisposable?.dispose()
        printDisposable = Observable.interval(10, TimeUnit.SECONDS).startWith(0L)
            .subscribeOnIoAndObserveOnMainThread({
                orderRepository.getPrintQueue(serialNumber)
                    .subscribeWithErrorParsing<List<GetPrintQueueItem>, HotBoxError>({
                        storeStateSubject.onNext(StoreState.GetPrintQueue(it))
                    }, {
                        Timber.e(it.toString())
                    })
            }, {

            })
    }

    fun updateOrderStatusDetails(orderStatus: String, orderId: Int) {
        orderRepository.updateOrderStatus(orderStatus, orderId).doOnSubscribe {
            storeStateSubject.onNext(StoreState.LoadingState(true))
        }.doAfterTerminate {
            storeStateSubject.onNext(StoreState.LoadingState(false))
        }.subscribeWithErrorParsing<UpdatedOrderStatusResponse, HotBoxError>({
            Timber.tag("TAG").e(it.toString())
            if (orderStatus == Constants.ORDER_STATUS_RECEIVE) {
//                loadOrderDetailsItem(orderId)
            }
            storeStateSubject.onNext(StoreState.UpdateStatusResponse(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    storeStateSubject.onNext(StoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }

                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun getEmployee() {
        storeRepository.getEmployee().doOnSubscribe {
            storeStateSubject.onNext(StoreState.LoadingState(true))
        }.doAfterTerminate {
            storeStateSubject.onNext(StoreState.LoadingState(false))
        }.subscribeWithErrorParsing<EmployeeInfo, HotBoxError>({
            storeStateSubject.onNext(StoreState.EmployeesInfo(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    storeStateSubject.onNext(StoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }

                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }


    fun unavailableToPrint(request: AvailableToPrintRequest) {
        storeRepository.unavailableToPrint(request).doOnSubscribe {
            storeStateSubject.onNext(StoreState.LoadingState(true))
        }.doAfterTerminate {
            storeStateSubject.onNext(StoreState.LoadingState(false))
        }.subscribeWithErrorParsing<HotBoxResponses, HotBoxError>({
            storeStateSubject.onNext(StoreState.UnavailableToPrintInfo(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    storeStateSubject.onNext(StoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }

                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        })
    }

    fun updatePrintStatus(request: UpdatePrintStatusRequest) {
        storeRepository.updatePrintStatus(request).doOnSubscribe {
            storeStateSubject.onNext(StoreState.LoadingState(true))
        }.doAfterTerminate {
            storeStateSubject.onNext(StoreState.LoadingState(false))
        }.subscribeWithErrorParsing<HotBoxResponses, HotBoxError>({
            storeStateSubject.onNext(StoreState.UnavailableToPrintInfo(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    storeStateSubject.onNext(StoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }

                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun loadOrderDetailsItem(OrderId: Int) {
        orderRepository.getOrderDetailsData(OrderId).doOnSubscribe {
            storeStateSubject.onNext(StoreState.LoadingState(true))
        }.doAfterTerminate {
            storeStateSubject.onNext(StoreState.LoadingState(false))
        }.subscribeWithErrorParsing<OrderDetailsResponse, HotBoxError>({
            Timber.tag("TAG").e(it.toString())
            storeStateSubject.onNext(StoreState.OrderDetailItemResponse(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    storeStateSubject.onNext(StoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }

                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun closeObserver() {
        orderDataDisposable?.dispose()
        printDisposable?.dispose()
    }
}

sealed class StoreState {
    data class ErrorMessage(val errorMessage: String) : StoreState()
    data class SuccessMessage(val successMessage: String) : StoreState()
    data class LoadingState(val isLoading: Boolean) : StoreState()
    data class StoreResponses(val storeResponse: StoreResponse) : StoreState()
    data class LoadStoreShiftTime(val listOfShiftTime: List<StoreShiftTime>) : StoreState()
    data class BufferResponses(val bufferResponse: BufferResponse) : StoreState()
    data class OrderInfoSate(val orderInfo: List<OrdersInfo>) : StoreState()
    data class EmployeesInfo(val employeesInfo: EmployeeInfo) : StoreState()
    data class OrderDetailItemResponse(val orderDetails: OrderDetailsResponse) : StoreState()
    data class GetPrintQueue(val getPrintQueueInfo: List<GetPrintQueueItem>) : StoreState()
    data class UnavailableToPrintInfo(val availableToPrintInfo: HotBoxResponses) : StoreState()
    data class UpdateStatusResponse(val updatedOrderStatusResponse: UpdatedOrderStatusResponse) :
        StoreState()
}