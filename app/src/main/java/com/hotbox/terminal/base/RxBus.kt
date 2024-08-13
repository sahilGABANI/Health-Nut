package com.hotbox.terminal.base

import com.hotbox.terminal.api.userstore.model.CreateOrderResponse
import com.hotbox.terminal.api.userstore.model.OrderPrice
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

object RxBus {

    private val publisher = PublishSubject.create<Any>()

    fun publish(event: Any) {
        publisher.onNext(event)
    }

    fun <T> listen(eventType: Class<T>): Observable<T> = publisher.ofType(eventType)
}

class RxEvent {
    data class EventOrderCountListen(val count: Int)
    data class EventDeliveryCountListen(val count: Int)
    data class EventCartGroupIdListen(val cartGroupId: Int)
    data class EventPaymentButtonEnabled(val enable :Boolean)
    data class EventCreateOrderMaterialButton(val enable :Boolean)
    object EventDismissLoyaltyRegistrationSuccess
    object EventGotoStartButton
    data class EventTotalPayment(val orderPrice: OrderPrice)
    data class EventTotalCheckOut(val orderPrice: OrderPrice)
    data class HideShowEditTextMainActivity(val isShow: Boolean)
    object EventCheckValidation
    object EventValidation
    data class EventGoToPaymentScreen(val data :Boolean)
    data class EventGoToBack(val data :Boolean)
    data class AddGiftCart(val giftCardAmount: Double,val giftCardId :String)
    data class AddPromoCode(val promocodeAmount: Double)
    data class AddCredit(val credit: Double)
    data class AddEmployeeDiscount(val discount: Double)
    data class AddAdjustmentDiscount(val discount: Double)
    data class RemoveGiftCart(val giftCard: Boolean)
    data class RemoveCredit(val credit: Boolean)
    data class RemovePromoCode(val giftCard: Boolean)
    data class PassPromocodeAndGiftCard(val couponCodeId: Int,val giftCardAmount :Int,val giftCardId : String)
    data class PassCreditAmount(val creditAmount: Int)
    object CheckOutValidationFailed
    data class OpenRedeemPoint(val redeemPoint :Int)
    data class PassTotal(val redeemPoint: Double)
    object VisibleCheckOutScreen
    object VisibleOrderFragment
    object VisibleDeliveryFragment
    data class SearchOrderFilter(val searchText : String)
    data class OpenOrderSuccessDialog(val orderId: CreateOrderResponse)
    object RedeemProduct
    object DismissedPrinterDialog
    object RemoveBackButton
    object CloseOrderDetailsScreen
    object ClearLoyaltyScreen
    object HideProgressBar

}