package  com.zenchn.update

import android.app.Application
import android.content.Context
import android.widget.Toast
import com.pgyersdk.Pgyer
import com.pgyersdk.PgyerActivityManager
import com.pgyersdk.crash.PgyCrashManager
import com.pgyersdk.update.DownloadFileListener
import com.pgyersdk.update.PgyUpdateManager
import com.pgyersdk.update.UpdateManagerListener
import com.pgyersdk.update.javabean.AppBean
import com.zenchn.common.ext.checkNotNull
import com.zenchn.common.utils.getMetaData
import com.zenchn.update.Config.META_DATA_PGY_APP_ID
import java.io.File

internal object PGYUpdateManager {

    fun init(context: Context) {
        try {
            context.applicationContext.apply {
                PgyerActivityManager.set(this as Application)
                PgyCrashManager.register()
                val appId = getMetaData(META_DATA_PGY_APP_ID).checkNotNull { "请在Manifest.xml中配置PGY_APPID" }
                Pgyer.setAppId(appId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun autoUpdate(silent: Boolean = true) {

        PgyUpdateManager
                .Builder()
                .setForced(true)                //设置是否强制更新,非自定义回调更新接口此方法有用
                .setUserCanRetry(true)         //失败后是否提示重新下载，非自定义下载 apk 回调此方法有用
                .setDeleteHistroyApk(true)     // 检查更新前是否删除本地历史 Apk
                .setUpdateManagerListener(object : UpdateManagerListener {
                    override fun onNoUpdateAvailable() {
                        //没有更新是回调此方法
                        if (!silent) {
                            UpdateManager.getContext()?.let {
                                Toast.makeText(it, "没有发现更新的版本！", Toast.LENGTH_LONG).show()
                            }
                        }
                    }

                    override fun onUpdateAvailable(appBean: AppBean) {
                        //没有更新是回调此方法
                        if (!silent) {
                            UpdateManager.getContext()?.let {
                                Toast.makeText(it, "发现新的版本:${appBean.versionCode}", Toast.LENGTH_LONG)
                                        .show()
                            }
                        }

                        //调用以下方法，DownloadFileListener 才有效；如果完全使用自己的下载方法，不需要设置DownloadFileListener
                        PgyUpdateManager.downLoadApk(appBean.downloadURL)
                    }

                    override fun checkUpdateFailed(e: Exception) {
                        //更新检测失败回调

                    }
                })
                //注意 ：下载方法调用 PgyUpdateManager.downLoadApk(appBean.getDownloadURL()); 此回调才有效
                .setDownloadFileListener(object : DownloadFileListener {

                    override fun onProgressUpdate(vararg p0: Int?) {
                    }   // 使用蒲公英提供的下载方法，这个接口才有效。

                    override fun downloadFailed() {
                        //下载失败
                    }

                    override fun downloadSuccessful(file: File?) {
                        try {
                            PgyUpdateManager.installApk(file)  // 使用蒲公英提供的安装方法提示用户 安装apk

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                })
                .register()
    }

}