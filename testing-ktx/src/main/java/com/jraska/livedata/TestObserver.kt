package com.jraska.livedata

import androidx.annotation.Nullable
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

fun <T> LiveData<T>.test(): TestObserver<T> {
  return TestObserver.test(this)
}

class TestObserver<T> private constructor() : Observer<T> {
  private val valueHistory: MutableList<T> = mutableListOf()
  private val childObservers: MutableList<Observer<T>> = mutableListOf()

  private var valueLatch = CountDownLatch(1)

  override fun onChanged(@Nullable value: T) {
    valueHistory.add(value)
    valueLatch.countDown()
    childObservers.forEach { it.onChanged(value) }
  }

  fun value(): T {
    assertHasValue()
    return valueHistory[valueHistory.size - 1]
  }

  fun valueHistory(): List<T> {
    return Collections.unmodifiableList(valueHistory)
  }

  fun assertHasValue(): TestObserver<T> {
    if (valueHistory.isEmpty()) {
      throw fail("Observer never received any value")
    }

    return this
  }

  fun assertNoValue(): TestObserver<T> {
    if (!valueHistory.isEmpty()) {
      throw fail("Expected no value, but received: " + value()!!)
    }

    return this
  }

  fun assertHistorySize(expectedSize: Int): TestObserver<T> {
    val size = valueHistory.size
    if (size != expectedSize) {
      throw fail("History size differ; Expected: $expectedSize, Actual: $size")
    }
    return this
  }

  fun assertValue(expected: T?): TestObserver<T> {
    val value = value()

    if (expected == null && value == null) {
      return this
    }

    if (value != expected) {
      throw fail("Expected: " + valueAndClass(expected) + ", Actual: " + valueAndClass(value))
    }

    return this
  }

  fun assertValue(valuePredicate: (T) -> Boolean): TestObserver<T> {
    val value = value()

    if (!valuePredicate(value)) {
      throw fail("Value not present")
    }

    return this
  }

  fun assertNever(valuePredicate: (T) -> Boolean): TestObserver<T> {
    val size = valueHistory.size
    for (valueIndex in 0 until size) {
      val value = this.valueHistory[valueIndex]
      if (valuePredicate(value)) {
        throw fail("Value at position " + valueIndex + " matches predicate "
          + valuePredicate.toString() + ", which was not expected.")
      }
    }

    return this
  }

  /**
   * Allows assertion of some mapped value extracted from originally observed values.
   * History of observed values is retained.
   *
   * This can became useful when you want to perform assertions on some complex structure and
   * you want to assert only on one field.
   *
   * @param mapper Function to map originally observed value.
   * @param <N> Type of mapper.
   * @return TestObserver for mapped value
  </N> */
  fun <N> map(mapper: (T) -> N): TestObserver<N> {
    val newObserver = create<N>()
    // We want the history match the current one
    for (value in valueHistory) {
      newObserver.onChanged(mapper(value))
    }

    childObservers.add(Observer { newObserver.onChanged(mapper(it)) })
    return newObserver
  }

  /**
   * Awaits until this TestObserver has any value.
   *
   * If this TestObserver has already value then this method returns immediately.
   *
   * @return this
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  @Throws(InterruptedException::class)
  fun awaitValue(): TestObserver<T> {
    valueLatch.await()
    return this
  }

  /**
   * Awaits the specified amount of time or until this TestObserver has any value.
   *
   * If this TestObserver has already value then this method returns immediately.
   *
   * @return this
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  @Throws(InterruptedException::class)
  fun awaitValue(timeout: Long, timeUnit: TimeUnit): TestObserver<T> {
    valueLatch.await(timeout, timeUnit)
    return this
  }

  /**
   * Awaits until this TestObserver receives next value.
   *
   * If this TestObserver has already value then it awaits for another one.
   *
   * @return this
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  @Throws(InterruptedException::class)
  fun awaitNextValue(): TestObserver<T> {
    return withNewLatch().awaitValue()
  }


  /**
   * Awaits the specified amount of time or until this TestObserver receives next value.
   *
   * If this TestObserver has already value then it awaits for another one.
   *
   * @return this
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  @Throws(InterruptedException::class)
  fun awaitNextValue(timeout: Long, timeUnit: TimeUnit): TestObserver<T> {
    return withNewLatch().awaitValue(timeout, timeUnit)
  }

  private fun withNewLatch(): TestObserver<T> {
    valueLatch = CountDownLatch(1)
    return this
  }

  private fun fail(message: String): AssertionError {
    return AssertionError(message)
  }

  private fun valueAndClass(value: Any?): String {
    if (value != null) {
      return value.toString() + " (class: " + value.javaClass.simpleName + ")"
    } else {
      return "null"
    }
  }

  companion object {
    @JvmStatic
    fun <T> create(): TestObserver<T> {
      return TestObserver()
    }

    @JvmStatic
    fun <T> test(liveData: LiveData<T>): TestObserver<T> {
      val observer = create<T>()
      liveData.observeForever(observer)
      return observer
    }
  }
}
