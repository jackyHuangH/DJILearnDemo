package com.zenchn.update

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import com.pgyersdk.crash.PgyCrashManager
import com.tencent.bugly.beta.Beta
import com.tencent.bugly.crashreport.CrashReport
import com.zenchn.common.ext.safelyRun
import java.lang.ref.WeakReference


class UpdateInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        context.let { UpdateManager.init(it) }
        Log.d("Init", "UpdateInitializer-create")
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

internal object Config {

    const val META_DATA_TINKER_APP_ID = "BUGLY_APPID"
    const val META_DATA_PGY_APP_ID = "PGY_APPID"
}

object UpdateManager {

    private var contextReference: WeakReference<Context>? = null

    internal fun init(context: Context) {
        context.applicationContext.apply {

            contextReference = WeakReference(this)

            //蒲公英初始化
//            PGYUpdateManager.init(this)

            //Bugly Sdk初始化
//            BuglyManager.init(this)
        }

    }

    internal fun getContext(): Context? {
        return contextReference?.get()
    }

    fun checkUpdate(isSilent: Boolean = false, isManual: Boolean = true) {
        if (isSilent) {
            //Bugly 检查更新
            Beta.checkUpgrade(isManual, isSilent)
        }
        //蒲公英更新
        PGYUpdateManager.autoUpdate(isSilent)
    }

}

object CrashManager {

    /**
     * 异常上报
     */
    fun postCrash(thread: Thread, ex: Throwable) {
        safelyRun {
            PgyCrashManager.reportCaughtException(Exception(ex))
            CrashReport.postCatchedException(ex)
        }
    }

}
