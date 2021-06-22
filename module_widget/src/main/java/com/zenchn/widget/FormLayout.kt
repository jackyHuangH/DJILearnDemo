package com.zenchn.widget

import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.zenchn.common.ext.safelyRun
import org.jetbrains.annotations.Nullable

/**
 * @author:Hzj
 * @date  :2020/9/27
 * desc  ：表单 数据处理封装
 * record：
 */
interface IFormLayout : ILayout

interface IFormLayoutHelper<L, V> {
    val setValue: (View, V?) -> Unit
    val setLabel: (View, L?) -> Unit
    val getValue: (View) -> V?
}

abstract class AbsFormLayoutHelper<V> : IFormLayoutHelper<V, V> {
    /**
     * label和value一致
     */
    val setLabelAndValue: (View, V?) -> Unit = { view, v ->
        setLabel(view, v)
        setValue(view, v)
    }
}

@Suppress("UNCHECKED_CAST")
class TextFormLayoutHelper<V>(@IdRes resId: Int) : IFormLayoutHelper<CharSequence, V> {
    override val setValue: (View, V?) -> Unit = { view, t ->
        view.findViewById<TextView>(resId)?.tag = t
    }

    override val setLabel: (View, CharSequence?) -> Unit = { view, l ->
        view.findViewById<TextView>(resId).text = l
    }
    override val getValue: (View) -> V? = { view ->
        view.findViewById<TextView>(resId)?.tag as? V
    }
}

//输入表单
fun IFormLayout.initFormLayout(
    @IdRes inputLayoutId: Int,
    @StringRes itemTitleResId: Int,
    withMark: Boolean = false,
    withNext: Boolean = false,
    enabled: Boolean = true,
    editEnable: Boolean = true,
    marquee: Boolean = true,
    extra: (View.() -> Unit)? = null,
    helper: IFormLayoutHelper<CharSequence, Any> = TextFormLayoutHelper<Any>(R.id.v_form_content)
) {
    viewExt<View>(inputLayoutId, extra = {
        childViewExt<TextView>(R.id.tv_form_title, extra = {
            setTextExt(itemTitleResId)
            isSelected = marquee
        })
        childViewStubExt(R.id.vs_form_mark, visible = withMark)
        childViewStubExt(R.id.vs_form_next, visible = withNext)
        childViewExt<TextView>(R.id.v_form_content, extra = {
            if (this is EditText) {
                isEnabled = editEnable
                addTextWatcher { charSequence: CharSequence?, _: Int, _: Int, _: Int ->
                    helper.setValue(this, charSequence.toString())
                }
            }
        })

        isEnabled = enabled
        setBackgroundColor(ContextCompat.getColor(context, if (enabled) R.color.white else R.color.color_DFDFDF))
        setTag(inputLayoutId, helper)
        extra?.invoke(this)
    })
}


//选择表单
fun <L, V> IFormLayout.initFormLayout(
    @IdRes inputLayoutId: Int,
    @StringRes itemTitleResId: Int,
    withMark: Boolean = false,
    withNext: Boolean = false,
    enabled: Boolean = true,
    marquee: Boolean = true,
    helper: IFormLayoutHelper<L, V>,
    extra: (View.() -> Unit)? = null,
    @Nullable nextAction: ((String, View, IFormLayoutHelper<L, V>) -> Unit)? = null
) {
    viewExt<View>(inputLayoutId, extra = {
        childViewExt<TextView>(R.id.tv_form_title, extra = {
            setTextExt(itemTitleResId)
            isSelected = marquee
        })
        childViewStubExt(R.id.vs_form_mark, visible = withMark)
        childViewStubExt(R.id.vs_form_next, visible = withNext)

        isEnabled = enabled
        setBackgroundColor(ContextCompat.getColor(context, if (enabled) R.color.white else R.color.color_DFDFDF))
        setOnAntiShakeClickListener {
            val content = findViewById<TextView>(R.id.v_form_content)?.text.toString()
            nextAction?.invoke(content, this, helper)
        }
        setTag(inputLayoutId, helper)
        extra?.invoke(this)
    })
}

@Suppress("UNCHECKED_CAST")
fun <V> IFormLayout.getFormLayoutValue(
    @IdRes inputLayoutId: Int
): V? = safelyRun {
    findViewWithId<View>(inputLayoutId)?.run {
        (getTag(inputLayoutId) as? IFormLayoutHelper<*, *>)?.getValue?.invoke(this) as? V
    }
}

@Suppress("UNCHECKED_CAST")
fun <L> IFormLayout.setFormLayoutLabel(
    @IdRes inputLayoutId: Int,
    label: L?,
    contentViewExt: ((TextView) -> Unit)? = null
) = safelyRun {
    findViewWithId<View>(inputLayoutId)?.let { view ->
        (view.getTag(inputLayoutId) as? IFormLayoutHelper<L, *>)?.setLabel?.invoke(view, label)
        view.findViewById<TextView>(R.id.v_form_content)?.apply {
            contentViewExt?.invoke(this)
        }
    }
}


@Suppress("UNCHECKED_CAST")
fun <V> IFormLayout.setFormLayoutValue(
    @IdRes inputLayoutId: Int,
    value: V?
) = safelyRun {
    findViewWithId<View>(inputLayoutId)?.let { view ->
        (view.getTag(inputLayoutId) as? IFormLayoutHelper<*, V>)?.setValue?.invoke(view, value)
    }
}


@Suppress("UNCHECKED_CAST")
inline fun <L, V> IFormLayout.setFormLayoutLabelAndValue(
    @IdRes inputLayoutId: Int,
    label: L?,
    crossinline labelToValue: (L?) -> V? = { l -> l as? V }
) = safelyRun {
    findViewWithId<View>(inputLayoutId)?.let { view ->
        (view.getTag(inputLayoutId) as? IFormLayoutHelper<L, V>)?.run {
            labelToValue.invoke(label)?.let { value ->
                setValue(view, value)
            }
            setLabel.invoke(view, label)
        }
    }
}