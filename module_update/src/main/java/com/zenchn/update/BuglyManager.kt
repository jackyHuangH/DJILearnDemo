package com.zenchn.update

import android.content.Context
import com.tencent.bugly.Bugly
import com.zenchn.common.BuildConfig
import com.zenchn.common.ext.checkNotNull
import com.zenchn.common.utils.getMetaData
import com.zenchn.update.Config.META_DATA_TINKER_APP_ID

internal object BuglyManager {

    fun init(context: Context) {
        try {
            // 设置开发设备，默认为false，上传补丁如果下发范围指定为“开发设备”，需要调用此接口来标识开发设备
            context.applicationContext.apply {
                val appId = getMetaData(META_DATA_TINKER_APP_ID).checkNotNull { "请在Manifest.xml中配置BUGLY_APPID" }
                Bugly.setIsDevelopmentDevice(this, false)
                Bugly.init(this, appId, BuildConfig.DEBUG)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}

