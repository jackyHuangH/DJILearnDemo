package com.zenchn.djilearndemo.base

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.lifecycle.ViewModel
import com.jacky.support.base.IActivity
import com.jacky.support.setOnAntiShakeClickListener
import com.jacky.support.utils.PreferenceUtil
import java.lang.reflect.ParameterizedType

/**
 * @author:Hzj
 * @date  :2018/10/30/030
 * desc  ：
 * record：
 */
interface IView : IActivity {
    fun onApiFailure(msg: String)

    fun onApiGrantRefuse()
}

interface IVMView<VM : ViewModel> {
    val mViewModel: VM

    val startObserve: (VM.() -> Unit)
}

//反射获取ViewModel实例
@Suppress("UNCHECKED_CAST")
fun <T : ViewModel> IVMView<T>.provideViewModelClass(): Class<T>? {
    return (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as? Class<T>
}

//---------------扩展函数------------------

//根据ID获取view
fun <V : View> IActivity.getView(@IdRes viewId: Int): V = findViewWithId<V>(viewId)

//获取textiew，editText的文字
fun IActivity.getTextString(@IdRes viewId: Int): String =
    getView<TextView>(viewId).text?.toString() ?: ""

//避免直接引用相同名称id造成错乱难以排查
fun <V : View> IActivity.viewExt(@IdRes viewId: Int, extra: V.() -> Unit) {
    val view = getView<V>(viewId)
    view.run(extra)
}

//获取editText 内容变化后的内容
fun IActivity.getEditChangeText(@IdRes viewId: Int, afterTextChange: (String) -> Unit) {
    val view: EditText = getView<EditText>(viewId)
    view.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            afterTextChange.invoke(s.toString())
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

    })
}

//对View的子view操作
fun <V : View> View.childViewExt(
    @IdRes childId: Int,
    extra: V.() -> Unit
) = findViewById<V>(childId)?.run {
    extra.invoke(this)
}

//设置是否可见
fun IActivity.viewVisibleExt(@IdRes viewId: Int, visible: Boolean) {
    viewExt<View>(viewId) {
        visibility = if (visible) View.VISIBLE else View.GONE
    }
}

//设置是否可见占位
fun IActivity.viewInvisibleExt(@IdRes viewId: Int, visible: Boolean) {
    viewExt<View>(viewId) {
        visibility = if (visible) View.VISIBLE else View.INVISIBLE
    }
}

//设置是否可用
fun IActivity.viewEnabledExt(@IdRes viewId: Int, enabled: Boolean) {
    viewExt<View>(viewId) {
        isEnabled=enabled
    }
}

//点击事件封装
fun IActivity.viewClickListener(@IdRes viewId: Int, click: (View) -> Unit) {
    viewExt<View>(viewId) {
        setOnAntiShakeClickListener { click.invoke(this) }
    }
}

//-------------------扩展属性-----------------
//用户登录id
//var IActivity.id by PreferenceUtil(PreferenceUtil.KEY_ID, 0)

