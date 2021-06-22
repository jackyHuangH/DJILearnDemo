package com.zenchn.api

import android.app.Application
import android.content.Context
import android.text.TextUtils
import android.util.Log
import androidx.startup.Initializer
import com.hjq.toast.ToastUtils
import com.zenchn.api.frame.*
import com.zenchn.common.SupportConfig
import com.zenchn.common.ext.checkNotNull
import com.zenchn.common.ext.isTrue
import com.zenchn.common.utils.LoggerKit
import com.zenchn.common.utils.getMetaData
import com.zenchn.common.utils.isNetworkAvailable
import okhttp3.logging.HttpLoggingInterceptor
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit


class ApiInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        context.let {
            ApiManager.init(it)
            //初始化工具类
            DeviceUtils.init(it)
        }
        Log.d("Init", "ApiInitializer-create")
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

object ApiManager {

    internal lateinit var contextRef: WeakReference<Context>

    private var reporter: IApiExceptionReporter? = null

    private var retrofitProvider: (() -> Retrofit)? = null

    private val retrofit: Retrofit by lazy {
        retrofitProvider?.invoke() ?: createDefaultRetrofit(contextRef.get().checkNotNull { })
    }

    internal fun init(@NotNull context: Context) {
        this.contextRef = WeakReference(context.applicationContext)
    }

    /**
     * 设置访问主机地址
     *
     * @param reporter
     */
    fun config(@Nullable reporter: IApiExceptionReporter? = null) {
        this.reporter = reporter
    }

    /**
     * 设置访问主机地址
     *
     * @param baseUrl
     */
    fun config(@NotNull baseUrl: String, @Nullable reporter: IApiExceptionReporter? = null) {
        val context = contextRef.get().checkNotNull { }
        this.retrofitProvider = { createDefaultRetrofit(context, baseUrl) }
        this.reporter = reporter
    }

    /**
     * 设置访问主机地址
     *
     * @param retrofit
     * @param reporter
     */
    fun config(@NotNull retrofit: Retrofit, @Nullable reporter: IApiExceptionReporter? = null) {
        this.retrofitProvider = { retrofit }
        this.reporter = reporter
    }

    /**
     * 释放资源
     */
    fun release() {
        this.contextRef.clear()
        this.reporter = null
    }

    internal fun checkNetworkAvailable() = contextRef.get().isNetworkAvailable().isTrue(true)


    /*
    * 判断设备 是否使用代理上网
    * */
    fun isWifiProxy(): Boolean {
        val proxyAddress: String? = System.getProperty("http.proxyHost")
        val proxyPort: Int? = System.getProperty("http.proxyPort")?.toIntOrNull()
        return !TextUtils.isEmpty(proxyAddress) && proxyPort != null
    }


    internal fun getExceptionReporter(): IApiExceptionReporter? = reporter

    internal fun <T : Any> create(@NotNull clazz: Class<T>): T {
        return retrofit.create(clazz).checkNotNull {
            "u must provider common_footer_loading really api host url"
        }
    }

    private fun createDefaultRetrofit(@NotNull context: Context): Retrofit {
        val baseUrl = getUrlFromMetaData(context)
        return createDefaultRetrofit(context, baseUrl = baseUrl)
    }

    private fun getUrlFromMetaData(context: Context): String {
        return context.run {
            "${getMetaData(ApiConfig.META_DATA_BASE_HOST_KEY)}${
                getMetaData(
                    ApiConfig.META_DATA_REST_API_PREFIX_KEY
                )
            }"
        }
    }

    private fun createDefaultRetrofit(
        @NotNull context: Context,
        @Nullable baseUrl: String?
    ): Retrofit {
        return if (baseUrl.isURL()) {
            newRetrofit(
                baseUrl = baseUrl.checkNotNull { },
                clientOptions = {

                    connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                    readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.SECONDS)
                    writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)

                    //添加cookies
//                        cookieJar(PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(context)))
                    retryOnConnectionFailure(true)//错误重连

                    // 开发模式记录整个body，否则只记录基本信息如返回200，http协议版本等
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                            else HttpLoggingInterceptor.Level.NONE
                        }
                    )
                    addNetworkInterceptor(NetworkCheckInterceptor())//增加网络状态拦截器
                    addNetworkInterceptor(RequestHeaderInterceptor())//增加请求头拦截器
                },
                retrofitOptions = {
                    addConverterFactory(ScalarsConverterFactory.create())
                    addConverterFactory(GsonConverterFactory.create())
                }
            )
        } else {
            throw ApiException("Retrofit create Error : u must provider a really api host url .")
        }
    }
}



