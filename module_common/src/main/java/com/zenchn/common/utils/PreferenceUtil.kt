package com.zenchn.common.utils

import android.content.Context
import com.zenchn.common.ModuleManager
import java.io.*
import kotlin.reflect.KProperty

/**
 * @author:Hzj
 * @date  :2018/12/21/021
 * desc  ：kotlin 属性委托+SharedPreference 工具类
 * record：
 */
class PreferenceUtil<T>(val keyName: String, private val defaultValue: T) {

    companion object {
        private const val FILE_NAME = "SP_Config"

        //记录已登录用户access-token
        const val AUTH_TOKEN = "access_token"
        const val USER_PWD = "USER_PWD"//密码
        const val USER_NAME = "USER_NAME"//用户名
        const val WHETHER_NEW_USER = "WHETHER_NEW_USER"//第一次安装启动App
        const val REMEMBER_PASSWORD = "REMEMBER_PASSWORD"//记住密码
        const val AUTO_LOGIN = "AUTO_LOGIN"//自动登录
        const val AIRCRAFT_UPLOAD_FREQ = "AIRCRAFT_UPLOAD_FREQ"//无人机信息上报频率

        private val mPreference by lazy {
            ModuleManager.getContext().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        }

        /**
         * 退出登录后，清除token缓存
         */
        fun clearAccessToken() {
            removePreferenceByKey(AUTH_TOKEN)
        }

        /**
         * 根据key移除SP数据
         */
        fun removePreferenceByKey(key: String) {
            mPreference.edit().remove(key).apply()
        }

        /**
         * 清空SP数据
         */
        fun clearAllPreference() {
            mPreference.edit().clear().apply()
        }

        /**
         * 查询是否存在key
         */
        fun contains(key: String): Boolean {
            return mPreference.contains(key)
        }

        /**
         * 取出SP中所有键值对
         */
        fun getAllMap(): Map<String, *> {
            return mPreference.all
        }
    }

    //属性委托定义set,get
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return getPreference(keyName, defaultValue)
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        putPreference(keyName, value)
    }

    /**
     * 从SP取出
     */
    @Suppress("UNCHECKED_CAST")
    private fun getPreference(key: String, default: T): T = with(mPreference) {
        val res: Any = when (default) {
            is Long -> getLong(key, default)
            is String -> getString(key, default) ?: ""
            is Int -> getInt(key, default)
            is Boolean -> getBoolean(key, default)
            is Float -> getFloat(key, default)
            else -> deSerialization(getString(key, serialize(default)) ?: "")
        }
        return res as T
    }

    /**
     * 存入SP
     */
    private fun putPreference(key: String, value: T) = with(mPreference.edit()) {
        when (value) {
            is Long -> putLong(key, value)
            is String -> putString(key, value)
            is Int -> putInt(key, value)
            is Boolean -> putBoolean(key, value)
            is Float -> putFloat(key, value)
            else -> putString(key, serialize(value))
        }.apply()
    }

    /**
     * 序列化对象

     * @param person
     * *
     * @return
     * *
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun <A> serialize(obj: A): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val objectOutputStream = ObjectOutputStream(
            byteArrayOutputStream
        )
        objectOutputStream.writeObject(obj)
        var serStr = byteArrayOutputStream.toString("ISO-8859-1")
        serStr = java.net.URLEncoder.encode(serStr, "UTF-8")
        objectOutputStream.close()
        byteArrayOutputStream.close()
        return serStr
    }

    /**
     * 反序列化对象

     * @param str
     * *
     * @return
     * *
     * @throws IOException
     * *
     * @throws ClassNotFoundException
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IOException::class, ClassNotFoundException::class)
    private fun <A> deSerialization(str: String): A {
        val redStr = java.net.URLDecoder.decode(str, "UTF-8")
        val byteArrayInputStream = ByteArrayInputStream(
            redStr.toByteArray(charset("ISO-8859-1"))
        )
        val objectInputStream = ObjectInputStream(
            byteArrayInputStream
        )
        val obj = objectInputStream.readObject() as A
        objectInputStream.close()
        byteArrayInputStream.close()
        return obj
    }
}