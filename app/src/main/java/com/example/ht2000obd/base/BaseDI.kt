package com.example.ht2000obd.base

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ht2000obd.utils.LogUtils

/**
 * Interface for dependency provider
 */
interface DependencyProvider {
    fun <T : Any> provide(clazz: Class<T>): T
    fun <T : Any> provideLazy(clazz: Class<T>): Lazy<T>
}

/**
 * Base implementation of dependency provider
 */
abstract class BaseDependencyProvider : DependencyProvider {
    private val dependencies = mutableMapOf<Class<*>, Any>()
    private val lazyDependencies = mutableMapOf<Class<*>, Lazy<Any>>()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> provide(clazz: Class<T>): T {
        return try {
            dependencies.getOrPut(clazz) { createDependency(clazz) } as T
        } catch (e: Exception) {
            LogUtils.e("DI", "Error providing dependency for ${clazz.simpleName}", e)
            throw e
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> provideLazy(clazz: Class<T>): Lazy<T> {
        return try {
            lazyDependencies.getOrPut(clazz) {
                lazy { createDependency(clazz) }
            } as Lazy<T>
        } catch (e: Exception) {
            LogUtils.e("DI", "Error providing lazy dependency for ${clazz.simpleName}", e)
            throw e
        }
    }

    protected abstract fun <T : Any> createDependency(clazz: Class<T>): T

    protected fun clear() {
        dependencies.clear()
        lazyDependencies.clear()
    }
}

/**
 * Factory for creating ViewModels with dependencies
 */
class ViewModelFactory(
    private val dependencyProvider: DependencyProvider
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return try {
            dependencyProvider.provide(modelClass)
        } catch (e: Exception) {
            LogUtils.e("DI", "Error creating ViewModel for ${modelClass.simpleName}", e)
            throw e
        }
    }
}

/**
 * Interface for dependency scope
 */
interface DependencyScope {
    fun init(context: Context)
    fun clear()
}

/**
 * Base implementation of dependency scope
 */
abstract class BaseDependencyScope : DependencyScope {
    protected val dependencies = mutableMapOf<Class<*>, Any>()

    override fun clear() {
        dependencies.clear()
    }
}

/**
 * Application scope for dependencies
 */
class ApplicationScope : BaseDependencyScope() {
    override fun init(context: Context) {
        // Initialize application-scoped dependencies
        dependencies[Context::class.java] = context.applicationContext
        // Add other application-scoped dependencies here
    }
}

/**
 * Activity scope for dependencies
 */
class ActivityScope : BaseDependencyScope() {
    override fun init(context: Context) {
        // Initialize activity-scoped dependencies
        dependencies[Context::class.java] = context
        // Add other activity-scoped dependencies here
    }
}

/**
 * Fragment scope for dependencies
 */
class FragmentScope : BaseDependencyScope() {
    override fun init(context: Context) {
        // Initialize fragment-scoped dependencies
        dependencies[Context::class.java] = context
        // Add other fragment-scoped dependencies here
    }
}

/**
 * Extension functions for dependency injection
 */
inline fun <reified T : Any> DependencyProvider.get(): T {
    return provide(T::class.java)
}

inline fun <reified T : Any> DependencyProvider.getLazy(): Lazy<T> {
    return provideLazy(T::class.java)
}

/**
 * Interface for component that requires dependencies
 */
interface DependencyRequester {
    fun inject(dependencyProvider: DependencyProvider)
}

/**
 * Interface for dependency container
 */
interface DependencyContainer {
    fun <T : Any> register(clazz: Class<T>, instance: T)
    fun <T : Any> register(clazz: Class<T>, provider: () -> T)
    fun <T : Any> unregister(clazz: Class<T>)
    fun clear()
}

/**
 * Base implementation of dependency container
 */
class BaseDependencyContainer : DependencyContainer {
    private val instances = mutableMapOf<Class<*>, Any>()
    private val providers = mutableMapOf<Class<*>, () -> Any>()

    override fun <T : Any> register(clazz: Class<T>, instance: T) {
        instances[clazz] = instance
    }

    override fun <T : Any> register(clazz: Class<T>, provider: () -> T) {
        providers[clazz] = provider
    }

    override fun <T : Any> unregister(clazz: Class<T>) {
        instances.remove(clazz)
        providers.remove(clazz)
    }

    override fun clear() {
        instances.clear()
        providers.clear()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> resolve(clazz: Class<T>): T {
        return instances[clazz] as? T
            ?: providers[clazz]?.invoke() as? T
            ?: throw IllegalStateException("No dependency found for ${clazz.simpleName}")
    }
}