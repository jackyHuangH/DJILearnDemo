package com.zenchn.common.eventbus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Parcelable
import com.zenchn.common.utils.parseJSONObject
import com.zenchn.common.utils.toJSONString
import java.io.Serializable

private object IpcConst {

    const val ACTION = "intent.action.ACTION_LEB_IPC"
    const val KEY = "KEY"
    const val VALUE_TYPE = "VALUE_TYPE"
    const val VALUE = "VALUE"
    const val CLASS_NAME = "CLASS_NAME"
}

class DecodeException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)

class EncodeException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)

interface JSONConverter {

    @Throws(DecodeException::class)
    fun <T> decode(json: String, clazz: Class<T>): T

    @Throws(EncodeException::class)
    fun encode(any: Any): String

}

internal class DefaultJSONConverter : JSONConverter {

    override fun <T> decode(json: String, clazz: Class<T>): T {
        return json.parseJSONObject(clazz)
            ?: throw DecodeException("Decode error by:json=$json clazz:${clazz.simpleName}")

    }

    override fun encode(any: Any): String {
        return any.toJSONString() ?: throw DecodeException("Encode error by:${any.toString()}")
    }

}

internal class IpcManager(var converter: JSONConverter? = DefaultJSONConverter()) {

    private lateinit var receiver: IpcReceiver
    private var appContext: Context? = null

    fun createIntent(key: String, value: Any, foreground: Boolean = true): Intent {
        return Intent(IpcConst.ACTION).apply {
            if (foreground) addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            putExtra(IpcConst.KEY, key)
            encode(value, converter)
        }
    }

    fun sendBroadcast(intent: Intent, foreground: Boolean = true) {
        try {
            if (foreground) intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            appContext?.sendBroadcast(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun registerReceiver(context: Context?) {
        if (context != null) appContext = context.applicationContext
        receiver = IpcReceiver()
        appContext?.registerReceiver(receiver, IntentFilter().apply {
            addAction(IpcConst.ACTION)
        })
    }

}

class IpcReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (IpcConst.ACTION == intent.action) {
            val key = intent.getStringExtra(IpcConst.KEY)
            try {
                val value = intent.decode()
                if (key != null) {
                    LiveEventBus[key]?.post(value)
                }
            } catch (e: DecodeException) {
                e.printStackTrace()
            }
        }
    }
}

private fun Intent.encode(value: Any, converter: JSONConverter? = null) {
    when (value) {
        is String -> {
            putExtra(IpcConst.VALUE_TYPE, DataType.STRING.ordinal)
            putExtra(IpcConst.VALUE, value)
        }
        is Int -> {
            putExtra(IpcConst.VALUE_TYPE, DataType.INTEGER.ordinal)
            putExtra(IpcConst.VALUE, value)
        }
        is Boolean -> {
            putExtra(IpcConst.VALUE_TYPE, DataType.BOOLEAN.ordinal)
            putExtra(IpcConst.VALUE, value)
        }
        is Long -> {
            putExtra(IpcConst.VALUE_TYPE, DataType.LONG.ordinal)
            putExtra(IpcConst.VALUE, value)
        }
        is Float -> {
            putExtra(IpcConst.VALUE_TYPE, DataType.FLOAT.ordinal)
            putExtra(IpcConst.VALUE, value)
        }
        is Double -> {
            putExtra(IpcConst.VALUE_TYPE, DataType.DOUBLE.ordinal)
            putExtra(IpcConst.VALUE, value)
        }
        is Bundle -> {
            putExtra(IpcConst.VALUE_TYPE, DataType.BUNDLE.ordinal)
            putExtra(IpcConst.VALUE, value)
        }
        is Parcelable -> {
            putExtra(IpcConst.VALUE_TYPE, DataType.PARCELABLE.ordinal)
            putExtra(IpcConst.VALUE, value)
        }
        is Serializable -> {
            putExtra(IpcConst.VALUE_TYPE, DataType.SERIALIZABLE.ordinal)
            putExtra(IpcConst.VALUE, value)
        }
        else -> try {
            val json = converter?.encode(value)
            putExtra(IpcConst.VALUE_TYPE, DataType.JSON.ordinal)
            putExtra(IpcConst.VALUE, json)
            putExtra(IpcConst.CLASS_NAME, value.javaClass.canonicalName)
        } catch (e: Exception) {
            throw EncodeException(cause = e)
        }
    }
}

private fun Intent.decode(converter: JSONConverter? = null): Any {
    val valueTypeIndex = getIntExtra(IpcConst.VALUE_TYPE, -1)
    if (valueTypeIndex < 0) throw DecodeException("Index Error")
    return when (DataType.values()[valueTypeIndex]) {
        DataType.STRING -> getStringExtra(IpcConst.VALUE)
        DataType.INTEGER -> getIntExtra(IpcConst.VALUE, -1)
        DataType.BOOLEAN -> getBooleanExtra(IpcConst.VALUE, false)
        DataType.LONG -> getLongExtra(IpcConst.VALUE, -1)
        DataType.FLOAT -> getFloatExtra(IpcConst.VALUE, -1f)
        DataType.DOUBLE -> getDoubleExtra(IpcConst.VALUE, -1.0)
        DataType.PARCELABLE -> getParcelableExtra<Parcelable>(IpcConst.VALUE)
        DataType.SERIALIZABLE -> getSerializableExtra(IpcConst.VALUE)
        DataType.BUNDLE -> getBundleExtra(IpcConst.VALUE)
        DataType.JSON -> {
            try {
                val json = getStringExtra(IpcConst.VALUE)
                val className = getStringExtra(IpcConst.CLASS_NAME)
                return converter?.decode(json, Class.forName(className))
                    ?: throw DecodeException("converter Error")
            } catch (e: Exception) {
                throw DecodeException(cause = e)
            }
        }
        DataType.UNKNOWN -> throw DecodeException()
    }
}

private enum class DataType {

    STRING,
    INTEGER,
    BOOLEAN,
    LONG,
    FLOAT,
    DOUBLE,
    PARCELABLE,
    SERIALIZABLE,
    BUNDLE,
    JSON,
    UNKNOWN
}


