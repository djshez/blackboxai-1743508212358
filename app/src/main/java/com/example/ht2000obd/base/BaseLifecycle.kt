package com.example.ht2000obd.base

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.example.ht2000obd.utils.LogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Interface for lifecycle-aware components
 */
interface LifecycleAware {
    fun onStart()
    fun onStop()
    fun onResume()
    fun onPause()
    fun onDestroy()
}

/**
 * Base implementation of lifecycle observer
 */
open class BaseLifecycleObserver : DefaultLifecycleObserver {
    
    override fun onCreate(owner: LifecycleOwner) {
        LogUtils.d("Lifecycle", "${javaClass.simpleName} onCreate")
    }

    override fun onStart(owner: LifecycleOwner) {
        LogUtils.d("Lifecycle", "${javaClass.simpleName} onStart")
    }

    override fun onResume(owner: LifecycleOwner) {
        LogUtils.d("Lifecycle", "${javaClass.simpleName} onResume")
    }

    override fun onPause(owner: LifecycleOwner) {
        LogUtils.d("Lifecycle", "${javaClass.simpleName} onPause")
    }

    override fun onStop(owner: LifecycleOwner) {
        LogUtils.d("Lifecycle", "${javaClass.simpleName} onStop")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        LogUtils.d("Lifecycle", "${javaClass.simpleName} onDestroy")
    }
}

/**
 * Extension function to launch coroutine in lifecycle scope
 */
fun LifecycleOwner.launchWhenStarted(
    block: suspend CoroutineScope.() -> Unit
): Job {
    return (this as? CoroutineScope)?.launch {
        lifecycle.whenStarted {
            block()
        }
    } ?: throw IllegalStateException("LifecycleOwner must implement CoroutineScope")
}

/**
 * Extension function to collect flow in lifecycle scope
 */
fun <T> Flow<T>.collectInLifecycle(
    lifecycleOwner: LifecycleOwner,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    collector: suspend (T) -> Unit
): Job {
    return lifecycleOwner.launchWhenStarted {
        lifecycleOwner.lifecycle.whenStateAtLeast(minActiveState) {
            collect { value ->
                collector(value)
            }
        }
    }
}

/**
 * Extension function to bind lifecycle
 */
fun LifecycleAware.bindToLifecycle(lifecycleOwner: LifecycleOwner) {
    lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            this@bindToLifecycle.onStart()
        }

        override fun onStop(owner: LifecycleOwner) {
            this@bindToLifecycle.onStop()
        }

        override fun onResume(owner: LifecycleOwner) {
            this@bindToLifecycle.onResume()
        }

        override fun onPause(owner: LifecycleOwner) {
            this@bindToLifecycle.onPause()
        }

        override fun onDestroy(owner: LifecycleOwner) {
            this@bindToLifecycle.onDestroy()
            lifecycleOwner.lifecycle.removeObserver(this)
        }
    })
}

/**
 * Extension function to execute code in specific lifecycle state
 */
suspend fun Lifecycle.whenStateAtLeast(
    state: Lifecycle.State,
    block: suspend () -> Unit
) {
    if (currentState.isAtLeast(state)) {
        block()
    }
}

/**
 * Extension function to execute code when started
 */
suspend fun Lifecycle.whenStarted(
    block: suspend () -> Unit
) = whenStateAtLeast(Lifecycle.State.STARTED, block)

/**
 * Extension function to execute code when resumed
 */
suspend fun Lifecycle.whenResumed(
    block: suspend () -> Unit
) = whenStateAtLeast(Lifecycle.State.RESUMED, block)

/**
 * Extension function to execute code when created
 */
suspend fun Lifecycle.whenCreated(
    block: suspend () -> Unit
) = whenStateAtLeast(Lifecycle.State.CREATED, block)

/**
 * Base class for lifecycle-aware components
 */
abstract class BaseLifecycleComponent : LifecycleAware {
    private var isStarted = false
    private var isResumed = false

    override fun onStart() {
        if (!isStarted) {
            isStarted = true
            onFirstStart()
        }
    }

    override fun onStop() {
        isStarted = false
    }

    override fun onResume() {
        if (!isResumed) {
            isResumed = true
            onFirstResume()
        }
    }

    override fun onPause() {
        isResumed = false
    }

    override fun onDestroy() {
        isStarted = false
        isResumed = false
    }

    protected open fun onFirstStart() {}
    protected open fun onFirstResume() {}
}

/**
 * Interface for lifecycle event callbacks
 */
interface LifecycleCallback {
    fun onLifecycleEvent(event: Lifecycle.Event)
}

/**
 * Extension function to add lifecycle callback
 */
fun LifecycleOwner.addLifecycleCallback(callback: LifecycleCallback) {
    lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            callback.onLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }

        override fun onStart(owner: LifecycleOwner) {
            callback.onLifecycleEvent(Lifecycle.Event.ON_START)
        }

        override fun onResume(owner: LifecycleOwner) {
            callback.onLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        override fun onPause(owner: LifecycleOwner) {
            callback.onLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }

        override fun onStop(owner: LifecycleOwner) {
            callback.onLifecycleEvent(Lifecycle.Event.ON_STOP)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            callback.onLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            lifecycle.removeObserver(this)
        }
    })
}