package com.hotbox.terminal.ui.main.viewmodel

import com.hotbox.terminal.api.clockinout.ClockInOutRepository
import com.hotbox.terminal.api.clockinout.model.ClockInOutHistoryResponse
import com.hotbox.terminal.api.clockinout.model.ClockType
import com.hotbox.terminal.api.clockinout.model.SubmitTimeRequest
import com.hotbox.terminal.api.clockinout.model.TimeResponse
import com.hotbox.terminal.api.store.StoreRepository
import com.hotbox.terminal.base.BaseViewModel
import com.hotbox.terminal.base.extension.subscribeWithErrorParsing
import com.hotbox.terminal.base.network.model.ErrorResult
import com.hotbox.terminal.base.network.model.HotBoxCommonResponse
import com.hotbox.terminal.base.network.model.HotBoxError
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class ClockInOutViewModel(
    private val clockInOutRepository: ClockInOutRepository,
    private val storeRepository: StoreRepository
) : BaseViewModel() {
    private val clockInOutStateSubject: PublishSubject<ClockInOutState> = PublishSubject.create()
    val clockInOutState: Observable<ClockInOutState> = clockInOutStateSubject.hide()

    fun submitTime(submitTimeRequest: SubmitTimeRequest) {
        clockInOutRepository.submitTime(submitTimeRequest)
            .doOnSubscribe {
                clockInOutStateSubject.onNext(ClockInOutState.LoadingState(true))
            }
            .doAfterTerminate {
                clockInOutStateSubject.onNext(ClockInOutState.LoadingState(false))
            }.subscribeWithErrorParsing<HotBoxCommonResponse, HotBoxError>({
                it.data?.let { message ->
                    clockInOutStateSubject.onNext(ClockInOutState.SuccessMessage(message))
                }
            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        clockInOutStateSubject.onNext(ClockInOutState.ErrorMessage(it.errorResponse.safeErrorMessage))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            }).autoDispose()
    }

    fun getCurrentTime(userId: Int) {
        clockInOutRepository.getCurrentTime(userId)
            .doOnSubscribe {
                clockInOutStateSubject.onNext(ClockInOutState.LoadingState(true))
            }
            .doAfterTerminate {
                clockInOutStateSubject.onNext(ClockInOutState.LoadingState(false))
            }.subscribeWithErrorParsing<TimeResponse, HotBoxError>({
                clockInOutStateSubject.onNext(ClockInOutState.CurrentTimeStatus(it))
            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        clockInOutStateSubject.onNext(
                            ClockInOutState.CurrentTimeStatus(
                                TimeResponse(0, null, ClockType.ClockIn.type, null)
                            )
                        )
                    }
                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            }).autoDispose()
    }

    fun loadClockInOutData(userId: Int, month: Int, year: Int) {
        clockInOutRepository.clockInOutData(userId, month, year).doOnSubscribe {
            clockInOutStateSubject.onNext(ClockInOutState.LoadingState(true))
        }.doAfterTerminate {
            clockInOutStateSubject.onNext(ClockInOutState.LoadingState(false))
        }.subscribeWithErrorParsing<ClockInOutHistoryResponse, HotBoxError>({
            clockInOutStateSubject.onNext(ClockInOutState.LoadClockInOutResponse(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    clockInOutStateSubject.onNext(ClockInOutState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun loadCurrentStoreLocation() {
        storeRepository.getCurrentStoreLocation()
            .subscribeWithErrorParsing<String, HotBoxError>({
                clockInOutStateSubject.onNext(ClockInOutState.StoreLocation(it))
            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        clockInOutStateSubject.onNext(ClockInOutState.ErrorMessage(it.errorResponse.safeErrorMessage))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            }).autoDispose()
    }
}

sealed class ClockInOutState {
    data class ErrorMessage(val errorMessage: String) : ClockInOutState()
    data class SuccessMessage(val successMessage: String) : ClockInOutState()
    data class CurrentTimeStatus(val currentTimeResponse: TimeResponse) : ClockInOutState()
    data class LoadingState(val isLoading: Boolean) : ClockInOutState()
    data class LoadClockInOutResponse(val clockInOutDetailsInfo: ClockInOutHistoryResponse) : ClockInOutState()
    data class StoreLocation(val fullAddress: String) : ClockInOutState()
}
