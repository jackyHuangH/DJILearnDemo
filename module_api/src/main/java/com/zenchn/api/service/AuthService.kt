package com.zenchn.api.service

import com.zenchn.api.ApiManager
import com.zenchn.api.DeviceUtils
import com.zenchn.api.OAuthConfig
import com.zenchn.api.entity.ResponseModel
import com.zenchn.api.entity.TokenEntity
import com.zenchn.api.frame.RequestBodyBuilder
import okhttp3.RequestBody
import retrofit2.http.*


private val authInternalService by lazy { ApiManager.create(AuthInternalService::class.java) }

val authService by lazy { ApiManager.create(AuthService::class.java) }

internal interface AuthInternalService {

    /**
     * 登录
     */
    @FormUrlEncoded
    @POST("oauth/token")
    suspend fun login(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("grant_type") grantType: String,
        @Field("username") username: String,
        @Field("password") password: String
    ): ResponseModel<TokenEntity>

    /**
     * 发送手机短信验证码
     */
    @POST("retrievePwd/sendSmsCaptcha")
    suspend fun sendSmsCaptcha(@Body body: RequestBody): ResponseModel<Any>

    /**
     * 忘记密码信息认证
     */
    @PUT("retrievePwd/authenticateModifyPwdSubmit")
    suspend fun authenticateModifyPwdSubmit(@Body body: RequestBody): ResponseModel<Any>
}

private const val LOGIN_SOURCE = "mobile"//标记登录设备

interface AuthService {

    /**
     * 退出登录,注销token
     */
    @GET("oauth/logout")
    suspend fun logout(): ResponseModel<Any>

    /**
     * 重置密码
     */
    @GET("retrievePwd/resetPwd")
    suspend fun resetPwd(
        @Query("accountName") account: String,
        @Query("password") password: String
    ): ResponseModel<Any>
}

internal suspend fun AuthService.login(
    loginName: String,
    encryptPwd: String
): ResponseModel<TokenEntity> {
    return DeviceUtils.getClientInfo().let {
        authInternalService.login(
            clientId = OAuthConfig.CLIENT_ID,
            clientSecret = OAuthConfig.CLIENT_SECRET,
            grantType = OAuthConfig.GRANT_TYPE,
            username = loginName,
            password = encryptPwd
        )
    }
}

suspend fun AuthService.sendSmsCaptcha(
    account: String,
    phoneNum: String
): ResponseModel<Any> {
    return RequestBodyBuilder.create {
        put("accountName", account)
        put("phoneNum", phoneNum)
    }.let {
        authInternalService.sendSmsCaptcha(it)
    }
}

suspend fun AuthService.infoApprove(
    account: String,
    phoneNum: String,
    captcha: String
): ResponseModel<Any> {
    return RequestBodyBuilder.create {
        put("accountName", account)
        put("phoneNum", phoneNum)
        put("captcha", captcha)
    }.let {
        authInternalService.authenticateModifyPwdSubmit(it)
    }
}