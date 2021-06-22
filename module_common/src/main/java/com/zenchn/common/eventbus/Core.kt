package com.zenchn.common.eventbus

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.CallSuper
import androidx.lifecycle.*
import java.util.concurrent.ConcurrentHashMap

//data class LiveBusEntity<T>(val t: T)

object LiveEventBus {

    operator fun <T> get(type: Class<T>): Event<T>? {
        return LiveEventBus[type.name, type]
    }

    operator fun <T> get(channel: String, type: Class<T>): Event<T>? {
        return LiveEventBusCore.get().with("${channel}:${type.name}")
    }

    operator fun get(key: String): Event<Any>? {
        return get(key, Any::class.java)
    }

    fun config(block: Config.() -> Unit) {
        return LiveEventBusCore.get().config.block()
    }

    fun clearAll() {
        LiveEventBusCore.get().events.clear()
    }

}

fun <T> LifecycleOwner.registerLiveEventBus(channel: String, type: Class<T>, sticky: Boolean, observer: Observer<T>) {

    lifecycle.addObserver(object : LifecycleObserver {

        @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
        fun onCreate(owner: LifecycleOwner) {
            LiveEventBus[channel, type]?.observeForever(sticky, observer)
        }

        @CallSuper
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy(owner: LifecycleOwner) {
            LiveEventBus[channel, type]?.removeObserver(observer)
        }
    })

}

internal class LiveEventBusCore private constructor() {

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    internal val events: MutableMap<String, LiveEvent<Any>> = ConcurrentHashMap()

    internal var lifecycleObserverAlwaysActive = true
    internal var autoClear = false
    internal var config = Config()
    internal var ipcManager: IpcManager? = null

//    @Synchronized
//    fun <T> with(key: String, type: Class<T>): Event<T>? {
//        if (!events.containsKey(key)) {
//            events[key] = LiveEvent(key)
//        }
//        return events[key] as? Event<T>
//    }

    @Synchronized
    fun <T> with(key: String): Event<T>? {
        if (!events.containsKey(key)) {
            events[key] = LiveEvent(key)
        }
        return events[key] as? Event<T>
    }

    internal fun sendToTarget(delay: Long = 0, runnable: () -> Unit) {
        mainHandler.postDelayed(runnable, delay)
    }

    internal fun sendBroadcast(key: String, value: Any, foreground: Boolean) {
        try {
            ipcManager?.apply {
                createIntent(key, value, foreground)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {

        private val DEFAULT_BUS = LiveEventBusCore()

        fun get(): LiveEventBusCore {
            return DEFAULT_BUS
        }
    }
}

class Config {

    fun lifecycleObserverAlwaysActive(active: Boolean) {
        LiveEventBusCore.get().lifecycleObserverAlwaysActive = active
    }

    fun autoClear(clear: Boolean) {
        LiveEventBusCore.get().autoClear = clear
    }

    fun supportBroadcast(context: Context, converter: JSONConverter? = null) {
        LiveEventBusCore.get().ipcManager = IpcManager(converter).apply {
            registerReceiver(context)
        }
    }

    fun addConverter(converter: JSONConverter) {
        LiveEventBusCore.get().ipcManager?.converter = converter
    }

    fun openDebug(debug: Boolean, tag: String? = DefaultLogger.TAG) {

    }
}
