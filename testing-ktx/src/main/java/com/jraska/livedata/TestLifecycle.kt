package com.jraska.livedata

import androidx.annotation.NonNull
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

class TestLifecycle private constructor() : LifecycleOwner {
  private val registry = LifecycleRegistry(this)

  val currentState: Lifecycle.State
    get() = registry.currentState

  fun create(): TestLifecycle {
    return handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
  }

  fun start(): TestLifecycle {
    return handleLifecycleEvent(Lifecycle.Event.ON_START)
  }

  fun resume(): TestLifecycle {
    return handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
  }

  fun pause(): TestLifecycle {
    return handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
  }

  fun stop(): TestLifecycle {
    return handleLifecycleEvent(Lifecycle.Event.ON_STOP)
  }

  fun destroy(): TestLifecycle {
    return handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
  }

  private fun handleLifecycleEvent(@NonNull event: Lifecycle.Event): TestLifecycle {
    registry.handleLifecycleEvent(event)
    return this
  }

  @NonNull
  override fun getLifecycle(): Lifecycle {
    return registry
  }

  companion object {

    @JvmStatic
    fun initialized(): TestLifecycle {
      return TestLifecycle()
    }

    @JvmStatic
    fun resumed(): TestLifecycle {
      return initialized().resume()
    }
  }
}
