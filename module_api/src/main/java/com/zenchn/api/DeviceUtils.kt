package com.zenchn.api

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresPermission
import java.io.Serializable

/**
 * 描    述：
 * 修订记录：
 * @author hzj
 */
object DeviceUtils {
    private lateinit var mContext: Context

    /**
     * 使用前请在Application中初始化
     *
     * @param context
     */
    fun init(context: Context) {
        mContext = context
    }

    // 返回手机唯一标示
    //返回手机型号-系统版本
    /**
     * 用户登陆令牌授权时获取用户信息(获取令牌)
     *
     * @return
     */
    fun getClientInfo(): DeviceInfoDO {
        return DeviceInfoDO().apply {
            // 返回手机唯一标示
            deviceId = getAndroidId()
            //返回手机型号
            deviceModel = Build.MODEL
            //返回手机系统版本
            deviceAndroidSdk = sdkVersionName
        }
    }

    fun getAndroidId(): String {
        return try {
            if (mContext == null) {
                throw RuntimeException("please init this in application!")
            }
            val contentResolver = mContext!!.contentResolver
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            ""
        }
    }


    /**
     * Return the version name of device's system.
     *
     * @return the version name of device's system
     */
    val sdkVersionName: String
        get() = Build.VERSION.RELEASE

    /**
     * Return version code of device's system.
     *
     * @return version code of device's system
     */
    val sdkVersionCode: Int
        get() = Build.VERSION.SDK_INT

    /**
     * Skip to dial.
     *
     * @param phoneNumber The phone number.
     */
    fun dial(from: Activity, phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        val data = Uri.parse("tel:$phoneNumber")
        intent.data = data
        from.startActivity(intent)
    }

    /**
     * Make a phone call.
     *
     * Must hold `<uses-permission android:name="android.permission.CALL_PHONE" />`
     *
     * @param phoneNum The phone number.
     */
    @RequiresPermission(permission.CALL_PHONE)
    fun call(from: Activity, phoneNum: String) {
        val intent = Intent(Intent.ACTION_CALL)
        val data = Uri.parse("tel:$phoneNum")
        intent.data = data
        from.startActivity(intent)
    }

    /**
     * Send sms.
     *
     * @param phoneNumber The phone number.
     * @param msgBody     The content.
     */
    fun sendSms(from: Activity, phoneNumber: String, msgBody: String?) {
        val smsToUri = Uri.parse("smsto:$phoneNumber")
        val intent = Intent(Intent.ACTION_VIEW, smsToUri)
            .putExtra("sms_body", msgBody)
        from.startActivity(intent)
    }
}

/**
 * 设备授权信息
 */
data class DeviceInfoDO(
    var username: String = "",
    var password: String = "",
    var deviceId: String = "",
    var deviceModel: String = "",
    var deviceAndroidSdk: String = ""
) : Serializable