package com.zenchn.api.frame

import com.google.gson.JsonParser
import com.zenchn.api.ApiConfig
import com.zenchn.api.ResponseCode
import com.zenchn.api.entity.ResponseModel
import kotlinx.coroutines.Deferred
import okhttp3.ResponseBody

/**
 * 提取返回数据中的信息
 *
 * @return
 */
internal fun ResponseBody.obtainApiMessage(): String? {
    return try {
        JsonParser().parse(this.string()).asJsonObject.get(ApiConfig.API_MESSAGE_KEY).asString
    } catch (e: Exception) {
        null
    }
}

/**
 * 判断api是否访问成功
 *
 * @return
 */
fun ResponseModel<*>?.isApiSuccess(): Boolean = this?.let { it.status == ResponseCode.CODE_200 } ?: false

fun ResponseModel<*>?.orError() = this ?: ERROR_RESPONSE

fun <T> ResponseModel<T>?.observe(callback: (Boolean, T?, String?) -> Unit) {
    if (this != null && isApiSuccess()) {
        callback.invoke(true, data, null)
    } else {
        callback.invoke(false, null, this?.message)
    }
}

//suspend fun <T> Deferred<ResponseModel<T>?>?.observe(callback: (Boolean, T?, String?) -> Unit) {
//    this?.await().observe(callback)
//}

suspend fun <T> Deferred<ResponseModel<T>?>?.observe(callback: (Boolean, T?, String?) -> Unit) {
    try {
        this?.await().observe(callback)
    } catch (e: Exception) {
        callback.invoke(false, null, e.message())
        e.printStackTrace()
    }
}

internal fun Exception.errorResponse(): ResponseModel<Any> {
    return ResponseModel<Any>(
        status = ResponseCode.CODE_400,
        message = message(),
        data = null
    )
}


internal val ERROR_RESPONSE: ResponseModel<Any> = ResponseModel<Any>(
    status = ResponseCode.CODE_400,
    message = null,
    data = null
)

internal val OK_RESPONSE: ResponseModel<Any> = ResponseModel<Any>(
    status = ResponseCode.CODE_200,
    message = null,
    data = null
)

