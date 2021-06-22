package com.zenchn.common.eventbus

import android.util.Log
import com.zenchn.common.BuildConfig
import java.util.logging.Level

interface Logger {

    fun log(level: Level, msg: String)

    fun log(level: Level, msg: String, th: Throwable)
}

internal class DefaultLogger(var debug: Boolean = BuildConfig.DEBUG) : Logger {

    override fun log(level: Level, msg: String) {
        if (debug) {
            when {
                level === Level.SEVERE -> Log.e(TAG, msg)
                level === Level.WARNING -> Log.w(TAG, msg)
                level === Level.INFO -> Log.i(TAG, msg)
                level === Level.CONFIG -> Log.d(TAG, msg)
                level !== Level.OFF -> Log.v(TAG, msg)
            }
        }
    }

    override fun log(level: Level, msg: String, th: Throwable) {
        if (debug) {
            when {
                level === Level.SEVERE -> Log.e(TAG, msg, th)
                level === Level.WARNING -> Log.w(TAG, msg, th)
                level === Level.INFO -> Log.i(TAG, msg, th)
                level === Level.CONFIG -> Log.d(TAG, msg, th)
                level !== Level.OFF -> Log.v(TAG, msg, th)
            }
        }
    }

    companion object {

        internal const val TAG = "[LiveEventBus]"
    }

}