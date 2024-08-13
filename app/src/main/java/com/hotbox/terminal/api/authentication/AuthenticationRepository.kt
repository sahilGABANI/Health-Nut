package com.hotbox.terminal.api.authentication

import com.hotbox.terminal.api.authentication.model.*
import com.hotbox.terminal.base.network.HotBoxResponseConverter
import io.reactivex.Single

class AuthenticationRepository(
    private val authenticationRetrofitAPI: AuthenticationRetrofitAPI,
    private val loggedInUserCache: LoggedInUserCache
) {

    private val hotBoxResponseConverter: HotBoxResponseConverter = HotBoxResponseConverter()

    fun loginCrew(request: LoginCrewRequest): Single<HealthNutUser> {
        return authenticationRetrofitAPI.loginCrew(request)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
            .flatMap { storeUserToken(it) }
            .flatMap { loginCrew ->
                getUserDetails(loginCrew.userId).flatMap {
                    Single.just(LoggedInUser(loginCrew, it))
                }
            }
            .flatMap {
                storeUserInformation(it)
            }
    }

    fun checkAdminPin(request: LoginCrewRequest): Single<LoginCrewResponse> {
        return authenticationRetrofitAPI.checkAdminPin(request)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }

    }

    fun availableToPrint(request: AvailableToPrintRequest): Single<AvailableToPrintInfo> {
        return authenticationRetrofitAPI.availableToPrint(request)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }

    }

    fun getLocation(serialNumber: String): Single<LocationResponse> {
        return authenticationRetrofitAPI.getLocation(serialNumber).flatMap {
            hotBoxResponseConverter.convertToSingle(it)
        }.doAfterSuccess {
            loggedInUserCache.setLocationInfo(it)
        }
    }

    private fun storeUserToken(loginCrewResponse: LoginCrewResponse): Single<LoginCrewResponse> {
        loginCrewResponse.token?.let { loggedInUserCache.setLoggedInUserToken(it) }
        return Single.just(loginCrewResponse)
    }

    private fun storeUserInformation(loggedInUser: LoggedInUser): Single<HealthNutUser> {
        loggedInUserCache.setLoggedInUser(loggedInUser.crewResponse, loggedInUser.hotBoxUser)
        return Single.just(loggedInUser.hotBoxUser)
    }

    private fun getUserDetails(id: String?): Single<HealthNutUser> {
        return authenticationRetrofitAPI.getUserDetails(id)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }
}