package com.zenchn.widget

import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import org.jetbrains.annotations.Nullable

/**
 * 作   者：wangr on 2020/4/13 11:50 AM
 * 描   述：
 * 修订记录：
 */
interface ISegmentMenu : ILayout

data class SegmentMenuDelegate(val segmentMenu: ISegmentMenu)

fun ISegmentMenu.initSegmentMenu(
    @IdRes leftId: Int = R.id.tv_left_button,
    @IdRes rightId: Int = R.id.tv_right_button,
    @StringRes leftTitleResId: Int? = null,
    @StringRes rightTitleResId: Int? = null,
    leftTitle: CharSequence? = null,
    rightTitle: CharSequence? = null,
    @ColorRes normalTextColorRes: Int = R.color.color_Segment_Press,
    @ColorRes selectTextColorRes: Int = R.color.color_Segment_Normal,
    @DrawableRes leftNormalDrawableRes: Int = R.drawable.shape_segment_button_left_normal,
    @DrawableRes leftSelectDrawableRes: Int = R.drawable.shape_segment_button_left_pressed,
    @DrawableRes rightNormalDrawableRes: Int = R.drawable.shape_segment_button_right_normal,
    @DrawableRes rightSelectDrawableRes: Int = R.drawable.shape_segment_button_right_pressed,
    defaultSelectLeft: Boolean = true,
    @Nullable action: (View, Boolean) -> Unit,
    option: (SegmentMenuDelegate.() -> Unit)? = null
) {
    findViewWithId<View>(R.id.segment_menu)?.apply {
        tag = SegmentMenuDelegate(this@initSegmentMenu).apply {
            button(id = leftId, resId = leftTitleResId, text = leftTitle, action = { view ->
                (view as? TextView)?.run {
                    setTextColorRes(selectTextColorRes)
                    setBackgroundResource(leftSelectDrawableRes)
                }
                findViewById<TextView>(rightId)?.run {
                    setTextColorRes(normalTextColorRes)
                    setBackgroundResource(rightNormalDrawableRes)
                }
                action.invoke(view, true)
            })
            button(id = rightId, resId = rightTitleResId, text = rightTitle, action = { view ->
                (view as? TextView)?.run {
                    setTextColorRes(selectTextColorRes)
                    setBackgroundResource(rightSelectDrawableRes)
                }
                findViewById<TextView>(leftId)?.run {
                    setTextColorRes(normalTextColorRes)
                    setBackgroundResource(leftNormalDrawableRes)
                }
                action.invoke(view, false)
            })
            if (defaultSelectLeft) findViewById<View>(leftId)?.performClick()
            else findViewById<View>(rightId)?.performClick()
            option?.invoke(this)
        }
    }
}

private fun SegmentMenuDelegate.button(
    @IdRes id: Int,
    @StringRes resId: Int? = null,
    @Nullable text: CharSequence? = null,
    @Nullable extra: (TextView.() -> Unit)? = null,
    @Nullable action: (View) -> Unit
) {
    segmentMenu.findViewWithId<View>(R.id.segment_menu)?.apply {
        findViewById<TextView>(id)?.apply {
            setTextExt(resId, text)
            setOnAntiShakeClickListener {
                action.invoke(it)
            }
            extra?.invoke(this)
        }
    }
}

