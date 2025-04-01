package com.example.ht2000obd.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ht2000obd.utils.CoroutineUtils
import com.example.ht2000obd.utils.LogUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {

    // UI State handling
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error handling
    private val _error = Channel<String>()
    val error: Flow<String> = _error.receiveAsFlow()

    // Event handling
    private val _events = MutableSharedFlow<Event>()
    val events: SharedFlow<Event> = _events.asSharedFlow()

    // Protected methods for state management
    protected fun showLoading() {
        _isLoading.value = true
    }

    protected fun hideLoading() {
        _isLoading.value = false
    }

    protected fun emitError(error: String) {
        viewModelScope.launch {
            _error.send(error)
        }
    }

    protected fun emitEvent(event: Event) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }

    // Coroutine utility methods
    protected fun launchCoroutine(
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
        block: suspend () -> Unit
    ): Job {
        return viewModelScope.launch(
            CoroutineUtils.createExceptionHandler(javaClass.simpleName)
        ) {
            try {
                block()
            } catch (e: Exception) {
                LogUtils.e(javaClass.simpleName, "Error in coroutine", e)
                emitError(e.message ?: "An unexpected error occurred")
            }
        }
    }

    protected fun launchWithLoading(
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
        block: suspend () -> Unit
    ): Job {
        return viewModelScope.launch(
            CoroutineUtils.createExceptionHandler(javaClass.simpleName)
        ) {
            try {
                showLoading()
                block()
            } catch (e: Exception) {
                LogUtils.e(javaClass.simpleName, "Error in coroutine", e)
                emitError(e.message ?: "An unexpected error occurred")
            } finally {
                hideLoading()
            }
        }
    }

    // Safe state flow creation with initial value
    protected fun <T> createStateFlow(initialValue: T): MutableStateFlow<T> {
        return MutableStateFlow(initialValue)
    }

    // Safe shared flow creation
    protected fun <T> createSharedFlow(): MutableSharedFlow<T> {
        return MutableSharedFlow()
    }

    // Base Event class for UI events
    sealed class Event {
        data class ShowMessage(val message: String) : Event()
        data class ShowError(val error: String) : Event()
        data class Navigate(val route: String) : Event()
        object NavigateBack : Event()
        object Refresh : Event()
    }

    // Lifecycle logging
    init {
        LogUtils.d("ViewModel", "${javaClass.simpleName} initialized")
    }

    override fun onCleared() {
        super.onCleared()
        LogUtils.d("ViewModel", "${javaClass.simpleName} cleared")
    }

    // Protected method for handling errors
    protected fun handleError(throwable: Throwable) {
        LogUtils.e(javaClass.simpleName, "Error occurred", throwable)
        val errorMessage = when (throwable) {
            is com.example.ht2000obd.exceptions.OBDException -> throwable.message
            else -> throwable.message ?: "An unexpected error occurred"
        }
        emitError(errorMessage)
    }

    // Protected method for safe flow collection
    protected fun <T> Flow<T>.collectSafely(
        onError: (Throwable) -> Unit = { handleError(it) },
        onSuccess: suspend (T) -> Unit
    ) {
        viewModelScope.launch {
            try {
                collect { value ->
                    onSuccess(value)
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    // Protected method for retrying operations
    protected suspend fun <T> retryOperation(
        times: Int = 3,
        initialDelay: Long = 100,
        maxDelay: Long = 1000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        return CoroutineUtils.retryWithBackoff(
            times = times,
            initialDelay = initialDelay,
            maxDelay = maxDelay,
            factor = factor,
            block = block
        )
    }
}