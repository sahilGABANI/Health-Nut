package com.hotbox.terminal.ui.userstore.viewmodel

import com.google.gson.Gson
import com.hotbox.terminal.api.authentication.AuthenticationRepository
import com.hotbox.terminal.api.authentication.model.HealthNutUser
import com.hotbox.terminal.api.authentication.model.LocationResponse
import com.hotbox.terminal.api.checkout.CheckOutRepository
import com.hotbox.terminal.api.checkout.model.LoyaltyWithPhoneResponse
import com.hotbox.terminal.api.checkout.model.QRScanResponse
import com.hotbox.terminal.api.checkout.model.UserLoyaltyPointResponse
import com.hotbox.terminal.api.menu.model.MenuListInfo
import com.hotbox.terminal.api.menu.model.ProductsItem
import com.hotbox.terminal.api.order.model.UpdatedOrderStatusResponse
import com.hotbox.terminal.api.store.StoreRepository
import com.hotbox.terminal.api.store.model.StoreResponse
import com.hotbox.terminal.api.stripe.StripeRepository
import com.hotbox.terminal.api.userstore.UserStoreRepository
import com.hotbox.terminal.api.userstore.model.*
import com.hotbox.terminal.base.BaseViewModel
import com.hotbox.terminal.base.extension.getMessage
import com.hotbox.terminal.base.extension.subscribeWithErrorParsing
import com.hotbox.terminal.base.network.model.*
import com.hotbox.terminal.base.network.parseRetrofitException
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import retrofit2.adapter.rxjava2.HttpException
import timber.log.Timber
import java.util.concurrent.TimeUnit

