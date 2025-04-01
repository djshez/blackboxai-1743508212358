package com.example.ht2000obd.base

import com.example.ht2000obd.utils.LogUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Interface for cache entries
 */
interface CacheEntry<T> {
    val data: T
    val timestamp: Long
    val expirationTime: Long
    
    fun isExpired(): Boolean = System.currentTimeMillis() > expirationTime
}

/**
 * Data class implementing cache entry
 */
data class DefaultCacheEntry<T>(
    override val data: T,
    override val timestamp: Long = System.currentTimeMillis(),
    override val expirationTime: Long = timestamp + DEFAULT_CACHE_DURATION
) : CacheEntry<T>

/**
 * Interface for cache operations
 */
interface Cache<K, V> {
    suspend fun get(key: K): V?
    suspend fun put(key: K, value: V)
    suspend fun remove(key: K)
    suspend fun clear()
    suspend fun contains(key: K): Boolean
    fun observe(key: K): Flow<V?>
}

/**
 * Base class for memory cache implementation
 */
abstract class BaseMemoryCache<K, V> : Cache<K, V> {
    
    protected val cache = ConcurrentHashMap<K, CacheEntry<V>>()

    override suspend fun get(key: K): V? {
        return try {
            cache[key]?.let { entry ->
                if (entry.isExpired()) {
                    remove(key)
                    null
                } else {
                    entry.data
                }
            }
        } catch (e: Exception) {
            LogUtils.e("Cache", "Error getting value for key: $key", e)
            null
        }
    }

    override suspend fun put(key: K, value: V) {
        try {
            val entry = createCacheEntry(value)
            cache[key] = entry
        } catch (e: Exception) {
            LogUtils.e("Cache", "Error putting value for key: $key", e)
        }
    }

    override suspend fun remove(key: K) {
        try {
            cache.remove(key)
        } catch (e: Exception) {
            LogUtils.e("Cache", "Error removing value for key: $key", e)
        }
    }

    override suspend fun clear() {
        try {
            cache.clear()
        } catch (e: Exception) {
            LogUtils.e("Cache", "Error clearing cache", e)
        }
    }

    override suspend fun contains(key: K): Boolean {
        return try {
            cache.containsKey(key) && get(key) != null
        } catch (e: Exception) {
            LogUtils.e("Cache", "Error checking key: $key", e)
            false
        }
    }

    override fun observe(key: K): Flow<V?> = flow {
        emit(get(key))
    }

    protected open fun createCacheEntry(value: V): CacheEntry<V> {
        return DefaultCacheEntry(value)
    }

    protected fun removeExpiredEntries() {
        try {
            cache.entries.removeIf { it.value.isExpired() }
        } catch (e: Exception) {
            LogUtils.e("Cache", "Error removing expired entries", e)
        }
    }

    companion object {
        const val DEFAULT_CACHE_DURATION = TimeUnit.HOURS.toMillis(1)
    }
}

/**
 * Base class for two-level cache implementation (memory + disk)
 */
abstract class BaseTwoLevelCache<K, V>(
    private val memoryCache: Cache<K, V>,
    private val diskCache: Cache<K, V>
) : Cache<K, V> {

    override suspend fun get(key: K): V? {
        return try {
            // Try memory cache first
            memoryCache.get(key) ?: run {
                // If not in memory, try disk cache
                diskCache.get(key)?.also { value ->
                    // Put in memory cache if found in disk
                    memoryCache.put(key, value)
                }
            }
        } catch (e: Exception) {
            LogUtils.e("Cache", "Error getting value for key: $key", e)
            null
        }
    }

    override suspend fun put(key: K, value: V) {
        try {
            memoryCache.put(key, value)
            diskCache.put(key, value)
        } catch (e: Exception) {
            LogUtils.e("Cache", "Error putting value for key: $key", e)
        }
    }

    override suspend fun remove(key: K) {
        try {
            memoryCache.remove(key)
            diskCache.remove(key)
        } catch (e: Exception) {
            LogUtils.e("Cache", "Error removing value for key: $key", e)
        }
    }

    override suspend fun clear() {
        try {
            memoryCache.clear()
            diskCache.clear()
        } catch (e: Exception) {
            LogUtils.e("Cache", "Error clearing cache", e)
        }
    }

    override suspend fun contains(key: K): Boolean {
        return try {
            memoryCache.contains(key) || diskCache.contains(key)
        } catch (e: Exception) {
            LogUtils.e("Cache", "Error checking key: $key", e)
            false
        }
    }

    override fun observe(key: K): Flow<V?> = flow {
        emit(get(key))
    }
}

/**
 * Cache configuration
 */
data class CacheConfig(
    val maxSize: Int = DEFAULT_MAX_SIZE,
    val expirationTime: Long = DEFAULT_EXPIRATION_TIME,
    val cleanupInterval: Long = DEFAULT_CLEANUP_INTERVAL
) {
    companion object {
        const val DEFAULT_MAX_SIZE = 100
        const val DEFAULT_EXPIRATION_TIME = TimeUnit.HOURS.toMillis(1)
        const val DEFAULT_CLEANUP_INTERVAL = TimeUnit.MINUTES.toMillis(30)
    }
}

/**
 * Cache result wrapper
 */
sealed class CacheResult<out T> {
    data class Success<T>(val data: T) : CacheResult<T>()
    data class Error(val exception: Exception) : CacheResult<Nothing>()
    object NotFound : CacheResult<Nothing>()
    object Expired : CacheResult<Nothing>()
}

/**
 * Extension function to wrap cache operations in CacheResult
 */
suspend fun <K, V> Cache<K, V>.getWithResult(key: K): CacheResult<V> {
    return try {
        get(key)?.let {
            CacheResult.Success(it)
        } ?: CacheResult.NotFound
    } catch (e: Exception) {
        CacheResult.Error(e)
    }
}