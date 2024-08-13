package com.hotbox.terminal.base.network

import com.hotbox.terminal.base.extension.onSafeError
import com.hotbox.terminal.base.extension.onSafeSuccess
import com.hotbox.terminal.base.network.model.HotBoxCommonResponse
import com.hotbox.terminal.base.network.model.HotBoxResponse
import io.reactivex.Single
import timber.log.Timber

class HotBoxResponseConverter {

    fun <T> convert(hotBoxResponse: HotBoxResponse<T>?): Single<T> {
        return convertToSingle(hotBoxResponse)
    }

    fun <T> convertToSingle(hotBoxResponse: HotBoxResponse<T>?): Single<T> {
        return Single.create { emitter ->
            when {
                hotBoxResponse == null -> emitter.onSafeError(Exception("Failed to process the info"))
                hotBoxResponse.status != 200 -> {
                    emitter.onSafeError(Exception(hotBoxResponse.message))
                }
                hotBoxResponse.status == 200 -> {
                    emitter.onSafeSuccess(hotBoxResponse.data)
                }
                else -> emitter.onSafeError(Exception("Failed to process the info"))
            }
        }
    }

    fun <T> convertToSingleWithFullResponse(hotBoxResponse: HotBoxResponse<T>?): Single<HotBoxResponse<T>> {
        return Single.create { emitter ->
            when {
                hotBoxResponse == null -> emitter.onSafeError(Exception("Failed to process the info"))
                hotBoxResponse.status != 200 -> {
                        emitter.onSafeError(Exception(hotBoxResponse.message))
                }
                hotBoxResponse.status == 200  -> {
                    emitter.onSafeSuccess(hotBoxResponse)
                }
                else -> emitter.onSafeError(Exception("Failed to process the info"))
            }
        }
    }

    fun convertCommonResponse(hotBoxCommonResponse: HotBoxCommonResponse?): Single<HotBoxCommonResponse> {
        return Single.create { emitter ->
            when {
                hotBoxCommonResponse == null -> emitter.onSafeError(Exception("Failed to process the info"))
                !hotBoxCommonResponse.success -> {
                    emitter.onSafeError(Exception(hotBoxCommonResponse.message))
                }
                hotBoxCommonResponse.success -> {
                    emitter.onSafeSuccess(hotBoxCommonResponse)
                }
                else -> emitter.onSafeError(Exception("Failed to process the info"))
            }
        }
    }
}