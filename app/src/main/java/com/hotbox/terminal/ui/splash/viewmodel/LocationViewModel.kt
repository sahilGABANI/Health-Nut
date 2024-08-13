package com.hotbox.terminal.ui.splash.viewmodel

import com.hotbox.terminal.api.authentication.AuthenticationRepository
import com.hotbox.terminal.api.authentication.model.Location
import com.hotbox.terminal.api.authentication.model.LocationResponse
import com.hotbox.terminal.base.BaseViewModel
import com.hotbox.terminal.base.extension.retryWithDelay
import com.hotbox.terminal.base.extension.subscribeWithErrorParsing
import com.hotbox.terminal.base.network.model.ErrorResult
import com.hotbox.terminal.base.network.model.HotBoxError
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class LocationViewModel(
    private val authenticationRepository: AuthenticationRepository
) : BaseViewModel() {
    private val locationStateSubject: PublishSubject<LocationViewState> = PublishSubject.create()
    val locationState: Observable<LocationViewState> = locationStateSubject.hide()

    private var locationResponse: LocationResponse? = null

    fun loadLocation(serialNumber: String) {
        //14b0fbb66d3cac64
        authenticationRepository.getLocation(serialNumber)
            .doOnSubscribe {
                locationStateSubject.onNext(LocationViewState.LoadingState(true))
                locationStateSubject.onNext(LocationViewState.StartButtonState(false))
            }
            .doAfterTerminate {
                locationStateSubject.onNext(LocationViewState.LoadingState(false))
            }
            .doOnError {
                locationStateSubject.onNext(LocationViewState.ErrorMessage("No Location Set"))
            }
            .retryWithDelay(15, 15000)
            .subscribeWithErrorParsing<LocationResponse, HotBoxError>({
                locationResponse = it
                locationStateSubject.onNext(LocationViewState.LocationsData(it.location))
                locationStateSubject.onNext(LocationViewState.StartButtonState(true))
            }, {
                locationStateSubject.onNext(LocationViewState.StartButtonState(false))
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        locationStateSubject.onNext(LocationViewState.ErrorMessage(it.errorResponse.safeErrorMessage))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            }).autoDispose()

    }

    fun clickOnStartButton() {
        locationResponse?.let {
            locationStateSubject.onNext(LocationViewState.OpenLoginScreen(it))
        } ?: run {
            locationStateSubject.onNext(LocationViewState.ErrorMessage("No location found"))
        }
    }

    fun clear() {
        clearCompositeDisposable()
    }
}

sealed class LocationViewState {
    data class ErrorMessage(val errorMessage: String) : LocationViewState()
    data class SuccessMessage(val successMessage: String) : LocationViewState()
    data class LoadingState(val isLoading: Boolean) : LocationViewState()
    data class StartButtonState(val isVisible: Boolean) : LocationViewState()
    data class LocationsData(val locationResponse: Location?) : LocationViewState()
    data class OpenLoginScreen(val locationResponse: LocationResponse) : LocationViewState()
}