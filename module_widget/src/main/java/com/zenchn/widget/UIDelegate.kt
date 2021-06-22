package com.zenchn.widget

import android.app.Dialog
import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.hjq.toast.ToastUtils
import com.zenchn.common.utils.ResolveUtils
import org.jetbrains.annotations.NotNull
import java.lang.ref.WeakReference

/**
 * 作   者：wangr on 2019/11/12 10:58
 * 描   述：
 * 修订记录：
 */


interface IUiDelegate {

    fun showMessage(@NotNull msg: String? = null,@StringRes msgResId: Int? = null)

    fun showProgress(cancelable: Boolean = false)

    fun hideProgress()

    fun detachedFromView() {}

}

class DefaultUiDelegate(context: Context) : IUiDelegate {

    private val contextRef: WeakReference<Context> = WeakReference(context)
    private val loadingDialog: Dialog = LoadingDialog(context)

    init {
        if (context is LifecycleOwner) {
            lifecycleOwner(context)
        }
    }

    override fun showProgress(cancelable: Boolean) = with(loadingDialog) {
        setCancelable(cancelable)
        setCanceledOnTouchOutside(cancelable)
        show()
    }

    override fun hideProgress() = loadingDialog.dismiss()

    override fun showMessage(msg: String?, msgResId: Int?) {
        contextRef.get()?.apply {
            if (msgResId != null) {
                ResolveUtils.resolveString(this, msgResId)
            } else {
                msg
            }?.let { msg ->
                ToastUtils.show(msg.toString())
            }
        }
    }

    override fun detachedFromView() {
        if (loadingDialog.isShowing) loadingDialog.dismiss()
        contextRef.clear()
    }
}

internal class UiDelegateLifecycleObserver(private val destroy: () -> Unit) :
    DefaultLifecycleObserver {
    override fun onDestroy(owner: LifecycleOwner) {
        destroy()
    }
}

fun IUiDelegate.lifecycleOwner(owner: LifecycleOwner? = null) {
    owner?.lifecycle?.let {
        val observer = UiDelegateLifecycleObserver(::detachedFromView)
        it.addObserver(observer)
    }
}