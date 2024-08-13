package com.hotbox.terminal.base

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

/**
 * This uses the parent context to add observables to the view for parent lifecycle events
 */
abstract class ConstraintLayoutWithLifecycle(
    context: Context, attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle), LifecycleObserver {
    private val compositeDisposable = CompositeDisposable()

    init {
        when (context) {
            is AppCompatActivity -> context.lifecycle.addObserver(this)
            is Fragment -> context.lifecycle.addObserver(this)
            else -> throw Exception("Parent was not a lifecycle owner")
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    open fun onDestroy() {
        compositeDisposable.clear()
    }

    protected fun Disposable.autoDispose() {
        compositeDisposable.add(this)
    }

    protected fun destroy() {
        compositeDisposable.clear()
    }
}