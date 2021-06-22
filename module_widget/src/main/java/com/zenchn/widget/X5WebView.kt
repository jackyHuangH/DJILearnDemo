package com.zenchn.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.tencent.smtt.sdk.CookieSyncManager
import com.tencent.smtt.sdk.WebSettings
import com.tencent.smtt.sdk.WebViewClient
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

interface IWebView : ILayout {

    val webViewHolder: ViewHolder

    abstract class ViewHolder : LifecycleViewHolder<X5WebView>() {

        override fun onDestroy(owner: LifecycleOwner) {
            //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
            view?.destroy()
        }

    }
}

fun IWebView.initWebView(@Nullable block: (X5WebView.() -> Unit)? = null) = with(webViewHolder) {
    hostLifecycle()?.addObserver(this)
    view = findViewWithId<X5WebView>(getViewId())?.apply {
        overScrollMode = View.OVER_SCROLL_ALWAYS
        if (block != null) block()
    }
}

fun IWebView.updateWebView(@NotNull block: X5WebView.() -> Unit) = with(webViewHolder) {
    findViewWithId<X5WebView>(getViewId())?.apply {
        block()
    }
}


interface WebViewJavaScriptFunction {

    fun onJsFunctionCalled(tag: String)

}

class X5WebView : com.tencent.smtt.sdk.WebView {

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
            context,
            attrs,
            defStyleAttr,
            false
    )

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, b: Boolean) :
            this(context, attrs, defStyleAttr, null, b)

    constructor(
            context: Context,
            attrs: AttributeSet?,
            defStyleAttr: Int,
            var4: Map<String, Any>?,
            var5: Boolean
    ) : super(context, attrs, defStyleAttr, var4, var5) {
        this.webViewClient = object : WebViewClient() {

            /**
             * 防止加载网页时调起系统浏览器
             */
            override fun shouldOverrideUrlLoading(
                    view: com.tencent.smtt.sdk.WebView,
                    url: String
            ): Boolean {
                view.loadUrl(url)
                return true
            }
        }
        initWebViewSettings()
    }

    @Suppress("DEPRECATION")
    @SuppressLint("SetJavaScriptEnabled")
    protected fun initWebViewSettings() {
        settings.apply {
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            allowFileAccess = true
            layoutAlgorithm = WebSettings.LayoutAlgorithm.NARROW_COLUMNS
            setSupportZoom(true)
            builtInZoomControls = true
            useWideViewPort = true
            setSupportMultipleWindows(true)
            // setLoadWithOverviewMode(true);
            setAppCacheEnabled(true)
            // setDatabaseEnabled(true);
            domStorageEnabled = true
            setGeolocationEnabled(true)
            setAppCacheMaxSize(java.lang.Long.MAX_VALUE)
            // setPageCacheCapacity(IX5WebSettings.DEFAULT_CACHE_CAPACITY);
            pluginState = WebSettings.PluginState.ON_DEMAND
            // webSetting.setRenderPriority(WebSettings.RenderPriority.HIGH);
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
        CookieSyncManager.createInstance(context).sync()
    }
}