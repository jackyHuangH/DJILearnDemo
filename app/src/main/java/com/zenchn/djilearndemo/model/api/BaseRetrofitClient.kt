package com.zenchn.djilearndemo.model.api

import com.zenchn.djilearndemo.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * @author:Hzj
 * @date  :2019/7/2/002
 * desc  ：retrofit+okhttp3 基类
 * record：
 */
abstract class BaseRetrofitClient {
    companion object {
        //请求超时
        private const val TIME_OUT = 5
    }

    private val okHttpClient: OkHttpClient
        get() {
            val builder = OkHttpClient.Builder()
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.BASIC
                }
            }
            builder.addInterceptor(loggingInterceptor)
                .connectTimeout(TIME_OUT.toLong(), TimeUnit.SECONDS)
            handleBuilder(builder)

            return builder.build()
        }

    protected abstract fun handleBuilder(builder: OkHttpClient.Builder)

    fun <C> getService(serviceClass: Class<C>, baseUrl: String): C {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(serviceClass)
    }
}

//默认RetrofitClient
object DefaultRetrofitClient : BaseRetrofitClient() {
    //提供默认service
    val mService by lazy { getService(ApiService::class.java, ApiService.BASE_URL) }

    override fun handleBuilder(builder: OkHttpClient.Builder) {
        //do nothing
    }
}