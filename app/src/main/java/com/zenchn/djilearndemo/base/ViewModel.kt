package com.zenchn.djilearndemo.base

import android.app.Activity
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.provider.Settings
import androidx.annotation.CallSuper
import androidx.annotation.IntRange
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.zenchn.api.entity.ResponseModel
import com.zenchn.api.frame.dispatch
import com.zenchn.api.frame.isApiSuccess
import com.zenchn.api.frame.observe
import com.zenchn.common.ext.checkNotNull
import com.zenchn.common.ext.coroutineTryCatch
import com.zenchn.common.ext.safelyRun
import com.zenchn.common.utils.CodecUtils
import com.zenchn.djilearndemo.R
import com.zenchn.djilearndemo.app.GlobalLifecycleObserver
import com.zenchn.djilearndemo.app.MyApplication
import kotlinx.coroutines.*
import java.math.BigInteger

/**
 * 作   者：hzj on 2019/11/12 15:49
 * 描   述：用作修饰会被暂停的函数，被标记为 suspend 的函数只能运行在协程或者其他 suspend 函数当中。
 * 修订记录：
 */

abstract class BaseViewModel(application: Application) : AndroidViewModel(application),
    LifecycleObserver {

    //消息通道
    val msgChannel: MutableLiveData<String> by lazy { MutableLiveData<String>() }

    //加载框通道
    val loadingChannel: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }

    //刷新通道
    val refreshStatusChannel: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }

    protected lateinit var lifecycle: Lifecycle

    /**
     * 建议用protected修饰，保证只在子类间传递
     */
    @Suppress("UNCHECKED_CAST")
    @CallSuper
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    open fun onCreate(owner: LifecycleOwner) {
        //初始化ViewModel绑定
        lifecycle = owner.lifecycle
        //初始化ViewModel绑定
        (owner as? IVMView<ViewModel>)?.onViewModelStartup?.invoke(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    open fun onResume(owner: LifecycleOwner) {

    }

    @CallSuper
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    open fun onDestroy(owner: LifecycleOwner) {

    }

    @CallSuper
    open fun onNewIntent(intent: Intent?) {

    }

    fun launchOnUI(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch {
            block()
        }
    }

}

fun <T> BaseViewModel.httpRequest(
    showLoading: Boolean = false,
    showError: Boolean = true,
    request: suspend CoroutineScope.() -> ResponseModel<T>,
    onNext: (Boolean, T?, String?) -> Unit,
    onSubscribe: (() -> Unit)? = null,
    onError: (Throwable) -> Unit = { e ->
        e.dispatch(
            msgResult = { errorMessage ->
                // 提取出的错误信息
                onNext.invoke(false, null, errorMessage)
                if (showError) {
                    msgChannel.value = errorMessage
                }
            },
            refused = {
                //授权失败，返回登录页
                GlobalLifecycleObserver.apiRefusedToLogin()
            }
        )
    },
    onComplete: (() -> Unit)? = null
) {
    launchOnUI {
        coroutineTryCatch(
            tryBlock = {
                onSubscribe?.invoke()
                return@coroutineTryCatch withContext(Dispatchers.IO) {
                    if (showLoading) loadingChannel.postValue(true)
                    request.invoke(this)
                }
            },
            catchBlock = { e -> onError.invoke(e) },
            finallyBlock = {
                if (showLoading) loadingChannel.value = false
                onComplete?.invoke()
            },
            handleCancellationExceptionManually = true
        )?.let {
            if (it.isApiSuccess()) {
                onNext.invoke(true, it.data, null)
            } else {
                onNext.invoke(false, null, it.message)
                if (showError) {
                    msgChannel.value = it.message
                }
            }
        }
    }
}

fun <T> BaseViewModel.httpRequest(
    showLoading: Boolean = false,
    showRefreshing: Boolean = false,
    showError: Boolean = true,
    request: suspend CoroutineScope.() -> ResponseModel<T>,
    callback: (Boolean, T?, String?) -> Unit
) {
    launchOnUI {
        coroutineTryCatch(
            tryBlock = withContext(Dispatchers.IO) {
                if (showLoading) loadingChannel.postValue(true)
                if (showRefreshing) refreshStatusChannel.postValue(true)
                request
            },
            catchBlock = { e ->
                e.dispatch(
                    msgResult = { errorMessage ->
                        // 提取出的错误信息
                        callback.invoke(false, null, errorMessage)
                        if (showError) msgChannel.value = errorMessage
                    },
                    refused = {
                        GlobalLifecycleObserver.apiRefusedToLogin()      //授权失败，返回登录页
                    }
                )
            },
            finallyBlock = {
                if (showLoading) loadingChannel.value = false
                if (showRefreshing) refreshStatusChannel.value = false
            },
            handleCancellationExceptionManually = true
        )?.observe { success, t, s: String? ->
            callback.invoke(success, t, s)
            if (!success && showError) msgChannel.value = s
        }
    }
}

suspend fun <T> BaseViewModel.httpRequestSync(
    silent: Boolean = false,
    request: CoroutineScope.() -> ResponseModel<T>
): ResponseModel<T>? {
    return coroutineTryCatch(
        tryBlock = { withContext(Dispatchers.IO) { request.invoke(this) } },
        catchBlock = { e ->
            e.dispatch(
                msgResult = { errorMessage -> if (!silent) msgChannel.postValue(errorMessage) },
                refused = { GlobalLifecycleObserver.apiRefusedToLogin() }
            )
        },
        handleCancellationExceptionManually = true
    )
}

suspend fun <T> BaseViewModel.httpRequestAsync(
    silent: Boolean = false,
    request: suspend CoroutineScope.() -> ResponseModel<T>
): Deferred<ResponseModel<T>>? {
    return coroutineTryCatch(
        tryBlock = { viewModelScope.async(Dispatchers.IO) { request.invoke(this) } },
        catchBlock = { e ->
            e.dispatch(
                msgResult = { errorMessage -> if (!silent) msgChannel.postValue(errorMessage) },
                refused = { GlobalLifecycleObserver.apiRefusedToLogin() }
            )
        },
        handleCancellationExceptionManually = true
    )
}

fun BaseViewModel.delayExecute(
    @IntRange(from = 0) timeMillis: Long,
    onPreExecute: (() -> Unit)? = null,
    onPostExecute: () -> Unit
) {
    viewModelScope.launch {
        onPreExecute?.invoke()
        withContext(Dispatchers.IO) {
            delay(timeMillis)
        }
        onPostExecute.invoke()
    }
}

suspend fun <T> launchIO(block: suspend CoroutineScope.() -> T): T {
    return withContext(Dispatchers.IO) {
        block.invoke(this)
    }
}

fun BaseViewModel.getString(@StringRes resId: Int): String {
    return getApplication<MyApplication>().getString(resId)
}

//--------------------------------NFC VM封装-------------------------------------------

abstract class BaseNFCViewModel(application: Application) : BaseViewModel(application) {

    private var nfcAdapter: NfcAdapter? = null

    private lateinit var pendingIntent: PendingIntent

    val nfcSupportObservable by lazy { MutableLiveData<Boolean>() }
    val nfcObservable by lazy { MutableLiveData<Tag>() }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        pendingIntent = when (owner) {
            is Activity -> PendingIntent.getActivity(owner, 0, Intent(owner, owner::class.java), 0)
            is Fragment -> owner.activity.checkNotNull { " Error , Fragment not attached to an activity ! ! !" }
                .let { activity ->
                    PendingIntent.getActivity(
                        activity,
                        0,
                        Intent(activity, activity::class.java),
                        0
                    )
                }
            else -> throw IllegalStateException(" Error , BaseNFCViewModel just support Activity or Fragment ! ! !")
        }
    }

    @CallSuper
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    open fun onStart(owner: LifecycleOwner) {
        nfcAdapter = NfcAdapter.getDefaultAdapter(getApplication())
    }

    @CallSuper
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    override fun onResume(owner: LifecycleOwner) {
        when {
            nfcAdapter == null -> {
                nfcSupportObservable.value = false
                msgChannel.postValue(getString(R.string.common_msg_request_enable_nfc))
            }

            // 支持NFC，但未开启，根据包名打开对应的设置界面
            nfcAdapter?.isEnabled != true -> {
                nfcSupportObservable.value = true
                msgChannel.postValue(getString(R.string.common_msg_request_enable_nfc))
                val intent = Intent(Settings.ACTION_NFC_SETTINGS)
                getApplication<MyApplication>().startActivity(intent)
            }

            //设置处理优于所有其他NFC的处理
            else -> {
                nfcSupportObservable.value = true
                nfcAdapter?.enableForegroundDispatch(owner.getActivity(), pendingIntent, null, null)
            }

        }
    }

    @CallSuper
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    open fun onPause(owner: LifecycleOwner) {
        //恢复默认状态
        nfcAdapter?.disableForegroundDispatch(owner.getActivity())
    }

    companion object {
        const val NFC_EXTRA_TAG = "android.nfc.extra.TAG"
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        safelyRun {
            intent?.getParcelableExtra<Tag>(NFC_EXTRA_TAG)
                ?.let { tag -> nfcObservable.postValue(tag) }
        }
    }

//    abstract fun onNfcReceived(tag: Tag)

    fun Tag.readCardId(radix: Int = 10): String {
        return CodecUtils.bytes2Hex(id).run { BigInteger(this, 16).toString(radix) }
    }

    private fun LifecycleOwner.getActivity(): Activity? = when (this) {
        is Activity -> this
        is Fragment -> activity
        else -> null
    }

}