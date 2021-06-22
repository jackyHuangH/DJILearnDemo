package com.zenchn.widget

import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.ViewStub
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import org.jetbrains.annotations.Nullable

interface ITitleBar : ILayout

data class TitleBarDelegate(val titleBar: ITitleBar)

/**
 * @param titleCenter 标题是否居中，false居左
 */
fun ITitleBar.initTitleBar(
    @StringRes titleResId: Int? = null,
    hasBottomLine: Boolean = true,
    withBack: Boolean = true,
    autoBack: Boolean = true,
    marque: Boolean = true,
    titleCenter: Boolean = true,//标题是否居中，false居左
    option: (TitleBarDelegate.() -> Unit)? = null
) {
    initTitleBar(hasBottomLine, withBack, autoBack) {
        title(resId = titleResId, marque = marque, titleIsCenter = titleCenter)
        option?.invoke(this)
    }
}

fun ITitleBar.initTitleBar(
    titleText: CharSequence,
    hasBottomLine: Boolean = true,
    withBack: Boolean = true,
    autoBack: Boolean = true,
    marque: Boolean = true,
    option: (TitleBarDelegate.() -> Unit)? = null
) {
    initTitleBar(hasBottomLine, withBack, autoBack) {
        title(text = titleText, marque = marque)
        option?.invoke(this)
    }
}

fun ITitleBar.initTitleBar(
    hasBottomLine: Boolean = true,
    withBack: Boolean = true,
    autoBack: Boolean = true,
    option: (TitleBarDelegate.() -> Unit)? = null
) {
    findViewWithId<View>(R.id.title_bar)?.apply {
        tag = TitleBarDelegate(this@initTitleBar).apply {
            bottomLine(hasBottomLine = hasBottomLine)
            back(hasBack = withBack, autoBack = autoBack)
            option?.invoke(this)
        }
    }
}


fun ITitleBar.updateTitleBar(
    delegate: TitleBarDelegate.() -> Unit
) {
    (findViewWithId<View>(R.id.title_bar)?.tag as? TitleBarDelegate)?.run {
        delegate(this)
    }
}

fun TitleBarDelegate.title(
    @IdRes id: Int = R.id.tv_title,
    @IdRes parentId: Int = R.id.vs_title,
    @StringRes resId: Int? = null,
    @Nullable text: CharSequence? = null,
    marque: Boolean = true,
    titleIsCenter: Boolean = true,
) {
    titleBar.findViewWithId<View>(R.id.title_bar)?.apply {
        findViewById<ViewStub>(parentId)?.run {
            if (isInflated()) {
                (this@apply.getTag(parentId) as? View)?.apply { visibility = View.VISIBLE }
            } else {
                inflateSafely()?.let { this@apply.setTag(parentId, it) }
            }
        }
        findViewById<TextView>(id)?.run {
            setTextExt(resId, text)
            //是否开启跑马灯
            isSelected = marque
            //设置标题是否居中
            gravity = if (titleIsCenter) Gravity.CENTER else Gravity.CENTER_VERTICAL
        }
    }
}

fun TitleBarDelegate.rightButton(
    @IdRes id: Int = R.id.tv_right_button,
    @IdRes parentId: Int = R.id.vs_right_button,
    @StringRes resId: Int? = null,
    @Nullable text: CharSequence? = null,
    @Nullable extra: (TextView.() -> Unit)? = null,
    @Nullable action: (View) -> Unit
) {
    titleBar.findViewWithId<View>(R.id.title_bar)?.apply {
        findViewById<ViewStub>(parentId)?.run {
            if (isInflated()) {
                (this@apply.getTag(parentId) as? View)?.apply { visibility = View.VISIBLE }
            } else {
                inflateSafely()?.let { this@apply.setTag(parentId, it) }
            }
        }
        findViewById<TextView>(id)?.apply {
            setTextExt(resId, text)
            setOnAntiShakeClickListener {
                action.invoke(it)
            }
            extra?.invoke(this)
        }
    }
}

