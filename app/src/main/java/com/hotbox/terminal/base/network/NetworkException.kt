package com.hotbox.terminal.base.network

import com.hotbox.terminal.base.network.model.HotBoxError
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.HttpException
import retrofit2.Retrofit
import timber.log.Timber
import java.io.IOException
import kotlin.reflect.KClass

open class NetworkException(
    val error: Any? = null,
    message: String? = null,
    val exception: Throwable
) : RuntimeException(message, exception) {

    companion object {

        private fun parseError(
            retrofit: Retrofit,
            httpException: HttpException,
            kClass: KClass<*>
        ): Any? {
            if (httpException.response()?.isSuccessful == true) {
                return null
            }
            val errorBody = httpException.response()?.errorBody() ?: return null
            val converter: Converter<ResponseBody, Any> =
                retrofit.responseBodyConverter(kClass.java, arrayOf())
            return converter.convert(errorBody)
        }

        fun asRetrofitException(
            annotations: Array<Annotation>,
            retrofit: Retrofit,
            throwable: Throwable
        ): Throwable {
            val errorType: ErrorType? = annotations.find { it is ErrorType } as? ErrorType
            return when (throwable) {
                is HttpException -> {
                    return if (errorType != null) {
                        val error = parseError(retrofit, throwable, errorType.type)
                        NetworkException(error, throwable.message(), throwable)
                    } else {
                        throwable
                    }
                }
                is IOException -> {
                    NetworkError(
                        message = throwable.localizedMessage,
                        exception = throwable
                    )
                }
                else -> throwable
            }
        }
    }
}

fun Throwable.parseRetrofitException(): HotBoxError? {
    if (this is NetworkException && this.error is HotBoxError) {
        return this.error
    }
    Timber.e(this)
    return null
}

inline fun <reified T> Throwable.parseRetrofitException(): T? {
    if (this is NetworkException && this.error is T) {
        return this.error
    }
    Timber.e(this)
    return null
}

data class NetworkError(
    override val message: String? = null,
    val exception: Throwable
) : RuntimeException(message, exception)