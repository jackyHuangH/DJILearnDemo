package com.zenchn.common.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import java.io.File
import java.io.IOException
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.math.min

/**
 * apk工具类
 *
 * @author wangr
 */
object ApkUtils {

    object AppPackage {
        const val TENCENT_WEIXIN = "com.tencent.mm"
        const val BAIDU_MAP = "com.baidu.BaiduMap"
        const val GAODE_MAP = "com.autonavi.minimap"
        const val TENCENT_MAP = "com.tencent.map"
    }

    /**
     * 安装一个apk文件
     */
    fun install(context: Context, uriFile: File) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.fromFile(uriFile), "application/vnd.android.package-archive")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    /**
     * 卸载一个app
     */
    fun uninstall(context: Context, packageName: String) {
        // 通过程序的包名创建URI
        val packageURI = Uri.parse("package:$packageName")
        // 创建Intent意图
        val intent = Intent(Intent.ACTION_DELETE, packageURI)
        // 执行卸载程序
        context.startActivity(intent)
    }

    /**
     * 检查手机上是否安装了指定的软件
     */
    fun isAvailable(context: Context, packageName: String?): Boolean {
        // 获取packagemanager
        val packageManager = context.packageManager
        // 获取所有已安装程序的包信息
        val packageInfos = packageManager.getInstalledPackages(0)
        // 用于存储所有已安装程序的包名
        val packageNames = ArrayList<String>()
        // 从pinfo中将包名字逐一取出，压入pName list中
        if (packageInfos != null) {
            for (i in packageInfos.indices) {
                val packName = packageInfos[i].packageName
                packageNames.add(packName)
            }
        }
        // 判断packageNames中是否有目标程序的包名，有TRUE，没有FALSE
        return packageNames.contains(packageName)
    }

    /**
     * 检查手机上是否安装了指定的软件
     */
    fun isAvailable(context: Context, file: File): Boolean {
        return isAvailable(context, getPackageName(context, file.absolutePath))
    }

    /**
     * 根据文件路径获取包名
     */
    fun getPackageName(context: Context, filePath: String): String? {
        val packageManager = context.packageManager
        val info = packageManager.getPackageArchiveInfo(filePath, PackageManager.GET_ACTIVITIES)
        if (info != null) {
            val appInfo = info.applicationInfo
            return appInfo.packageName // 得到安装包名称
        }
        return null
    }

    /**
     * 从apk中获取版本信息
     */
    fun getChannelFromApk(context: Context, channelPrefix: String): String {
        // 从apk包中获取
        val appinfo = context.applicationInfo
        val sourceDir = appinfo.sourceDir
        // 默认放在meta-inf/里， 所以需要再拼接一下
        val key = "META-INF/$channelPrefix"
        var ret = ""
        var zipfile: ZipFile? = null
        try {
            zipfile = ZipFile(sourceDir)
            val entries = zipfile.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement() as ZipEntry
                val entryName = entry.name
                if (entryName.startsWith(key)) {
                    ret = entryName
                    break
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (zipfile != null) {
                try {
                    zipfile.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
        val split = ret.split(channelPrefix.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var channel = ""
        if (split.size >= 2) {
            channel = ret.substring(key.length)
        }
        return channel
    }

    /**
     * app版本名
     *
     * @param context
     * @return
     */
    fun getVersionName(context: Context): String {
        return getPackageInfo(context)!!.versionName
    }

    /**
     * app版本号
     *
     * @param context
     * @return
     */
    fun getVersionCode(context: Context): Int {
        return getPackageInfo(context)!!.versionCode
    }

    /**
     * app信息
     *
     * @param context
     * @return
     */
    private fun getPackageInfo(context: Context): PackageInfo? {
        var pi: PackageInfo? = null

        try {
            val pm = context.packageManager
            pi = pm.getPackageInfo(
                context.packageName,
                PackageManager.GET_CONFIGURATIONS
            )

            return pi
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return pi
    }

    /**
     * 比较版本号的大小,前者大则返回一个正数,后者大返回一个负数,相等则返回0   支持4.1.2,4.1.23.4.1.rc111这种形式
     *
     * @param version1
     * @param version2
     * @return
     */
    @Throws(Exception::class)
    fun compareVersion(version1: String?, version2: String?): Int {
        if (version1 == null || version2 == null) {
            throw Exception("compareVersion error:illegal params.")
        }
        val versionArray1 =
            version1.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()//注意此处为正则匹配，不能用"."；
        val versionArray2 = version2.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var idx = 0
        val minLength = min(versionArray1.size, versionArray2.size)//取最小长度值
        var diff = 0
        while (idx < minLength
            && ((versionArray1[idx].length - versionArray2[idx].length).let { diff = it;it == 0 })//先比较长度
            && ((versionArray1[idx].compareTo(versionArray2[idx])).let { diff = it;it == 0 })//再比较字符
        ) {
            ++idx
        }
        //如果已经分出大小，则直接返回，如果未分出大小，则再比较位数，有子版本的为大；
        diff = if (diff != 0) diff else versionArray1.size - versionArray2.size
        return diff
    }
}
