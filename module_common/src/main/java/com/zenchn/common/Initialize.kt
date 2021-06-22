package com.zenchn.common

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import com.hjq.toast.ToastUtils
import com.zenchn.common.utils.LoggerKit


class CommonInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        context.let {
            ModuleManager.init(it)
            ToastUtils.init(context as Application)
            LoggerKit.init(SupportConfig.DEFAULT_TAG)
        }
        Log.d("Init", "CommonInitializer-create")
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

internal object ModuleManager {

    private lateinit var mContext: Context

    fun init(context: Context) {
        context.applicationContext.apply {
            mContext = this
        }
    }

    internal fun getContext(): Context {
        return mContext
    }
}
