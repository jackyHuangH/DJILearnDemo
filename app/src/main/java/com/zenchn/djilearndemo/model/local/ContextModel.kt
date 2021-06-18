package com.zenchn.djilearndemo.model.local

import android.app.Application
import android.content.Context
import com.zenchn.djilearndemo.app.ApplicationKit
import io.reactivex.Observable

/**
 * @author:Hzj
 * @date  :2018/12/20/020
 * desc  ：提供applicationContext
 * record：
 */
object ContextModel {

    /**
     * 获取Context
     */
    fun getApplicationContext(): Context {
        return ApplicationKit.mApplicationContext as Context
    }

    fun getApplicationContextObservable(): Observable<Application> {
        val application = getApplicationContext() as Application
        return Observable.just(application)
    }
}