//右侧图片按钮自定义
fun TitleBarDelegate.rightImageButton(
    @IdRes id: Int = R.id.tv_right_image_button,
    @IdRes parentId: Int = R.id.vs_right_image_button,
    @DrawableRes resId: Int? = null,
    @Nullable drawable: Drawable? = null,
    @Nullable extra: (View.() -> Unit)? = null,
    @Nullable action: (View) -> Unit
) {
    titleBar.findViewWithId<View>(R.id.title_bar)?.apply {
        findViewById<ViewStub>(parentId)?.run {
            if (isInflated()) {
                (this@apply.getTag(parentId) as? View)?.apply { visibility = View.VISIBLE }
            } else {
                inflateSafely()?.let { this@apply.setTag(parentId, it) }
            }
        }
        findViewById<ImageButton>(id)?.apply {
            if (resId != null) {
                setImageDrawable(ContextCompat.getDrawable(context, resId))
            } else {
                drawable?.let { setImageDrawable(it) }
            }
            setOnAntiShakeClickListener {
                action.invoke(it)
            }
        }
    }
}

fun TitleBarDelegate.bottomLine(
    @IdRes parentId: Int = R.id.vs_line,
    hasBottomLine: Boolean = true
) {
    with(titleBar) {
        if (hasBottomLine) {
            findViewWithId<ViewStub>(parentId)?.run {
                if (isInflated()) {
                    (this@with.findViewWithId<View>(R.id.title_bar)
                        ?.getTag(parentId) as? View)?.visibility =
                        View.VISIBLE
                } else {
                    val inflateView = inflateSafely()
                    this@with.findViewWithId<View>(R.id.title_bar)?.setTag(parentId, inflateView)
                }
            }
        } else {
            findViewWithId<ViewStub>(parentId)?.run {
                if (isInflated()) {
                    (this@with.findViewWithId<View>(R.id.title_bar)
                        ?.getTag(parentId) as? View)?.visibility =
                        View.GONE
                }
            }
        }
    }
}

fun TitleBarDelegate.back(
    @IdRes id: Int = R.id.tv_back,
    @StringRes resId: Int? = null,
    @Nullable text: CharSequence? = null,
    @DrawableRes drawableResId: Int = R.drawable.ic_common_back_blue,
    @Nullable drawable: Drawable? = null,
    hasBackIcon: Boolean = true,
    hasBack: Boolean = true,
    autoBack: Boolean = true,
    @Nullable backAction: ((View) -> Unit)? = null
) {
    with(titleBar) {
        if (hasBack) {
            findViewWithId<TextView>(id)?.apply {
                setOnAntiShakeClickListener {
                    backAction?.invoke(it)
                    if (autoBack) hostActivity()?.onBackPressed()
                }
                setTextExt(resId, text)
                if (hasBackIcon) {
                    drawable ?: drawableResId.let { ContextCompat.getDrawable(context, it) }?.let {
                        setCompoundDrawablesRelativeWithIntrinsicBounds(it, null, null, null)
                    }
                } else {
                    setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null)
                }
                visibility = View.VISIBLE
            }
        } else {
            findViewWithId<TextView>(id)?.apply {
                if (hasBackIcon) {
                    (drawable ?: drawableResId.let {
                        ContextCompat.getDrawable(
                            context,
                            it
                        )
                    })?.let {
                        setCompoundDrawablesRelativeWithIntrinsicBounds(it, null, null, null)
                    }
                } else {
                    setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null)
                }
                visibility = View.GONE
            }
        }
    }

}

fun TitleBarDelegate.close(
    @IdRes id: Int = R.id.bt_close,
    @IdRes parentId: Int = R.id.vs_close_button,
    hasCloseButton: Boolean = true,
    @Nullable closeAction: ((View) -> Unit)? = null
) {
    with(titleBar) {
        if (hasCloseButton) {
            findViewWithId<ViewStub>(parentId)?.run {
                if (isInflated()) {
                    (this@with.findViewWithId<View>(R.id.title_bar)?.getTag(id) as? View)?.run {
                        visibility = View.VISIBLE
                        setOnAntiShakeClickListener {
                            closeAction?.invoke(it)
                        }
                    }
                } else {
                    val inflateView = inflateSafely().run {
                        setOnAntiShakeClickListener {
                            closeAction?.invoke(it)
                        }
                    }
                    this@with.findViewWithId<View>(R.id.title_bar)?.setTag(id, inflateView)
                }
            }
        } else {
            findViewWithId<ViewStub>(parentId)?.run {
                if (isInflated()) {
                    (this@with.findViewWithId<View>(R.id.title_bar)
                        ?.getTag(id) as? View)?.visibility =
                        View.GONE
                }
            }
        }
    }

}



