@file:Suppress("DEPRECATION")

package com.zenchn.common.utils

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.provider.Settings
import android.telephony.TelephonyManager
import com.zenchn.common.ext.isTrue
import com.zenchn.common.utils.NetWorkUtils.getActiveNetworkInfo
import com.zenchn.common.utils.NetWorkUtils.isNetworkAvailable

/**
 * 作   者：wangr on 2019/11/11 13:23
 * 描   述：
 * 修订记录：
 */
object NetWorkUtils {

    /**
     * 判断当前网络是否可用(是否可用)
     *
     * @return 获取网络信息实体
     * 由于从系统服务中获取数据属于进程间通信，基本类型外的数据必须实现Parcelable接口，
     * NetworkInfo实现了Parcelable，获取到的activeNetInfo相当于服务中网络信息实体对象的一个副本（拷贝），
     * 所以，不管系统网络服务中的实体对象是否置为了null，此处获得的activeNetInfo均不会发生变化
     */

    private fun Context.telephonyManager() =
        this.applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    private fun Context.connectivityManager() =
        this.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    /**
     * 获取活动网络信息
     *
     * @return NetworkInfo
     */
    internal fun Context.getActiveNetworkInfo(): NetworkInfo? =
        this.applicationContext.connectivityManager()?.activeNetworkInfo

    /**
     * 获取网络信息
     *
     * @return NetworkInfo
     */
    private fun Context.getNetworkInfo(networkType: Int): NetworkInfo? =
        this.applicationContext.connectivityManager()?.getNetworkInfo(networkType)

    /**
     * 判断网络是否可用
     * <p>需添加权限 {@code <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>}</p>
     *
     * @return {@code true}: 可用<br>{@code false}: 不可用
     */
    @Suppress("DEPRECATION")
    internal fun NetworkInfo?.isNetworkAvailable(): Boolean? = this?.let {
        it.isConnected && it.isAvailable
    }

    /**
     * 判断当前可用的网络是否是移动网络
     *
     * @param context
     * @return
     */
    @Suppress("DEPRECATION")
    fun isMoNetAvailable(context: Context): Boolean? = context.getActiveNetworkInfo()?.let {
        it.isNetworkAvailable().isTrue() && ConnectivityManager.TYPE_MOBILE == it.type
    }

    /**
     * 判断当前可用的网络是否是wifi网络
     *
     * @param context
     * @return
     */
    @Suppress("DEPRECATION")
    fun isWifiAvailable(context: Context): Boolean? = context.getActiveNetworkInfo()?.let {
        it.isNetworkAvailable().isTrue() && ConnectivityManager.TYPE_WIFI == it.type
    }

    /**
     * 判断是否wifi网络
     */
    @Suppress("DEPRECATION")
    fun isWifiEnabled(context: Context): Boolean =
        context.getNetworkInfo(ConnectivityManager.TYPE_WIFI)?.isConnected ?: false

    /**
     * 判断是否移动网络
     *
     * @param context
     * @return
     */
    @Suppress("DEPRECATION")
    fun isMoNetEnabled(context: Context): Boolean =
        context.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)?.isConnected ?: false

    /**
     * 获取移动网络运营商名称
     *
     * 如中国联通、中国移动、中国电信
     *
     * @param context 上下文
     * @return 移动网络运营商名称
     */
    fun getNetworkOperatorName(context: Context): String? =
        context.telephonyManager()?.networkOperatorName

    /**
     * 打开网络设置界面
     * <p>3.0以下打开设置界面</p>
     *
     * @param context 上下文
     */
    fun openWirelessSettings(context: Context) =
        context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))

}

fun Context?.isNetworkAvailable(): Boolean? = this?.getActiveNetworkInfo().isNetworkAvailable()