class UserStoreViewModel(
    private val userStoreRepository: UserStoreRepository,
    private val stripeRepository: StripeRepository,
    private val storeRepository: StoreRepository,
    private val authenticationRepository: AuthenticationRepository,
    private val checkOutRepository: CheckOutRepository
) : BaseViewModel() {

    private val userStoreStateSubject: PublishSubject<UserStoreState> = PublishSubject.create()
    val userStoreState: Observable<UserStoreState> = userStoreStateSubject.hide()

    fun getMenuProductByLocation() {
        userStoreRepository.getMenuProductByLocation().doOnSubscribe {
            userStoreStateSubject.onNext(UserStoreState.UserStoreLoadingState(true))
        }.doAfterTerminate {
            userStoreStateSubject.onNext(UserStoreState.UserStoreLoadingState(false))
        }.subscribeWithErrorParsing<MenuListInfo, HotBoxError>({
            userStoreStateSubject.onNext(UserStoreState.MenuInfo(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun getProductDetails(productId: String?) {
        userStoreRepository.getMenuProductByLocation(productId).doOnSubscribe {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(true))
        }.doAfterTerminate {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
        }.subscribeWithErrorParsing<ProductsItem, HotBoxError>({
            userStoreStateSubject.onNext(UserStoreState.SubProductState(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun addToCartProduct(request: AddToCartRequest) {
        userStoreRepository.addToCartProduct(request).doOnSubscribe {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(true))
        }.doAfterTerminate {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
        }.subscribeWithErrorParsing<AddToCartResponse, HotBoxError>({
            userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
            userStoreStateSubject.onNext(UserStoreState.AddToCartProductResponse(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun redeemCartItem(request: UpdateMenuItemQuantity, point: Int? = 0) {
        userStoreRepository.redeemCartItem(request, point).doOnSubscribe {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(true))
        }.doAfterTerminate {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
        }.subscribeWithErrorParsing<AddToCartResponse, HotBoxError>({
            userStoreStateSubject.onNext(UserStoreState.RedeemProduct(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun getCartDetails(cartGroupId: Int) {
        Observable.interval(1, TimeUnit.MINUTES).startWith(0L).flatMap { userStoreRepository.getCartDetails(cartGroupId).toObservable() }
            .doOnSubscribe {
                userStoreStateSubject.onNext(UserStoreState.LoadingState(true))
            }.doAfterTerminate {
                userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
            }.subscribeWithErrorParsing<CartInfoDetails, HotBoxError>({
                userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
                userStoreStateSubject.onNext(UserStoreState.CartDetailsInfo(it))
            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.throwable.message.toString()))
                        Timber.e(it.throwable)
                    }
                }
            }).autoDispose()
    }

    fun updateMenuItemQuantity(request: UpdateMenuItemQuantity, loayltyPoint: Int? = 0) {
        userStoreRepository.updateMenuItemQuantity(request, loayltyPoint).doOnSubscribe {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(true))
        }.doAfterTerminate {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
        }.subscribeWithErrorParsing<UpdateCartResponse, HotBoxError>({
            userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
            userStoreStateSubject.onNext(UserStoreState.UpdatedCartInfo(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun deleteCartItem(cartId: Int, point: Int? = 0) {
        userStoreRepository.deleteCartItem(cartId, point).doOnSubscribe {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(true))
        }.doAfterTerminate {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
        }.subscribeWithErrorParsing<DeleteCartItemRequest, HotBoxError>({
            userStoreStateSubject.onNext(UserStoreState.DeletedCartItem(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.message.toString()))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun createOrder(request: CreateOrderRequest) {
        userStoreRepository.createOrder(request).doOnSubscribe {
            userStoreStateSubject.onNext(UserStoreState.CreateOrderLoadingState(true))
        }.doAfterTerminate {
            userStoreStateSubject.onNext(UserStoreState.CreateOrderLoadingState(false))
        }.subscribeWithErrorParsing<CreateOrderResponse, HotBoxError>({
            Timber.tag("OkHttpClient").i("Create Pos Order Response : ${Gson().toJson(it)}")
            userStoreStateSubject.onNext(UserStoreState.CreatePosOrder(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreStateSubject.onNext(UserStoreState.CreateOrderErrorMessage(it.errorResponse.message.toString()))
                }
                is ErrorResult.ErrorThrowable -> {
                    userStoreStateSubject.onNext(UserStoreState.CreateOrderErrorMessage("Order Creation Failed : ${it.throwable.message}"))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }


    fun getOrderPromisedTime() {
        userStoreRepository.getOrderPromisedTime().doOnSubscribe {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(true))
        }.doAfterTerminate {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
        }.subscribeWithErrorParsing<GetPromisedTime, HotBoxError>({
            userStoreStateSubject.onNext(UserStoreState.GetOrderPromisedTime(it))
        }, {

            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.message.toString()))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun loadCurrentStoreResponse() {
        storeRepository.getCurrentStoreInformation().subscribeWithErrorParsing<StoreResponse, HotBoxError>({
            userStoreStateSubject.onNext(UserStoreState.StoreResponses(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }


    fun loadLocation(serialNumber: String) {
        Observable.interval(1, TimeUnit.MINUTES).startWith(0L).flatMap { authenticationRepository.getLocation(serialNumber).toObservable() }
            .subscribeWithErrorParsing<LocationResponse, HotBoxError>({

            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            }).autoDispose()
    }

    fun compProduct(request: CompProductRequest) {
        userStoreRepository.compProduct(request).doOnSubscribe {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(true))
        }.doAfterTerminate {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
        }.subscribeWithErrorParsing<AddToCartResponse, HotBoxError>({
            userStoreStateSubject.onNext(UserStoreState.CompProductProductResponse(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun employeeDiscount(orderSubTotal: Double) {
        userStoreRepository.employeeDiscount(orderSubTotal.toInt()).subscribeWithErrorParsing<EmployeeDiscountResponse, HotBoxError>({
            userStoreStateSubject.onNext(UserStoreState.DiscountPrize(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun getLoyaltyPointDetails(userId: Int?) {
        userStoreRepository.getLoyaltyPoints(userId = userId).subscribeWithErrorParsing<UserLoyaltyPointResponse, HotBoxError>({
            userStoreStateSubject.onNext(UserStoreState.UserLoyaltyPoint(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.throwable.localizedMessage))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun getQRData(data: String) {
        userStoreRepository.getDataFromQRCode(data).subscribeWithErrorParsing<QRScanResponse, HotBoxError>({
            userStoreStateSubject.onNext(UserStoreState.QrCodeData(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.throwable.parseRetrofitException()?.message ?: "Invalid Loyalty QR"))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun getUser(userId: String?) {
        userStoreRepository.getUser(userId = userId).subscribeWithErrorParsing<HealthNutUser, HotBoxError>({
            userStoreStateSubject.onNext(UserStoreState.UserCreditPoint(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.throwable.localizedMessage))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun getPhoneLoyaltyData(data: String) {
        userStoreRepository.getPhoneLoyaltyData(data).subscribeWithErrorParsing<LoyaltyWithPhoneResponse, HotBoxError>({
//            checkOutStateSubject.onNext(CheckOutState.QrCodeData(it))
            userStoreStateSubject.onNext(UserStoreState.PhoneLoyaltyData(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    val message = (it.throwable as HttpException).response()?.errorBody()?.byteStream()
                    message?.getMessage()?.let {
                        userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it))
                    }
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun getLoyaltyPoint(): Int {
        return userStoreRepository.getCurrentLoyaltyPoint()
    }

    fun sendReceipt(orderId: Int, type: String, email: String?, phone: String?) {
        return userStoreRepository.sendReceipt(orderId, type, email, phone)
            .doOnSubscribe {
                userStoreStateSubject.onNext(UserStoreState.LoadingState(true))
            }.doAfterTerminate {
                userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
            }.subscribeWithErrorParsing<HotBoxResponses, HotBoxError>({
                userStoreStateSubject.onNext(UserStoreState.SendReceiptSuccessMessage(it.message.toString()))
            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.message.toString()))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            }).autoDispose()
    }

    fun updateOrderStatusDetails(orderStatus: String, orderId: Int) {
        userStoreRepository.updateOrderStatus(orderStatus, orderId).doOnSubscribe {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(true))
        }.doAfterTerminate {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
        }.subscribeWithErrorParsing<UpdatedOrderStatusResponse, HotBoxError>({
            Timber.tag("TAG").e(it.toString())
            userStoreStateSubject.onNext(UserStoreState.UpdateStatusResponse(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }

                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        })
    }

}

sealed class UserStoreState {
    data class ErrorMessage(val errorMessage: String) : UserStoreState()
    data class CreateOrderErrorMessage(val errorMessage: String) : UserStoreState()
    data class SuccessMessage(val successMessage: String) : UserStoreState()
    data class SendReceiptSuccessMessage(val successMessage: String) : UserStoreState()
    data class LoadingState(val isLoading: Boolean) : UserStoreState()
    data class UserStoreLoadingState(val isLoading: Boolean) : UserStoreState()
    data class CreateOrderLoadingState(val isLoading: Boolean) : UserStoreState()
    data class SubProductState(val productsItem: ProductsItem) : UserStoreState()
    data class CartDetailsInfo(val cartInfo: CartInfoDetails) : UserStoreState()
    data class UpdatedCartInfo(val cartInfo: UpdateCartResponse?) : UserStoreState()
    data class DeletedCartItem(val cartInfo: DeleteCartItemRequest?) : UserStoreState()
    data class CreatePosOrder(val cartInfo: CreateOrderResponse?) : UserStoreState()
    data class AddToCartProductResponse(val addToCartResponse: AddToCartResponse) : UserStoreState()
    data class RedeemProduct(val addToCartResponse: AddToCartResponse) : UserStoreState()
    data class CompProductProductResponse(val addToCartResponse: AddToCartResponse) : UserStoreState()
    data class DiscountPrize(val employeeDiscountResponse: EmployeeDiscountResponse) : UserStoreState()
    data class MenuInfo(val menuListInfo: MenuListInfo) : UserStoreState()
    data class QrCodeData(val data: QRScanResponse) : UserStoreState()
    data class ConnectionTokenResponse(val boolean: Boolean) : UserStoreState()
    data class GetOrderPromisedTime(val getPromisedTime: GetPromisedTime) : UserStoreState()
    data class StoreResponses(val storeResponse: StoreResponse) : UserStoreState()
    data class UserLoyaltyPoint(val data: UserLoyaltyPointResponse) : UserStoreState()
    data class PhoneLoyaltyData(val data: LoyaltyWithPhoneResponse) : UserStoreState()
    data class UserCreditPoint(val data: HealthNutUser) : UserStoreState()
    data class QrCodeScanError(val errorType: String) : UserStoreState()
    data class UpdateStatusResponse(val updatedOrderStatusResponse: UpdatedOrderStatusResponse) : UserStoreState()
}