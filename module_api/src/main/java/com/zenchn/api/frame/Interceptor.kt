package com.zenchn.api.frame

import com.zenchn.api.*
import com.zenchn.common.utils.PreferenceUtil
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 请求头拦截器
 */
internal class RequestHeaderInterceptor : Interceptor {
    private var accessTokenConf by PreferenceUtil(PreferenceUtil.AUTH_TOKEN, "")

    override fun intercept(chain: Interceptor.Chain): Response {
        //头部添加设备信息：deviceId,deviceName,deviceType
        val loginInfo = DeviceUtils.getClientInfo()
        return chain.proceed {
            var token = OAuthController.getAccessToken()
            if (token.isEmpty()) {
                token = accessTokenConf
            }
            header(OAuthConfig.ACCESS_TOKEN, token)
            header(OAuthConfig.APP_DEVICE_ID, loginInfo.deviceId)
            header(OAuthConfig.APP_DEVICE_NAME, loginInfo.deviceModel)
            header(OAuthConfig.APP_DEVICE_TYPE, "${OAuthConfig.DEVICE_TYPE_ANDROID} ${loginInfo.deviceAndroidSdk}")
        }
    }
}

/**
 * 网络状态拦截器
 */
internal class NetworkCheckInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (ApiManager.checkNetworkAvailable()) {
            val request = chain.request()
            return chain.proceed(request)
        } else {
            throw ApiException(Message.NETWORK_NOT_AVAILABLE)
        }
    }

}
