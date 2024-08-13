package com.hotbox.terminal.ui.main.order.viewmodel

import com.hotbox.terminal.api.order.OrderRepository
import com.hotbox.terminal.api.order.model.OrdersInfo
import com.hotbox.terminal.base.BaseViewModel
import com.hotbox.terminal.base.extension.subscribeWithErrorParsing
import com.hotbox.terminal.base.network.model.ErrorResult
import com.hotbox.terminal.base.network.model.HotBoxError
import com.hotbox.terminal.helper.formatTo
import com.hotbox.terminal.helper.toDate
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

class OrderViewModel(private val orderRepository: OrderRepository) : BaseViewModel() {
    private lateinit var currentDate: String
    private  var orderType: String?= null
    private var orderStatus: String? = null
    private lateinit var date: String
    private val calendar = Calendar.getInstance()
    private val year = calendar.get(Calendar.YEAR)
    private var month: Int = calendar.get(Calendar.MONTH) + 1
    private val day = calendar.get(Calendar.DAY_OF_MONTH)
    private val orderStateSubject: PublishSubject<OrderViewState> = PublishSubject.create()
    val orderState: Observable<OrderViewState> = orderStateSubject.hide()
    private var orderDataDisposable: Disposable? = null

    fun loadOrderData(calenderDate: String, ordersType: String?, ordersStatus: String?) {
        orderType = ordersType?.ifEmpty { null }
        currentDate = "$year-$month-$day".toDate("yyyy-MM-dd")?.formatTo("yyyy-MM-dd").toString()
        date = calenderDate.ifEmpty { currentDate }
        orderStatus = ordersStatus
        orderDataDisposable?.dispose()
        orderDataDisposable = Observable.interval(30, TimeUnit.SECONDS)
            .startWith(0L)
            .flatMap { orderRepository.getOrderData(date, orderType, orderStatus).toObservable() }.doOnSubscribe{
                orderStateSubject.onNext(OrderViewState.LoadingState(true))
            }.doAfterTerminate {
                orderStateSubject.onNext(OrderViewState.LoadingState(false))
            }
            .subscribeWithErrorParsing<List<OrdersInfo>, HotBoxError>({
                orderStateSubject.onNext(OrderViewState.LoadingState(false))
                orderStateSubject.onNext(OrderViewState.OrderInfoSate(it))
            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        orderStateSubject.onNext(OrderViewState.ErrorMessage(it.errorResponse.safeErrorMessage))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            })
    }

    fun closeObserver() {
        orderDataDisposable?.dispose()
    }
}

sealed class OrderViewState {
    data class ErrorMessage(val errorMessage: String) : OrderViewState()
    data class SuccessMessage(val successMessage: String) : OrderViewState()
    data class LoadingState(val isLoading: Boolean) : OrderViewState()
    data class OrderInfoSate(val orderInfo: List<OrdersInfo>) : OrderViewState()
}