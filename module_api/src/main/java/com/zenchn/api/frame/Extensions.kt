package com.zenchn.api.frame

import com.zenchn.api.ApiConfig
import com.zenchn.common.ext.isNotNullAndNotEmpty
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jetbrains.annotations.NotNull
import retrofit2.Retrofit
import java.util.regex.Pattern

internal fun String?.isURL(): Boolean {
    return this?.takeIf { it.isNotNullAndNotEmpty() }?.let { url ->
        Pattern.matches(ApiConfig.URL_REGEX, url)
    } ?: false
}

fun newOkHttpClient(
        @NotNull clientOptions: OkHttpClient.Builder.() -> OkHttpClient.Builder
): OkHttpClient {
    return OkHttpClient.Builder().apply { clientOptions(this) }.build()
}

fun newRetrofit(
        @NotNull baseUrl: String,
        @NotNull clientOptions: OkHttpClient.Builder.() -> OkHttpClient.Builder,
        @NotNull retrofitOptions: Retrofit.Builder.() -> Retrofit.Builder
): Retrofit {
    val okHttpClient = newOkHttpClient(clientOptions)
    return Retrofit.Builder().baseUrl(baseUrl).client(okHttpClient).apply { retrofitOptions(this) }.build()
}

fun newRetrofit(
        @NotNull baseUrl: String,
        @NotNull retrofitOptions: Retrofit.Builder.() -> Retrofit.Builder
): Retrofit {
    return Retrofit.Builder().baseUrl(baseUrl).apply { retrofitOptions(this) }.build()
}

fun Request.options(
        @NotNull requestOptions: Request.Builder.() -> Request.Builder
): Request {
    return this.newBuilder().run { requestOptions(this) }.build()
}

fun Interceptor.Chain.proceed(
        @NotNull requestOptions: Request.Builder.() -> Request.Builder
): Response {
    val request = request().options(requestOptions)
    return proceed(request)
}









