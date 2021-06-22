package com.zenchn.common.utils

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.nio.charset.Charset

/**
 * @author:Hzj
 * @date  :2020/9/21
 * desc  ：gson解析工具
 * record：
 */
object GSonUtils {
    val gson by lazy { Gson() }

    /**
     * Object转json
     *
     * @param obj
     * @return
     */
    fun toJson(obj: Any): String {
        return gson.toJson(obj)
    }

    /**
     * json转Object
     */
    fun <T> fromJson(json: String, cls: Class<T>): T {
        return gson.fromJson(json, cls)
    }

    /**
     * Json转List集合
     */
    fun <T> jsonToList(json: String, clz: Class<T>): List<T> {
        val type = object : TypeToken<List<T>>() {}.type
        return gson.fromJson(json, type)
    }

    /**
     * Json转List集合,遇到解析不了的，就使用这个
     */
    fun <T> fromJsonList(json: String, cls: Class<T>): List<T> {
        val mutableList: MutableList<T> = ArrayList()
        val array: JsonArray = JsonParser().parse(json).asJsonArray
        for (elem in array) {
            mutableList.add(gson.fromJson(elem, cls))
        }
        return mutableList
    }


    /**
     * Json转换成Map的List集合对象
     */
    fun <T> toListMap(json: String, clz: Class<T>): List<Map<String, T>> {
        val type = object : TypeToken<List<Map<String, T>>>() {}.type
        return gson.fromJson(json, type)
    }

    /**
     * Json转Map对象
     */
    fun <T> toMap(json: String, clz: Class<T>): Map<String, T> {
        val type = object : TypeToken<Map<String, T>>() {}.type
        return gson.fromJson(json, type)
    }

}

fun <T> String.parseJSONObject(clazz: Class<T>, charset: Charset = Charsets.UTF_8): T? {
    return try {
        GSonUtils.fromJson(this, clazz)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun Any.toJSONString(): String? {
    return try {
        GSonUtils.toJson(this)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}