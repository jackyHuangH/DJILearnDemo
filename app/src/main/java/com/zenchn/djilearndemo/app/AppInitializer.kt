package com.zenchn.djilearndemo.app

import android.content.Context
import androidx.startup.Initializer
import com.jacky.support.SupportInitializer

/**
 * @author:Hzj
 * @date  :2021/6/7
 * desc  ：App startup
 * record：注意：先执行这里的create,最后执行application的create
 */
class AppInitializer:Initializer<Unit> {
    override fun create(context: Context) {
        //do initialize
        ApplicationKit.initKit(context)
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf(SupportInitializer::class.java)
    }
}