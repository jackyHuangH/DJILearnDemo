package com.zenchn.common.eventbus

import android.annotation.SuppressLint
import android.app.Application

internal object AppUtils {

    @SuppressLint("StaticFieldLeak")
    @Volatile
    private var sApplication: Application? = null

    val applicationContext: Application?
        @SuppressLint("PrivateApi")
        get() {
            if (sApplication == null) {
                synchronized(AppUtils::class.java) {
                    if (sApplication == null) {
                        try {
                            sApplication = Class.forName("android.app.ActivityThread")
                                    .getMethod("currentApplication")
                                    .invoke(null, (null as Array<Any>?)) as Application
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                    }
                }
            }
            return sApplication
        }
}