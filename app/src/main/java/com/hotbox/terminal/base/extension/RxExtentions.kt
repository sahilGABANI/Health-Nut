package com.hotbox.terminal.base.extension

import android.content.Context
import android.view.View
import android.widget.Toast
import com.hotbox.terminal.base.network.model.ErrorResult
import com.hotbox.terminal.base.network.parseRetrofitException
import com.jakewharton.rxbinding3.view.clicks
import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

fun View.throttleClicks(): Observable<Unit> {
    return clicks().throttleFirst(500, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
}

fun <T> Observable<T>.subscribeAndObserveOnMainThread(onNext: (t: T) -> Unit): Disposable {
    return observeOn(AndroidSchedulers.mainThread())
        .subscribe(onNext)
}

fun <T> Observable<T>.subscribeOnIoAndObserveOnMainThread(
    onNext: (t: T) -> Unit,
    onError: (Throwable) -> Unit
): Disposable {
    return subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(onNext, onError)
}

fun <T> Observable<T>.subscribeOnComputationAndObserveOnMainThread(
    onNext: (t: T) -> Unit,
    onError: (Throwable) -> Unit
): Disposable {
    return subscribeOn(Schedulers.computation())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(onNext, onError)
}

fun <T> Single<T>.subscribeOnIoAndObserveOnMainThread(
    onNext: (t: T) -> Unit,
    onError: (Throwable) -> Unit
): Disposable {
    return subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(onNext, onError)
}

fun <T> Single<T>.subscribeOnIoAndObserveOnIoThread(
    onNext: (t: T) -> Unit,
    onError: (Throwable) -> Unit
): Disposable {
    return subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(onNext, onError)
}

inline fun <T, reified E> Single<T>.subscribeWithErrorParsing(
    noinline onNext: (t: T) -> Unit,
    crossinline onError: (ErrorResult<E>) -> Unit,
): Disposable {
    return subscribe(onNext) {
        it.parseRetrofitException<E>()?.let { errorResponse ->
            onError(ErrorResult.ErrorMessage<E>(errorResponse = errorResponse))
        } ?: run {
            onError(ErrorResult.ErrorThrowable(it))
        }
    }
}

inline fun <T, reified E> Observable<T>.subscribeWithErrorParsing(
    noinline onNext: (t: T) -> Unit,
    crossinline onError: (ErrorResult<E>) -> Unit,
): Disposable {
    return subscribe(onNext) {
        it.parseRetrofitException<E>()?.let { errorResponse ->
            onError(ErrorResult.ErrorMessage<E>(errorResponse = errorResponse))
        } ?: run {
            onError(ErrorResult.ErrorThrowable(it))
        }
    }
}

fun Completable.subscribeOnIoAndObserveOnMainThread(
    onComplete: () -> Unit,
    onError: (Throwable) -> Unit
): Disposable {
    return subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(onComplete, onError)
}

fun <T> Observable<T>.subscribeOnIoAndObserveOnIo(
    onNext: (t: T) -> Unit,
    onError: (Throwable) -> Unit
): Disposable {
    return subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .subscribe(onNext, onError)
}

fun <T> Observable<T>.throttleClicks(): Observable<T> {
    return this.throttleFirst(500, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
}

fun <T> Single<T>.retryWithDelay(maxRetries: Int, retryDelayMillis: Int): Single<T> {
    var retryCount = 0
    return retryWhen { thObservable ->
        thObservable.flatMap { throwable ->
            if (++retryCount < maxRetries) {
                Flowable.timer(retryDelayMillis.toLong(), TimeUnit.MILLISECONDS)
            } else {
                Flowable.error(throwable)
            }
        }
    }
}


fun <T> SingleEmitter<T>.onSafeSuccess(t: T) {
    if (!isDisposed) onSuccess(t)
}

fun <T> SingleEmitter<T>.onSafeError(throwable: Exception) {
    if (!isDisposed) onError(throwable)
}

fun <T> MaybeEmitter<T>.onSafeSuccess(t: T) {
    if (!isDisposed) onSuccess(t)
}

fun <T> MaybeEmitter<T>.onSafeComplete() {
    if (!isDisposed) onComplete()
}

fun <T> MaybeEmitter<T>.onSafeError(throwable: Throwable) {
    if (!isDisposed) onError(throwable)
}

fun CompletableEmitter.onSafeComplete() {
    if (!isDisposed) onComplete()
}

fun CompletableEmitter.onSafeError(throwable: Throwable) {
    if (!isDisposed) onError(throwable)
}

fun <T> ObservableEmitter<T>.onSafeNext(t: T) {
    if (!isDisposed) onNext(t)
}

fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.showLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}