package com.zenchn.djilearndemo.model.api

import com.zenchn.djilearndemo.model.entity.BaseResponse
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONArray
import java.io.File
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type


/**
 * @author:Hzj
 * @date  :2020/6/8
 * desc  ：Api相关工具
 * record：
 */

object ResponseHandler {

    private val gson by lazy { Gson() }

    /**
     * 将Any 转换成 Map<String,T>
     */
    fun <T> objectToMap(obj: Any): Map<String, T> {
        val empMapType = object : TypeToken<Map<String, T>>() {}.type
        return gson.fromJson(objToString(obj), empMapType)
    }

    //obj 2 String
    fun objToString(obj: Any) = gson.toJson(obj)

    //从jsonObject解析
    fun <T> fromJsonObject(jsonObject: JsonObject, clazz: Class<T>): BaseResponse<T> {
        return fromJsonObject(objToString(jsonObject), clazz)
    }

    //从jsonArray解析
    fun <T> fromJsonArray(jsonArray: JSONArray, clazz: Class<T>): BaseResponse<List<T>> {
        return fromJsonArray(objToString(jsonArray), clazz)
    }

    //从jsonObject解析
    fun <T> fromJsonObject(jsonString: String, clazz: Class<T>): BaseResponse<T> {
        val type = ParameterizedTypeImpl(BaseResponse::class.java, arrayOf(clazz))
        return gson.fromJson(jsonString, type)
    }

    //从jsonArray解析
    fun <T> fromJsonArray(jsonString: String, clazz: Class<T>): BaseResponse<List<T>> {
        // 生成List<T> 中的 List<T>
        val listType: Type = ParameterizedTypeImpl(List::class.java, arrayOf(clazz))
        // 根据List<T>生成完整的Result<List<T>>
        val type: Type = ParameterizedTypeImpl(BaseResponse::class.java, arrayOf(listType))
        return gson.fromJson(jsonString, type)
    }
}

private class ParameterizedTypeImpl(
    private val raw: Class<out Any>,
    private val args: Array<Type>
) : ParameterizedType {

    override fun getRawType(): Type = raw

    override fun getOwnerType(): Type? = null

    override fun getActualTypeArguments(): Array<Type> = args
}

object RequestBodyProvider {

    //上传图片需要MultipartBody
    fun provideMultipartRequestBody(filePath: String): MultipartBody.Part {
        val file = File(filePath)
        val requestBody = RequestBody.create(MediaType.parse("image/jpg"), file)
        return MultipartBody.Part.createFormData("file", file.name, requestBody)
    }

    // 上传普通表单参数
    fun provideFormRequestBody(param: String): RequestBody {
        val mediaType = MediaType.parse("application/x-www-form-urlencoded")
        return RequestBody.create(mediaType, param)
    }

    //json请求体
    fun provideJsonRequestBody(content: String): RequestBody {
        return RequestBody.create(MediaType.parse("application/json;charset=UTF-8"), content)
    }
}