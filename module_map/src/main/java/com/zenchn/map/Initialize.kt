package com.zenchn.map

import android.content.Context
import android.util.Log
import androidx.startup.Initializer


class MapInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        context.let { MapManager.init(it) }
        Log.d("Init", "MapInitializer-create")
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

object MapManager {

    fun init(context: Context) {
        //初始化高德地图
    }

}
