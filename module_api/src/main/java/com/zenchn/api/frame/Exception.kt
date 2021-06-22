package com.zenchn.api.frame

import com.zenchn.api.ApiConfig
import com.zenchn.api.ApiManager
import com.zenchn.api.Message
import com.zenchn.api.ResponseCode
import com.zenchn.common.BuildConfig
import com.zenchn.common.ext.safelyRun
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException


internal class ApiException(val msg: String?) : RuntimeException(msg) {

    override val message: String
        get() = "ApiException message : $msg"

}


interface IApiExceptionReporter {

    fun report(throwable: Throwable)

}

/**
 * 处理重试
 *
 * @param retryCount
 * @return
 */
internal fun Throwable.handleApiRetry(@NotNull retryCount: Int): Boolean {
    if (this is HttpException) {
        val code = code()
        if (ResponseCode.CODE_400 == code || ResponseCode.CODE_401 == code) {
            return false
        }
    }
    return this !is ApiException && retryCount <= ApiConfig.MAX_RETRY_COUNT
}

/**
 * 提取普通接口异常中包含的错误信息
 *
 * @param defaultErrorMsg
 * @return
 */
fun Throwable.dispatch(
    @Nullable defaultErrorMsg: String = Message.DEFAULT_EXCEPTION,
    @Nullable debug: Boolean = BuildConfig.DEBUG,
    msgResult: (String) -> Unit,
    refused: () -> Unit
) {

    if (ApiManager.checkNetworkAvailable()) {
        when (this) {
            is ConnectException -> msgResult.invoke(Message.CONNECT_EXCEPTION)
            is SocketTimeoutException -> msgResult.invoke(Message.SOCKET_TIMEOUT_EXCEPTION)
            is ApiException -> msgResult.invoke(msg ?: defaultErrorMsg)
            is HttpException -> {
                when (code()) {
                    ResponseCode.CODE_401 -> {
                        msgResult.invoke(Message.AUTH_REFUSE_EXCEPTION)
                        refused.invoke()
                    }
                    ResponseCode.CODE_404 -> msgResult.invoke(Message.NOT_FOUND_EXCEPTION)
                    else -> {
                        msgResult.invoke(
                                safelyRun(
                                        catch = { if (debug) message() },
                                        runnable = { response()?.errorBody()?.obtainApiMessage() }
                                ) ?: defaultErrorMsg
                        )
                    }
                }
            }
            else -> msgResult.invoke(defaultErrorMsg)
        }
    } else {
        msgResult.invoke(Message.NETWORK_NOT_AVAILABLE)
    }

}

fun Throwable.message(@Nullable defaultErrorMsg: String = Message.DEFAULT_EXCEPTION, @Nullable debug: Boolean = BuildConfig.DEBUG): String {
    return if (ApiManager.checkNetworkAvailable()) {
        when (this) {
            is ConnectException -> {
                if (ApiManager.isWifiProxy()) Message.PROXY_EXCEPTION
                else Message.CONNECT_EXCEPTION
            }
            is SocketTimeoutException -> Message.SOCKET_TIMEOUT_EXCEPTION
            is ApiException -> msg ?: defaultErrorMsg
            is HttpException -> {
                when (code()) {
                    ResponseCode.CODE_401 -> Message.AUTH_REFUSE_EXCEPTION
                    ResponseCode.CODE_404 -> Message.NOT_FOUND_EXCEPTION
                    else -> {
                        safelyRun(
                                catch = { if (debug) message() },
                                runnable = { response()?.errorBody()?.obtainApiMessage() }
                        ) ?: defaultErrorMsg
                    }
                }
            }
            else -> defaultErrorMsg
        }
    } else {
        Message.NETWORK_NOT_AVAILABLE
    }
}

/**
 * 提取接口异常中包含的错误信息
 *
 * @param defaultErrorMsg
 * @return
 */
private fun HttpException.errorMessage(
        @Nullable defaultErrorMsg: String = Message.DEFAULT_EXCEPTION,
        @Nullable debug: Boolean = BuildConfig.DEBUG
): String {
    return try {
        response()?.errorBody()?.obtainApiMessage()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } ?: (if (debug) message() else defaultErrorMsg)
}