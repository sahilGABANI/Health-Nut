package com.hotbox.terminal.utils

import com.hotbox.terminal.BuildConfig

object Constants {

    const val CHECK_ALL = "All"
    const val CHECK_AVAILABLE = "Available"
    const val CHECK_UNAVAILABLE = "Unavailable"

    const val CATEGORY_FILTER_ALL = "All"
    const val TIER_1_DELIVERY_FEE = "tier_1_delivery_fee"
    const val MODE_ID = 5
    const val ORDER_TYPE_ID = 2

    const val CONNECTED_LOCATION_ID = "tml_EreAPwHzehWwZx"
    const val QR_CODE_TYPE_LOYALTY = "loyalty"
    const val QR_CODE_TYPE_GIFT_CARD = "giftcard"
    const val LIVE_READER = "tmr_ErwQjwmEiCCNdo"
    const val DEMO_READER = "tmr_EreA8w0pYIhBij"
    const val STRIPE_SK_TOKEN = "sk_test_51H7ofqHBh9c4S8JHZ2Uncluam52UeNXDBtsxsqQSSl3lh5n417UgKTIbwz4olMJXHjUZ91SfKMLelfpmpWRheCZC00rvxVj5pa"
    const val CAPTURE_ERROR = "Capture Error"
    const val CREATE_PAYMENT_ERROR = "Create Payment Error"
    const val PROCESS_PAYMENT_ERROR = "Process Payment Error"
    const val RETRIEVE_READER_ERROR = "Retrieve Reader Error"
    const val PAYMENT_METHOD_TYPE = "card_present"
    const val CURRENCY = "usd"
    const val CAPTURE_METHOD = "manual"
    const val PLATFORM = "POS"
    const val ACTIVE = "active"
    const val NEW = "new"
    const val PAST = "past"
    const val POS = "POS"
    const val KIOSK = "Kiosk"
    const val WEB_SITE = "www.healthnutla.com"
    const val MENTION = "@healthnutla"
    const val RECEIPT = "Order :"
    const val FRESH_OBSESSED = "FRESH OBSESSED!"
    const val ORDER_STATUS_RECEIVE = "received"
    const val ORDER_STATUS_COMPLETED = "completed"
    const val ORDER_STATUS_CANCELLED = "cancelled"
    const val ORDER_TYPE_ID_DINE_IN = 9
    const val ORDER_TYPE_DINE_IN = "DINE IN"
    const val ORDER_TYPE_ID_TO_GO = 10
    const val ORDER_TYPE_TO_GO = "TO GO"
    const val ORDER_TYPE_ID_EMPLOYEE_MEAL = 11
    const val ORDER_TYPE_EMPLOYEE_MEAL = "EMPLOYEE MEAL"
    const val DEFAULT_AUTO_RECEIVED_USER_ID = "48474"
    const val TRANSACTION_ID_OF_PROCESSOR = "88989"
    const val TRANSACTION_ID_OF_PROCESSOR_FOR_ORDER = "85171"
    const val TRANSACTION_CHARGE_ID = "000000000231868"
    const val ABANDONED = "abandoned"
    const val EMAIL = "Email"
    const val PHONE = "Phone"
    const val COMP_ORDER_TEXT = "COMP'D ORDER"
    const val AVAILABLE_TO_PRINT_STATUS = 1
    const val UNAVAILABLE_TO_PRINT_STATUS = 0
    const val CHIPS_AND_BOTTLED_DRINKS_MENU_ID = 9
    const val DRESSINGS_MENU_ID = 11

    /**
     *Our Device Id For Development
     */
    const val DEVICE_ID = "14b0fbb66d3cac64"
//    const val DEVICE_ID =  "3de8b421c7594026"
    /**
     * client Device Id For Development
     * */
//    const val DEVICE_ID =  "32f3c878ce8cf5c9"  // client Device Id For Development
//    const val DEVICE_ID =  "95981ef9953be389" // Our Device Id For production
    fun isDebugMode(): Boolean {
        return BuildConfig.DEBUG
    }

}