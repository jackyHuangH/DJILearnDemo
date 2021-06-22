package com.zenchn.api

import com.zenchn.api.entity.ResponseModel
import com.zenchn.api.entity.TokenEntity
import com.zenchn.api.frame.ERROR_RESPONSE
import com.zenchn.api.frame.OK_RESPONSE
import com.zenchn.api.frame.errorResponse
import com.zenchn.api.frame.isApiSuccess
import com.zenchn.api.service.authService
import com.zenchn.api.service.login
import com.zenchn.common.ext.checkNotNull
import com.zenchn.common.ext.encryptPwd
import com.zenchn.common.ext.isNotNullAndNotEmpty
import com.zenchn.common.ext.safelyRun
import com.zenchn.common.utils.PreferenceUtil
import org.jetbrains.annotations.NotNull

object OAuthController {

    //历史登录信息
    private var historyLoginNameConf by PreferenceUtil(PreferenceUtil.USER_NAME, "")
    private var historyLoginPwdConf by PreferenceUtil(PreferenceUtil.USER_PWD, "")
    private var rememberPassword by PreferenceUtil(PreferenceUtil.REMEMBER_PASSWORD, false)
    private var autoLogin by PreferenceUtil(PreferenceUtil.AUTO_LOGIN, false)
    private var accessTokenConf by PreferenceUtil(PreferenceUtil.AUTH_TOKEN, "")

    private val LOCK by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { Any() }

    private var tokenEntity: TokenEntity? = null

    internal fun getAccessToken(): String {
        return tokenEntity?.accessToken ?: ""
    }

    suspend fun autoLogin(): ResponseModel<Any> {
        return if (historyLoginNameConf.isNotNullAndNotEmpty() && historyLoginPwdConf.isNotNullAndNotEmpty()) {
            login(historyLoginNameConf, historyLoginPwdConf)
        } else {
            ERROR_RESPONSE
        }
    }

    /**
     * 登录
     */
    suspend fun login(
        @NotNull loginName: String,
        @NotNull password: String
    ): ResponseModel<Any> {
        return try {
            authService.login(
                loginName,
                password.encryptPwd().checkNotNull { "Error , pwd is empty!" }
            ).apply {
                if (isApiSuccess()) {
                    synchronized(LOCK) {
                        //默认记住用户名
                        historyLoginNameConf = loginName
                        //自动登录或者记住密码就保存密码
                        if (autoLogin || rememberPassword) {
                            historyLoginPwdConf = password
                        }
                        accessTokenConf = data?.accessToken.orEmpty()
                        this@OAuthController.tokenEntity = data
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            e.errorResponse()
        }
    }

    suspend fun logout(): ResponseModel<Any> {
        return try {
            authService.logout().apply {
                release()
                OK_RESPONSE
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ERROR_RESPONSE
        } finally {
            if (!autoLogin && !rememberPassword) {
                historyLoginPwdConf = ""
            }
        }
    }

    //清除用户登录状态
    private fun release() {
        safelyRun {
            this@OAuthController.tokenEntity = null
        }
    }
}