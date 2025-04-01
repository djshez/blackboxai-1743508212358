package com.example.ht2000obd.base

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface defining pagination functionality
 */
interface Pageable<T> {
    val currentPage: Int
    val pageSize: Int
    val totalItems: Int
    val totalPages: Int
    val items: List<T>
    val hasNextPage: Boolean
    val hasPreviousPage: Boolean
}

/**
 * Data class representing a page of items
 */
data class Page<T>(
    override val currentPage: Int,
    override val pageSize: Int,
    override val totalItems: Int,
    override val items: List<T>
) : Pageable<T> {
    override val totalPages: Int
        get() = if (totalItems == 0) 0 else (totalItems - 1) / pageSize + 1

    override val hasNextPage: Boolean
        get() = currentPage < totalPages

    override val hasPreviousPage: Boolean
        get() = currentPage > 1

    companion object {
        fun <T> empty() = Page<T>(
            currentPage = 0,
            pageSize = 0,
            totalItems = 0,
            items = emptyList()
        )
    }
}

/**
 * Interface for pagination state
 */
interface PaginationState<T> {
    val isLoading: Boolean
    val isRefreshing: Boolean
    val error: String?
    val page: Page<T>
    val isLastPage: Boolean
}

/**
 * Data class implementing pagination state
 */
data class DefaultPaginationState<T>(
    override val isLoading: Boolean = false,
    override val isRefreshing: Boolean = false,
    override val error: String? = null,
    override val page: Page<T> = Page.empty(),
    override val isLastPage: Boolean = false
) : PaginationState<T>

/**
 * Interface for pagination events
 */
sealed interface PaginationEvent {
    object LoadNextPage : PaginationEvent
    object Refresh : PaginationEvent
    object Retry : PaginationEvent
    data class LoadPage(val page: Int) : PaginationEvent
}

/**
 * Abstract class for handling pagination
 */
abstract class BasePagination<T>(
    private val initialPageSize: Int = DEFAULT_PAGE_SIZE,
    private val initialPage: Int = DEFAULT_INITIAL_PAGE
) {
    private val _state = MutableStateFlow(
        DefaultPaginationState<T>(
            page = Page(
                currentPage = initialPage,
                pageSize = initialPageSize,
                totalItems = 0,
                items = emptyList()
            )
        )
    )
    val state: StateFlow<PaginationState<T>> = _state

    /**
     * Load the next page
     */
    suspend fun loadNextPage() {
        if (_state.value.isLoading || _state.value.isLastPage) return
        loadPage(_state.value.page.currentPage + 1)
    }

    /**
     * Refresh the data
     */
    suspend fun refresh() {
        _state.value = _state.value.copy(
            isRefreshing = true,
            error = null
        )
        loadPage(initialPage)
    }

    /**
     * Retry loading after error
     */
    suspend fun retry() {
        if (_state.value.isLoading) return
        loadPage(_state.value.page.currentPage)
    }

    /**
     * Load a specific page
     */
    protected suspend fun loadPage(page: Int) {
        try {
            _state.value = _state.value.copy(
                isLoading = true,
                error = null
            )

            val result = load(page, initialPageSize)
            
            _state.value = _state.value.copy(
                isLoading = false,
                isRefreshing = false,
                page = result,
                isLastPage = !result.hasNextPage,
                error = null
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isLoading = false,
                isRefreshing = false,
                error = e.message ?: "Unknown error occurred"
            )
        }
    }

    /**
     * Abstract function to load data
     */
    protected abstract suspend fun load(page: Int, pageSize: Int): Page<T>

    companion object {
        const val DEFAULT_PAGE_SIZE = 20
        const val DEFAULT_INITIAL_PAGE = 1
    }
}

/**
 * Interface for paginated data source
 */
interface PaginatedDataSource<T> {
    suspend fun getPage(page: Int, pageSize: Int): Page<T>
    suspend fun refresh()
    fun observePages(): Flow<Page<T>>
}

/**
 * Extension function to create pagination state
 */
fun <T> createPaginationState(
    isLoading: Boolean = false,
    isRefreshing: Boolean = false,
    error: String? = null,
    page: Page<T> = Page.empty(),
    isLastPage: Boolean = false
): PaginationState<T> = DefaultPaginationState(
    isLoading = isLoading,
    isRefreshing = isRefreshing,
    error = error,
    page = page,
    isLastPage = isLastPage
)

/**
 * Extension function to update pagination state
 */
fun <T> PaginationState<T>.update(
    isLoading: Boolean = this.isLoading,
    isRefreshing: Boolean = this.isRefreshing,
    error: String? = this.error,
    page: Page<T> = this.page,
    isLastPage: Boolean = this.isLastPage
): PaginationState<T> = DefaultPaginationState(
    isLoading = isLoading,
    isRefreshing = isRefreshing,
    error = error,
    page = page,
    isLastPage = isLastPage
)