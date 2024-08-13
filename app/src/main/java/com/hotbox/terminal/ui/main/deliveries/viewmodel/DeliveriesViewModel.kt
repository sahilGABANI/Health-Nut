package com.hotbox.terminal.ui.main.deliveries.viewmodel

import com.hotbox.terminal.api.deliveries.DeliveriesRepository
import com.hotbox.terminal.api.order.model.OrderResponse
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

class DeliveriesViewModel(private val deliveriesRepository: DeliveriesRepository) : BaseViewModel() {

    private lateinit var currentDate: String
    private lateinit var date: String
    private val calendar = Calendar.getInstance()
    private val year = calendar.get(Calendar.YEAR)
    private var month: Int = calendar.get(Calendar.MONTH) + 1
    private val day = calendar.get(Calendar.DAY_OF_MONTH)
    private val deliveriesStateSubject: PublishSubject<DeliveriesViewState> = PublishSubject.create()
    val deliveriesState: Observable<DeliveriesViewState> = deliveriesStateSubject.hide()
    private var orderDataDisposable: Disposable? = null

    fun loadDeliverOrderData(calenderDate: String, orderType: String,orderStatus :String?) {
        currentDate = "$year-$month-$day".toDate("yyyy-MM-dd")?.formatTo("yyyy-MM-dd").toString()
        date = calenderDate.ifEmpty { currentDate }
        orderDataDisposable?.dispose()
        orderDataDisposable = Observable.interval(30, TimeUnit.SECONDS)
            .startWith(0L)
            .flatMap { deliveriesRepository.getDeliveriesOrderData(date, orderType,orderStatus).toObservable() }.doOnSubscribe {
                deliveriesStateSubject.onNext(DeliveriesViewState.LoadingState(true))
            }.doAfterTerminate {
                deliveriesStateSubject.onNext(DeliveriesViewState.LoadingState(false))
            }.subscribeWithErrorParsing<List<OrdersInfo>, HotBoxError>({

                deliveriesStateSubject.onNext(DeliveriesViewState.LoadingState(false))
                deliveriesStateSubject.onNext(DeliveriesViewState.OrderInfoSate(it))
            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        deliveriesStateSubject.onNext(DeliveriesViewState.LoadingState(false))
                        deliveriesStateSubject.onNext(DeliveriesViewState.ErrorMessage(it.errorResponse.safeErrorMessage))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        deliveriesStateSubject.onNext(DeliveriesViewState.LoadingState(false))
                        Timber.e(it.throwable)
                    }
                }
            })
    }

    fun closeObserver() {
        orderDataDisposable?.dispose()
    }
}

sealed class DeliveriesViewState {
    data class ErrorMessage(val errorMessage: String) : DeliveriesViewState()
    data class SuccessMessage(val successMessage: String) : DeliveriesViewState()
    data class LoadingState(val isLoading: Boolean) : DeliveriesViewState()
    data class OrderInfoSate(val orderInfo: List<OrdersInfo>) : DeliveriesViewState()
}