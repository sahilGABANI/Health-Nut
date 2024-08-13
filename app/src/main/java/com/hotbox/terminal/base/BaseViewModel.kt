package com.hotbox.terminal.base

import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

open class BaseViewModel : ViewModel() {

    private val compositeDisposable: CompositeDisposable = CompositeDisposable()

    override fun onCleared() {
        clearCompositeDisposable()
        super.onCleared()
    }

    fun Disposable.autoDispose() {
        compositeDisposable.add(this)
    }

    fun clearCompositeDisposable() {
        compositeDisposable.clear()
    }
}