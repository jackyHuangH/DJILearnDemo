package com.zenchn.common.eventbus

import androidx.annotation.Nullable
import androidx.lifecycle.ExternalLiveData
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

interface Event<T> {

    /**
     * 延迟发送一个消息，支持前台线程、后台线程发送
     *
     * @param value
     * @param delay 延迟毫秒数
     */
    fun post(value: T, delay: Long = 0)

    /**
     * 发送一个消息，支持前台线程、后台线程发送
     * 需要跨进程、跨APP发送消息的时候调用该方法
     *
     * @param value
     */
    fun broadcast(value: T, foreground: Boolean = true)

    /**
     * 注册一个Observer，生命周期感知，自动取消订阅
     *
     * @param owner
     * @param sticky
     * @param observer
     */
    fun observe(owner: LifecycleOwner, sticky: Boolean = false, observer: Observer<T>)

    /**
     * 注册一个Observer
     *
     *
     * @param observer
     * @param sticky 如果之前有消息发送，可以在注册时收到消息（消息同步）
     */
    fun observeForever(sticky: Boolean = false, observer: Observer<T>)

    /**
     * 通过observeForever或observeStickyForever注册的，需要调用该方法取消订阅
     *
     * @param observer
     */
    fun removeObserver(observer: Observer<T>)

}

internal class LiveEvent<T> internal constructor(private val key: String) : Event<T> {

    private val liveData: LifecycleLiveData<T> = LifecycleLiveData()
    private val observerMap = ConcurrentHashMap<Observer<*>, ObserverWrapper<T>>()

    companion object {
        val logger = DefaultLogger()
        val eventBus = LiveEventBusCore.get()
    }

    override fun post(value: T, delay: Long) = eventBus.sendToTarget(delay) {
        liveData.setValue(value)
    }

    override fun broadcast(value: T, foreground: Boolean) =
            eventBus.sendBroadcast(key, value as Any, foreground)

    override fun observe(owner: LifecycleOwner, sticky: Boolean, observer: Observer<T>) =
            eventBus.sendToTarget {
                val observerWrapper = ObserverWrapper(observer)
                if (sticky) {
                    liveData.observe(owner, observerWrapper)
                    logger.log(
                            Level.INFO,
                            "observe sticky observer: $observerWrapper($observer) on owner: $owner with key: $key"
                    )
                } else {
                    observerWrapper.preventNextEvent =
                            liveData.version > ExternalLiveData.START_VERSION;
                    liveData.observe(owner, observerWrapper);
                    logger.log(
                            Level.INFO,
                            "observe observer: $observerWrapper($observer) on owner: $owner with key: $key"
                    )
                }
            }

    override fun observeForever(sticky: Boolean, observer: Observer<T>) {

        val observerWrapper = ObserverWrapper(observer)

        if (sticky) {
            observerMap[observer] = observerWrapper
            liveData.observeForever(observerWrapper)
            logger.log(
                    Level.INFO,
                    "observe sticky forever observer: $observerWrapper($observer) with key: $key"
            )
        } else {
            observerWrapper.preventNextEvent = liveData.version > ExternalLiveData.START_VERSION;
            observerMap[observer] = observerWrapper
            liveData.observeForever(observerWrapper)
            logger.log(
                    Level.INFO,
                    "observe forever observer: $observerWrapper($observer) with key: $key"
            )
        }
    }

    override fun removeObserver(observer: Observer<T>) = LiveEventBusCore.get().sendToTarget {
        observer.run {
            if (observerMap.containsKey(this)) {
                observerMap.remove(this)?.let {
                    liveData.removeObserver(it)
                }
                logger.log(
                        Level.INFO,
                        "observe removed: $observer with key: $key"
                )
            } else {
                liveData.removeObserver(this)
            }
        }
    }

    private inner class LifecycleLiveData<T> : ExternalLiveData<T>() {

        override fun observerActiveLevel(): Lifecycle.State {
            return if (eventBus.lifecycleObserverAlwaysActive) Lifecycle.State.CREATED else Lifecycle.State.STARTED
        }

        override fun removeObserver(observer: Observer<in T>) {
            super.removeObserver(observer)
            if (eventBus.autoClear && !liveData.hasObservers()) {
                eventBus.events.remove(key)
                logger.log(Level.INFO, "observer removed: $observer")
            }
        }

    }

    internal class ObserverWrapper<T> internal constructor(private val observer: Observer<T>) :
            Observer<T> {

        internal var preventNextEvent = false

        override fun onChanged(@Nullable t: T) {
            if (preventNextEvent) {
                preventNextEvent = false
                return
            }
            logger.log(Level.INFO, "$this $observer :message received: $t")
            try {
                observer.onChanged(t)
            } catch (e: ClassCastException) {
                logger.log(Level.WARNING, "error on message received: $t", e)
            }
        }
    }

}


