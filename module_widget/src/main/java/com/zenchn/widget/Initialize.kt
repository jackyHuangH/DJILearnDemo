package com.zenchn.widget

import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.util.Log
import androidx.startup.Initializer
import com.hjq.toast.ToastUtils
import com.tencent.smtt.sdk.QbSdk
import com.zenchn.common.ext.safelyRun
import java.lang.ref.WeakReference

class WidgetInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        context.let { WidgetManager.init(it) }
        Log.d("Init", "WidgetInitializer-create")
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

object WidgetManager {

    internal lateinit var contextWeakReference: WeakReference<Context>
    internal lateinit var resources: Resources

    internal fun init(context: Context) {

        resources = context.resources

        contextWeakReference = WeakReference(context)
        //x5内核初始化接口
        safelyRun { QbSdk.initX5Environment(context, null) }
        //ToastUtil 初始化
        ToastUtils.init(context as Application)
    }

}
