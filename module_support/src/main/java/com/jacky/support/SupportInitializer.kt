package com.jacky.support

import android.app.Application
import android.content.Context
import androidx.startup.Initializer
import com.hjq.toast.ToastUtils
import com.jacky.support.utils.LoggerKit

/**
 * @author:Hzj
 * @date  :2021/6/7
 * desc  ：
 * record：
 */
class SupportInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        ToastUtils.init(context as Application)
        LoggerKit.init(SupportConfig.DEFAULT_TAG)
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf()

}