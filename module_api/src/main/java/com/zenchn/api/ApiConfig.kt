package com.zenchn.api

import androidx.annotation.StringRes
import com.zenchn.common.ext.isNotNullAndNotEmpty

/**
 * 配置
 */
internal object ApiConfig {

    const val URL_REGEX =
            "^(?:([A-Za-z]+):)?(\\/{0,3})([0-9.\\-A-Za-z]+)(?::(\\d+))?(?:\\/([^?#]*))?(?:\\?([^#]*))?(?:#(.*))?\$"

    const val API_MESSAGE_KEY = "message"
    const val API_SUCCESS_STATUS_CODE = 1//成功 默认 1，其他情况另定
    const val MAX_RETRY_COUNT = 3

    const val CONNECT_TIMEOUT = 15L
    const val READ_TIMEOUT = 20L
    const val WRITE_TIMEOUT = 20L

    const val DEFAULT_PAGE_NO = 1
    const val DEFAULT_PAGE_SIZE = 10

    const val META_DATA_BASE_HOST_KEY = "BASE_HOST_KEY"
    const val META_DATA_REST_API_PREFIX_KEY = "REST_API_PREFIX_KEY"
    const val HOST_FILE = "AppHost"

}

/**
 * OAuth授权配置
 */
internal interface OAuthConfig {
    companion object {
        const val CLIENT_ID = "00000000000000000000000000000001"
        const val CLIENT_SECRET = "monitor_manage_app"
        const val GRANT_TYPE = "password"
        const val REFRESH_TYPE = "refresh_token"
        const val DEVICE_TYPE_ANDROID = "Android"
        const val ACCESS_TOKEN = "access-token"
        const val APP_DEVICE_ID = "app-device-id"
        const val APP_DEVICE_NAME = "app-device-name"
        const val APP_DEVICE_TYPE = "app-device-type"

        //用户可登录权限
        const val USER_AUTHORITY_KEY_LOGIN = "*"
    }
}


/**
 * 常用响应码
 */
internal object ResponseCode {

    const val CODE_200 = 200//请求成功

    const val CODE_400 = 400//错误请求
    const val CODE_401 = 401//未授权
    const val CODE_403 = 403//请求禁止
    const val CODE_404 = 404//未找到页面
    const val CODE_405 = 405//方法禁用
    const val CODE_408 = 408//请求超时

    const val CODE_500 = 500//服务器内部错误
    const val CODE_502 = 502//错误网关
    const val CODE_503 = 503//服务不可用
}

/**
 * 消息
 */
internal object Message {

    val DEFAULT_EXCEPTION
        get() = getString(R.string.common_api_message_default_exception) ?: "系统升级维护中，请稍后再试"
    val PROXY_EXCEPTION
        get() = getString(R.string.common_api_message_proxy_unavailable) ?: "连接失败，请检查你的wifi代理"
    val NOT_FOUND_EXCEPTION
        get() = getString(R.string.common_api_message_404_not_found) ?: "找不到访问资源，请稍后再试"
    val AUTH_REFUSE_EXCEPTION
        get() = getString(R.string.common_api_message_auth_exception) ?: "授权失败，请重新登录"
    val CONNECT_EXCEPTION
        get() = getString(R.string.common_api_message_connect_exception) ?: "连接服务器失败，请稍后再试"
    val SOCKET_TIMEOUT_EXCEPTION
        get() = getString(R.string.common_api_message_socket_timeout_exception) ?: "连接服务器超时，请稍后再试"
    val NETWORK_NOT_AVAILABLE
        get() = getString(R.string.common_api_message_network_not_available) ?: "网络错误，请检查您的网络"

    private fun getString(@StringRes idRes: Int): String? {
        return ApiManager.contextRef.get()?.resources?.getString(idRes)
    }
}

/**
 * 对接口数据兼容处理
 */
internal object SupportUtils {

    private const val DELIMIT_FLAG = ","

    /**
     * 聚合成一个参数
     */
    fun <T> List<T?>?.groupParams(convert: (T) -> String?): String {
        return StringBuffer().apply {
            this@groupParams?.run {
                val lastIndex = size - 1
                forEachIndexed { index, t ->
                    t?.let {
                        convert(it).let { param ->
                            if (param.isNotNullAndNotEmpty()) {
                                append(param)
                                if (index < lastIndex) append(DELIMIT_FLAG)
                            }
                        }
                    }
                }
            }
        }.toString()
    }

    /**
     * 拆分成单个参数
     */
    fun <T> String?.splitParams(convert: (String) -> T): ArrayList<T> {
        return arrayListOf<T>().apply {
            this@splitParams?.split(DELIMIT_FLAG)?.forEach { url ->
                if (url.isNotNullAndNotEmpty()) {
                    add(convert(url))
                }
            }
        }
    }

    /**
     * 聚合成一个参数
     */
    fun <T> groupParams(vararg params: T, convert: (T) -> String?): String {
        return StringBuffer().apply {
            params.run {
                val lastIndex = params.size - 1
                forEachIndexed { index, t ->
                    t.let {
                        convert(it).let { param ->
                            if (param.isNotNullAndNotEmpty()) {
                                append(param)
                                if (index < lastIndex) append(DELIMIT_FLAG)
                            }
                        }
                    }
                }
            }
        }.toString()
    }

}


