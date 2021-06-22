@file:Suppress("DEPRECATION", "unused")

package com.zenchn.widget

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewStub
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle

interface ILayout {

    var instanceState: Bundle?

    fun hostLifecycle(): Lifecycle? = when (this) {
        is ComponentActivity -> lifecycle
        is Fragment -> lifecycle
        else -> null
    }

    fun <T : View> findViewWithId(@IdRes id: Int): T?

    fun hostActivity(): Activity? = when (this) {
        is Activity -> this
        is Fragment -> activity
        is android.app.Fragment -> activity
        else -> throw IllegalStateException("无效的布局容器，既不是Activity也不是Fragment")
    }

    fun hostRootView(): View = when (this) {
        is Activity -> window.decorView
        is Fragment -> view
        is android.app.Fragment -> view
        else -> throw IllegalStateException("无效的布局容器，既不是Activity也不是Fragment")
    } ?: throw IllegalStateException("容器未载入或者已经被移除")

}

abstract class AbsViewHolder<T : View> {

    var view: T? = null

    @IdRes
    abstract fun getViewId(): Int
}


abstract class LifecycleViewHolder<T : View> : AbsViewHolder<T>(), DefaultLifecycleObserver

abstract class LifecycleViewHolder1<T : View> : AbsViewHolder<T>() {

    protected lateinit var lifecycleObserver: androidx.lifecycle.LifecycleObserver

}

abstract class LifecycleViewHolder2<T : View, O> : AbsViewHolder<T>(), LifecycleObserver<O>

interface LifecycleObserver<O> {
    fun onCreate(owner: O) {}
    fun onStart(owner: O) {}
    fun onResume(owner: O) {}
    fun onPause(owner: O) {}
    fun onStop(owner: O) {}
    fun onDestroy(owner: O) {}
}

//获取textiew，editText的文字
fun ILayout.getTextString(@IdRes viewId: Int): String =
    findViewWithId<TextView>(viewId)?.text?.toString() ?: ""

fun <T : View> ILayout.viewExt(
    @IdRes viewId: Int,
    extra: T.() -> Unit
) = findViewWithId<T>(viewId)?.run {
    extra.invoke(this)
}

fun <T : View, D> ILayout.viewValueExt(
    @IdRes viewId: Int,
    extra: T.() -> D
) = findViewWithId<T>(viewId)?.run {
    extra.invoke(this)
}

fun ILayout.viewStubExt(
    @IdRes viewId: Int,
    predicate: () -> Boolean,
    extra: ILayout.() -> Unit
) = findViewWithId<ViewStub>(viewId)?.run {
    if (predicate.invoke()) {
        inflateSafely()
        extra.invoke(this@viewStubExt)
    }
}

fun ILayout.viewStubExt(
    @IdRes viewId: Int,
    visible: Boolean,
    extra: (View.() -> Unit)? = null
) {
    findViewWithId<View>(viewId)?.run {
        if (visible) {
            if (this is ViewStub) {
                inflateSafely()?.apply {
                    id = viewId
                    visibility = View.VISIBLE
                    extra?.invoke(this)
                }
            } else {
                visibility = View.VISIBLE
                extra?.invoke(this)
            }
        } else {
            visibility = View.GONE
            extra?.invoke(this)
        }
    }
}

inline fun ILayout.viewClickListenerExt(
    @IdRes viewId: Int,
    crossinline interceptor: ((View) -> Boolean) = { true },
    crossinline listener: (View) -> Unit
) = findViewWithId<View>(viewId)?.setOnAntiShakeClickListener(
    interceptor = interceptor,
    listener = listener
)

fun <V1 : View, V2 : View> ILayout.viewLinkExt(
    @IdRes viewId1: Int,
    @IdRes viewId2: Int,
    watch: (V1, V2) -> Unit
) = findViewWithId<V1>(viewId1)?.let { v1 ->
    findViewWithId<V2>(viewId2)?.let { v2 ->
        watch.invoke(v1, v2)
    }
}

fun <V1 : View, V2 : View, V3 : View> ILayout.viewLinkExt(
    @IdRes viewId1: Int,
    @IdRes viewId2: Int,
    @IdRes viewId3: Int,
    watch: (V1, V2, V3) -> Unit
) = findViewWithId<V1>(viewId1)?.let { v1 ->
    findViewWithId<V2>(viewId2)?.let { v2 ->
        findViewWithId<V3>(viewId3)?.let { v3 ->
            watch.invoke(v1, v2, v3)
        }
    }
}

fun ILayout.viewVisibleExt(@IdRes viewId: Int, visible: Boolean) {
    findViewWithId<View>(viewId)?.visibility = (if (visible) View.VISIBLE else View.GONE)
}

fun ILayout.viewInVisibleExt(@IdRes viewId: Int, visible: Boolean) {
    findViewWithId<View>(viewId)?.visibility = (if (visible) View.VISIBLE else View.INVISIBLE)
}

//设置是否可用
fun ILayout.viewEnabledExt(@IdRes viewId: Int, enabled: Boolean) {
    viewExt<View>(viewId) {
        isEnabled=enabled
    }
}