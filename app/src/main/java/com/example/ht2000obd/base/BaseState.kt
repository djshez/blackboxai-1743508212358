package com.example.ht2000obd.base

/**
 * Base class for UI states
 */
sealed class BaseState<out T> {
    /**
     * Initial state
     */
    object Initial : BaseState<Nothing>()

    /**
     * Loading state
     */
    data class Loading(
        val message: String? = null,
        val progress: Int? = null
    ) : BaseState<Nothing>()

    /**
     * Success state with data
     */
    data class Success<T>(
        val data: T
    ) : BaseState<T>()

    /**
     * Error state
     */
    data class Error(
        val message: String,
        val code: Int? = null,
        val throwable: Throwable? = null
    ) : BaseState<Nothing>()

    /**
     * Empty state
     */
    object Empty : BaseState<Nothing>()

    /**
     * Check if state is [Initial]
     */
    fun isInitial() = this is Initial

    /**
     * Check if state is [Loading]
     */
    fun isLoading() = this is Loading

    /**
     * Check if state is [Success]
     */
    fun isSuccess() = this is Success

    /**
     * Check if state is [Error]
     */
    fun isError() = this is Error

    /**
     * Check if state is [Empty]
     */
    fun isEmpty() = this is Empty

    /**
     * Get data if state is [Success]
     */
    fun getDataOrNull(): T? = if (this is Success) data else null

    /**
     * Get error message if state is [Error]
     */
    fun getErrorMessageOrNull(): String? = if (this is Error) message else null

    /**
     * Get loading message if state is [Loading]
     */
    fun getLoadingMessageOrNull(): String? = if (this is Loading) message else null

    /**
     * Get loading progress if state is [Loading]
     */
    fun getProgressOrNull(): Int? = if (this is Loading) progress else null

    /**
     * Handle state with callbacks
     */
    fun handle(
        onInitial: () -> Unit = {},
        onLoading: (String?, Int?) -> Unit = { _, _ -> },
        onSuccess: (T) -> Unit = {},
        onError: (String, Int?, Throwable?) -> Unit = { _, _, _ -> },
        onEmpty: () -> Unit = {}
    ) {
        when (this) {
            is Initial -> onInitial()
            is Loading -> onLoading(message, progress)
            is Success -> onSuccess(data)
            is Error -> onError(message, code, throwable)
            is Empty -> onEmpty()
        }
    }

    /**
     * Map success data to another type
     */
    fun <R> map(transform: (T) -> R): BaseState<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Loading -> Loading(message, progress)
            is Error -> Error(message, code, throwable)
            is Empty -> Empty
            is Initial -> Initial
        }
    }

    companion object {
        /**
         * Create success state
         */
        fun <T> success(data: T): BaseState<T> = Success(data)

        /**
         * Create error state
         */
        fun error(
            message: String,
            code: Int? = null,
            throwable: Throwable? = null
        ): BaseState<Nothing> = Error(message, code, throwable)

        /**
         * Create loading state
         */
        fun loading(
            message: String? = null,
            progress: Int? = null
        ): BaseState<Nothing> = Loading(message, progress)

        /**
         * Create empty state
         */
        fun empty(): BaseState<Nothing> = Empty

        /**
         * Create initial state
         */
        fun initial(): BaseState<Nothing> = Initial
    }
}

/**
 * Base class for UI events
 */
sealed class BaseEvent {
    /**
     * Show message event
     */
    data class ShowMessage(
        val message: String,
        val duration: Int = android.widget.Toast.LENGTH_SHORT
    ) : BaseEvent()

    /**
     * Show error event
     */
    data class ShowError(
        val message: String,
        val duration: Int = android.widget.Toast.LENGTH_LONG
    ) : BaseEvent()

    /**
     * Navigation event
     */
    data class Navigate(
        val route: String,
        val args: Map<String, Any> = emptyMap()
    ) : BaseEvent()

    /**
     * Navigate back event
     */
    object NavigateBack : BaseEvent()

    /**
     * Refresh event
     */
    object Refresh : BaseEvent()

    /**
     * Loading event
     */
    data class Loading(
        val show: Boolean,
        val message: String? = null
    ) : BaseEvent()
}

/**
 * Base class for UI effects
 */
sealed class BaseEffect {
    /**
     * Navigation effect
     */
    data class Navigation(
        val route: String,
        val args: Map<String, Any> = emptyMap()
    ) : BaseEffect()

    /**
     * Message effect
     */
    data class Message(
        val message: String,
        val type: Type = Type.INFO
    ) : BaseEffect() {
        enum class Type {
            INFO,
            SUCCESS,
            ERROR,
            WARNING
        }
    }

    /**
     * Loading effect
     */
    data class Loading(
        val show: Boolean,
        val message: String? = null
    ) : BaseEffect()
}