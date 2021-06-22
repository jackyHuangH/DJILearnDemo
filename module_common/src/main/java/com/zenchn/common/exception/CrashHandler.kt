package com.zenchn.common.exception

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Process
import com.zenchn.common.SupportConfig
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author:Hzj
 * @date  :2020/9/15
 * desc  ：异常捕获处理
 * record：
 */
class CrashHandler(
    private val mContext: Context,
    private val crashConfig: ICrashConfig = DefaultCrashConfig(),
    private val crashCallback: ((Thread, Throwable) -> Unit)?
) : Thread.UncaughtExceptionHandler {

    /**
     * 初始化异常捕获
     */
    fun init(){
        //将当前实例设为系统默认的异常处理器
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    /**
     * 这个是最关键的函数，当程序中有未被捕获的异常，系统将会自动调用#uncaughtException方法
     * thread为出现未捕获异常的线程，ex为未捕获的异常，有了这个ex，我们就可以得到异常信息。
     */
    override fun uncaughtException(thread: Thread, ex: Throwable) {
        //打印出当前调用栈信息
        ex.printStackTrace()
        //如果重写了异常处理，否则就由我们自己结束自己
        if (crashConfig.getReportMode()) {
            try {
                //导出异常信息到SD卡中
                dumpExceptionToSDCard(ex)?.let {
                    //这里可以通过网络上传异常信息到服务器，便于开发人员分析日志从而解决bug
                    crashConfig.uploadExceptionToServer(it)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        crashCallback?.let { crashCallback.invoke(thread, ex) }
            ?: Process.killProcess(Process.myPid()) //结束进程
    }


    /**
     * 持久化异常信息
     *
     * @param ex
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun dumpExceptionToSDCard(ex: Throwable): File? {
        var logFile: File? = null

        //如果SD卡不存在或无法使用，则无法把异常信息写入SD卡
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            val dir: File = File(crashConfig.getFilePath())
            if (!dir.exists() || dir.isFile) {
                dir.mkdirs()
            }

            //获取奔溃时间（格式化）
            val crashTime = getCrashTime()

            //以当前时间创建log文件
            logFile = File(
                dir, crashConfig.getFileNamePrefix() + crashTime + crashConfig.getFileNameSuffix()
            )
            try {
                val pw = PrintWriter(BufferedWriter(FileWriter(logFile)))

                //导出发生异常的时间
                pw.println(crashTime)

                //导出手机信息
                dumpPhoneInfo(pw)
                pw.println()

                //导出异常的调用栈信息
                ex.printStackTrace(pw)
                pw.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return logFile
    }

    /**
     * 获取奔溃时间
     *
     * @return
     */
    private fun getCrashTime(): String {
        var crashTime = ""
        val current = System.currentTimeMillis()
        try {
            if (crashConfig.getDateFormat().isNotEmpty()) {
                crashTime = SimpleDateFormat(
                    crashConfig.getDateFormat(),
                    Locale.CHINA
                ).format(Date(current))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            crashTime =
                SimpleDateFormat(SupportConfig.FILE_DATE_FORMAT, Locale.CHINA).format(Date(current))
        }
        return crashTime
    }

    /**
     * 持久化异常信息
     *
     * @param printWriter
     * @throws NameNotFoundException
     */
    @Throws(PackageManager.NameNotFoundException::class)
    private fun dumpPhoneInfo(printWriter: PrintWriter) {
        printWriter.append("================Build================\n")
        printWriter.append(String.format("BOARD\t%s\n", Build.BOARD))
        printWriter.append(String.format("BOOTLOADER\t%s\n", Build.BOOTLOADER))
        printWriter.append(String.format("BRAND\t%s\n", Build.BRAND))
        printWriter.append(String.format("CPU_ABI\t%s\n", Build.CPU_ABI))
        printWriter.append(String.format("CPU_ABI2\t%s\n", Build.CPU_ABI2))
        printWriter.append(String.format("DEVICE\t%s\n", Build.DEVICE))
        printWriter.append(String.format("DISPLAY\t%s\n", Build.DISPLAY))
        printWriter.append(String.format("FINGERPRINT\t%s\n", Build.FINGERPRINT))
        printWriter.append(String.format("HARDWARE\t%s\n", Build.HARDWARE))
        printWriter.append(String.format("HOST\t%s\n", Build.HOST))
        printWriter.append(String.format("ID\t%s\n", Build.ID))
        printWriter.append(String.format("MANUFACTURER\t%s\n", Build.MANUFACTURER))
        printWriter.append(String.format("MODEL\t%s\n", Build.MODEL))
        printWriter.append(String.format("SERIAL\t%s\n", Build.SERIAL))
        printWriter.append(String.format("PRODUCT\t%s\n", Build.PRODUCT))
        printWriter.append("================APP================\n")

        //应用的版本名称和版本号
        if (mContext != null) {
            val pm: PackageManager = mContext.getPackageManager()
            val packageInfo: PackageInfo =
                pm.getPackageInfo(mContext.getPackageName(), PackageManager.GET_ACTIVITIES)
            val versionCode = packageInfo.versionCode
            val versionName = packageInfo.versionName
            printWriter.append(String.format("versionCode\t%s\n", versionCode))
            printWriter.append(String.format("versionName\t%s\n", versionName))
        }
        printWriter.append("================Exception================\n")
    }
}

interface ICrashConfig {
    fun getReportMode(): Boolean

    fun getFilePath(): String

    fun getFileNamePrefix(): String

    fun getDateFormat(): String

    fun getFileNameSuffix(): String

    fun uploadExceptionToServer(logFile: File)
}

class DefaultCrashConfig : ICrashConfig {
    override fun getReportMode(): Boolean = SupportConfig.isReport

    override fun getFilePath(): String = SupportConfig.FILE_PATH

    override fun getFileNamePrefix(): String = SupportConfig.FILE_NAME_PREFIX

    override fun getDateFormat(): String = SupportConfig.FILE_DATE_FORMAT

    override fun getFileNameSuffix(): String = SupportConfig.FILE_NAME_SUFFIX

    override fun uploadExceptionToServer(logFile: File) {
        // Upload Exception Message To Your Web Server.
    }
}