package com.example.ht2000obd.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Base class for testing ViewModels
 */
abstract class BaseViewModelTest {
    protected val testDispatcher = TestCoroutineDispatcher()
    protected val testScope = TestCoroutineScope(testDispatcher)

    protected fun runTest(block: suspend TestCoroutineScope.() -> Unit) {
        testScope.runBlockingTest(block)
    }

    protected fun <T> LiveData<T>.getOrAwaitValue(
        time: Long = 2,
        timeUnit: TimeUnit = TimeUnit.SECONDS,
        afterObserve: () -> Unit = {}
    ): T {
        var data: T? = null
        val latch = CountDownLatch(1)
        val observer = object : Observer<T> {
            override fun onChanged(value: T) {
                data = value
                latch.countDown()
                this@getOrAwaitValue.removeObserver(this)
            }
        }
        this.observeForever(observer)

        try {
            afterObserve.invoke()

            // Don't wait indefinitely if the LiveData is not set.
            if (!latch.await(time, timeUnit)) {
                throw TimeoutException("LiveData value was never set.")
            }

        } finally {
            this.removeObserver(observer)
        }

        @Suppress("UNCHECKED_CAST")
        return data as T
    }
}

/**
 * JUnit Rule for testing coroutines
 */
@ExperimentalCoroutinesApi
class MainCoroutineRule(
    private val dispatcher: CoroutineDispatcher = TestCoroutineDispatcher()
) : TestWatcher() {

    override fun starting(description: Description?) {
        super.starting(description)
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        Dispatchers.resetMain()
    }
}

/**
 * Base class for testing repositories
 */
abstract class BaseRepositoryTest {
    protected val testDispatcher = TestCoroutineDispatcher()
    protected val testScope = TestCoroutineScope(testDispatcher)

    protected fun runTest(block: suspend TestCoroutineScope.() -> Unit) {
        testScope.runBlockingTest(block)
    }
}

/**
 * Base class for testing use cases
 */
abstract class BaseUseCaseTest {
    protected val testDispatcher = TestCoroutineDispatcher()
    protected val testScope = TestCoroutineScope(testDispatcher)

    protected fun runTest(block: suspend TestCoroutineScope.() -> Unit) {
        testScope.runBlockingTest(block)
    }
}

/**
 * Base class for testing data sources
 */
abstract class BaseDataSourceTest {
    protected val testDispatcher = TestCoroutineDispatcher()
    protected val testScope = TestCoroutineScope(testDispatcher)

    protected fun runTest(block: suspend TestCoroutineScope.() -> Unit) {
        testScope.runBlockingTest(block)
    }
}

/**
 * Base class for testing mappers
 */
abstract class BaseMapperTest<I, O> {
    protected abstract val mapper: BaseMapper<I, O>

    protected fun assertMapping(input: I, expectedOutput: O) {
        val actualOutput = mapper.map(input)
        assert(actualOutput == expectedOutput) {
            "Mapping failed. Expected: $expectedOutput, Actual: $actualOutput"
        }
    }
}

/**
 * Base class for testing validators
 */
abstract class BaseValidatorTest {
    protected fun assertValid(validationResult: ValidationResult) {
        assert(validationResult is ValidationResult.Valid) {
            "Expected validation to pass but got error: ${(validationResult as? ValidationResult.Invalid)?.message}"
        }
    }

    protected fun assertInvalid(validationResult: ValidationResult, expectedMessage: String) {
        assert(validationResult is ValidationResult.Invalid) {
            "Expected validation to fail but got Valid"
        }
        assert((validationResult as ValidationResult.Invalid).message == expectedMessage) {
            "Expected error message: $expectedMessage, but got: ${validationResult.message}"
        }
    }
}

/**
 * Base class for testing workers
 */
abstract class BaseWorkerTest {
    protected val testDispatcher = TestCoroutineDispatcher()
    protected val testScope = TestCoroutineScope(testDispatcher)

    protected fun runTest(block: suspend TestCoroutineScope.() -> Unit) {
        testScope.runBlockingTest(block)
    }
}

/**
 * Base class for testing services
 */
abstract class BaseServiceTest {
    protected val testDispatcher = TestCoroutineDispatcher()
    protected val testScope = TestCoroutineScope(testDispatcher)

    protected fun runTest(block: suspend TestCoroutineScope.() -> Unit) {
        testScope.runBlockingTest(block)
    }
}

/**
 * Base class for testing broadcast receivers
 */
abstract class BaseBroadcastReceiverTest {
    protected val testDispatcher = TestCoroutineDispatcher()
    protected val testScope = TestCoroutineScope(testDispatcher)

    protected fun runTest(block: suspend TestCoroutineScope.() -> Unit) {
        testScope.runBlockingTest(block)
    }
}

/**
 * Extension function for testing coroutines
 */
fun TestCoroutineScope.runBlockingTest(block: suspend TestCoroutineScope.() -> Unit) =
    this.testDispatcher.runBlockingTest {
        block()
    }