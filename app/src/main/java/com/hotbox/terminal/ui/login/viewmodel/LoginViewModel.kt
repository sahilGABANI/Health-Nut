package com.hotbox.terminal.ui.login.viewmodel

import com.hotbox.terminal.api.authentication.AuthenticationRepository
import com.hotbox.terminal.api.authentication.model.*
import com.hotbox.terminal.base.BaseViewModel
import com.hotbox.terminal.base.extension.subscribeWithErrorParsing
import com.hotbox.terminal.base.network.model.ErrorResult
import com.hotbox.terminal.base.network.model.HotBoxError
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class LoginViewModel(private val authenticationRepository: AuthenticationRepository) :
    BaseViewModel() {

    private val loginStateSubject: PublishSubject<LoginViewState> = PublishSubject.create()
    val loginState: Observable<LoginViewState> = loginStateSubject.hide()

    fun loginCrew(request: LoginCrewRequest) {
        authenticationRepository.loginCrew(request)
            .doOnSubscribe {
                loginStateSubject.onNext(LoginViewState.LoadingState(true))
            }
            .doAfterTerminate {
                loginStateSubject.onNext(LoginViewState.LoadingState(false))
            }.subscribeWithErrorParsing<HealthNutUser, HotBoxError>({
                loginStateSubject.onNext(LoginViewState.LoginSuccess)
            }, {
                when(it) {
                    is ErrorResult.ErrorMessage -> {
                        loginStateSubject.onNext(LoginViewState.ErrorMessage(it.errorResponse.safeErrorMessage))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            }).autoDispose()
    }

    fun checkAdminPin(request: LoginCrewRequest) {
        authenticationRepository.checkAdminPin(request)
            .doOnSubscribe {
                loginStateSubject.onNext(LoginViewState.LoadingState(true))
            }
            .doAfterTerminate {
                loginStateSubject.onNext(LoginViewState.LoadingState(false))
            }.subscribeWithErrorParsing<LoginCrewResponse, HotBoxError>({
                loginStateSubject.onNext(LoginViewState.CheckAdminPin(it))
            }, {
                when(it) {
                    is ErrorResult.ErrorMessage -> {
                        loginStateSubject.onNext(LoginViewState.ErrorMessage(it.errorResponse.safeErrorMessage))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            }).autoDispose()
    }
    fun availableToPrint(request: AvailableToPrintRequest) {
        authenticationRepository.availableToPrint(request)
            .doOnSubscribe {
                loginStateSubject.onNext(LoginViewState.LoadingState(true))
            }
            .doAfterTerminate {
                loginStateSubject.onNext(LoginViewState.LoadingState(false))
            }.subscribeWithErrorParsing<AvailableToPrintInfo, HotBoxError>({
                loginStateSubject.onNext(LoginViewState.AvailableToPrint(it))
            }, {
                when(it) {
                    is ErrorResult.ErrorMessage -> {
                        loginStateSubject.onNext(LoginViewState.ErrorMessage(it.errorResponse.safeErrorMessage))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            }).autoDispose()
    }
}

sealed class LoginViewState {
    data class ErrorMessage(val errorMessage: String) : LoginViewState()
    data class SuccessMessage(val successMessage: String) : LoginViewState()
    data class LoadingState(val isLoading: Boolean) : LoginViewState()
    data class CheckAdminPin(val loginResponse: LoginCrewResponse) : LoginViewState()
    data class AvailableToPrint(val loginResponse: AvailableToPrintInfo) : LoginViewState()
    object LoginSuccess : LoginViewState()
}