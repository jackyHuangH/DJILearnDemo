@file:Suppress("DEPRECATION", "UNCHECKED_CAST")

package com.zenchn.djilearndemo.base

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
import com.zenchn.common.frame.IPermission
import com.zenchn.widget.ILayout
import com.zenchn.widget.IUiDelegate
import java.lang.reflect.ParameterizedType


interface IRouter {

    fun getRouterContext(): Context? = when (this) {
        is Activity -> this
        is Fragment -> activity
        is android.app.Fragment -> activity
        else -> null
    }
}

interface IView : IUiDelegate, IRouter, IPermission, ILayout {

    @LayoutRes
    fun getLayoutId(): Int

    fun initWidget()

}

fun IView.getSupportFragmentManager() = when (this) {
    is FragmentActivity -> supportFragmentManager
    is Fragment -> fragmentManager ?: (hostActivity() as? FragmentActivity)?.supportFragmentManager
    else -> null
}

interface IActivity {

    fun initStatusBar()

    fun getContext(): Context

}

interface IFragment {

    var hostActivity: IContract.Activity?

    fun onNewIntent(intent: Intent?)

}

interface IVMView<VM : ViewModel> {

    val viewModel: VM

    val onViewModelStartup: (VM.() -> Unit)
}

interface IContract {

    interface Activity : IView, IActivity

    interface Fragment : IView, IFragment

    interface VMActivity<VM : ViewModel> : Activity, IVMView<VM>

    interface VMFragment<VM : ViewModel> : Fragment, IVMView<VM>

}

inline fun <reified VM : ViewModel> IContract.VMFragment<*>.getActivityViewModel(): VM? {
    return hostActivity()?.let { activity ->
        when (activity) {
            is IContract.VMActivity<*> -> viewModel as? VM
            is FragmentActivity -> ViewModelProviders.of(activity).get()
            else -> null
        }
    }
}

@Suppress("UNCHECKED_CAST")
fun <T : ViewModel> IVMView<T>.getViewModelClass(): Class<T>? {
    return (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as? Class<T>
}




