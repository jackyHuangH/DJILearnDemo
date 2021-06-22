package com.zenchn.djilearndemo.app

import android.content.Context
import androidx.startup.Initializer
import com.zenchn.api.ApiInitializer
import com.zenchn.common.CommonInitializer
import com.zenchn.map.MapInitializer
import com.zenchn.update.UpdateInitializer
import com.zenchn.widget.WidgetInitializer

/**
 * @author:Hzj
 * @date  :2021/6/7
 * desc  ：App startup
 * record：注意：先执行这里的create,最后执行application的create
 */
class AppInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        //do initialize
        ApplicationKit.initKit(context)
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf(
            ApiInitializer::class.java,
            CommonInitializer::class.java,
            MapInitializer::class.java,
            UpdateInitializer::class.java,
            WidgetInitializer::class.java
        )
    }
}