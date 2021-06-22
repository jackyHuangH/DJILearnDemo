package com.zenchn.widget

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.WindowManager
import android.widget.ProgressBar
import androidx.annotation.LayoutRes
import androidx.annotation.StyleRes
import androidx.lifecycle.LifecycleOwner
import com.github.ybq.android.spinkit.style.Circle

/**
 * 作   者：wangr on 2019/11/12 11:07
 * 描   述：
 * 修订记录：
 */
open class CustomDialog(
    context: Context,
    @LayoutRes layoutResId: Int,
    @StyleRes themeResId: Int = R.style.DialogStyle,
) : Dialog(context, themeResId) {

    open val attributes: WindowManager.LayoutParams.() -> Unit = {
        gravity = Gravity.CENTER
        width = WindowManager.LayoutParams.MATCH_PARENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // 设置Dialog参数
        window?.let {
            it.attributes = (it.attributes ?: WindowManager.LayoutParams()).apply(attributes)
        }
    }

    init {
        if (context is LifecycleOwner) {
            lifecycleOwner(context)
        }
        // 加载布局
        apply {
            setContentView(layoutResId)
            setCancelable(true)
            setCanceledOnTouchOutside(false)
        }
    }

    private fun lifecycleOwner(owner: LifecycleOwner? = null) {
        owner?.lifecycle?.let {
            val observer = UiDelegateLifecycleObserver {
                if (this.isShowing) this.dismiss()
            }
            it.addObserver(observer)
        }
    }
}

internal class LoadingDialog(
    context: Context,
    cancelable: Boolean = false,
    canceledOnTouchOutside: Boolean = false
) : Dialog(context, R.style.DialogStyle_Loading) {

    init {
        // 加载布局
        setContentView(R.layout.include_common_loading_dialog)
        findViewById<ProgressBar>(R.id.progressBar).apply {
            this.indeterminateDrawable = Circle()
        }
        setCancelable(cancelable)
        setCanceledOnTouchOutside(canceledOnTouchOutside)
        // 设置Dialog参数
        window?.let {
            it.attributes?.let { params ->
                params.gravity = Gravity.CENTER
                window?.attributes = params
            }
        }
    }

}