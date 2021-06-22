package com.zenchn.widget

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.annotation.ColorRes
import androidx.annotation.FloatRange
import androidx.fragment.app.Fragment
import com.gyf.immersionbar.ImmersionBar
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

interface IImmersionBar : ILayout

data class ImmersionBarDelegate(internal val immersionBar: ImmersionBar)

fun IImmersionBar.initImmersionBar(withDefault: Boolean = true, @Nullable delegate: (ImmersionBarDelegate.() -> Unit)? = null) {
    hostActivity()?.let { ImmersionBar.with(it) }?.apply {
        if (withDefault) {
            fitsSystemWindows(true)
            statusBarColor(android.R.color.white) //状态栏颜色，不写默认透明色
            navigationBarColor(android.R.color.black)//导航栏颜色，不写默认透明色
            statusBarDarkFont(true, 0.2f) //状态栏字体是深色，不写默认为亮色
            navigationBarEnable(true)//是否可以修改导航栏颜色，默认为true
            navigationBarWithKitkatEnable(true)//是否可以修改安卓4.4和emui3.1手机导航栏颜色，默认为true
        }
        delegate?.invoke(ImmersionBarDelegate(this))
    }?.init()
}

fun ImmersionBarDelegate.fitsSystemWindows(@NotNull fits: Boolean = true) {
    immersionBar.fitsSystemWindows(fits)
}

/**
 * 透明状态栏效果
 */
fun ImmersionBarDelegate.transparentStatusBarState(isDarkFont: Boolean = false) {
    immersionBar.fitsSystemWindows(false)
            .statusBarDarkFont(isDarkFont)
            .transparentStatusBar()
}

/** 增加View的paddingTop,增加的值为状态栏高度 (智能判断，并设置高度) */
fun ImmersionBarDelegate.setPaddingSmart(activity: Activity, view: View) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        val statusBarHeight = ImmersionBar.getStatusBarHeight(activity)
        val lp = view.layoutParams
        if (lp != null && lp.height > 0) {
            lp.height += statusBarHeight //增高
        }
        view.setPadding(view.paddingLeft,
                view.paddingTop + statusBarHeight,
                view.paddingRight,
                view.paddingBottom)
    }
}

/** 增加View的paddingTop,增加的值为状态栏高度 (智能判断，并设置高度) */
fun ImmersionBarDelegate.setPaddingSmart(fragment: Fragment, view: View) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        val statusBarHeight = ImmersionBar.getStatusBarHeight(fragment)
        val lp = view.layoutParams
        if (lp != null && lp.height > 0) {
            lp.height += statusBarHeight //增高
        }
        view.setPadding(view.paddingLeft,
                view.paddingTop + statusBarHeight,
                view.paddingRight,
                view.paddingBottom)
    }
}

fun ImmersionBarDelegate.navigationBarColor(
        @ColorRes navigationBarColor: Int,
        @NotNull navigationBarEnable: Boolean = true,
        @NotNull navigationBarWithKitkatEnable: Boolean = true
) {
    immersionBar.apply {
        navigationBarColor(navigationBarColor)//导航栏颜色，不写默认透明色
        navigationBarEnable(navigationBarEnable)//是否可以修改导航栏颜色，默认为true
        navigationBarWithKitkatEnable(navigationBarWithKitkatEnable)//是否可以修改安卓4.4和emui3.1手机导航栏颜色，默认为true
    }
}

fun ImmersionBarDelegate.statusBarColor(
        @ColorRes statusBarColor: Int,
        @NotNull isDarkFont: Boolean = true,
        @FloatRange(from = 0.0, to = 1.0) statusAlpha: Float = 0.2f
) {
    immersionBar.apply {
        statusBarColor(statusBarColor)//状态栏颜色，不写默认透明色
        statusBarDarkFont(isDarkFont, statusAlpha) //状态栏字体是深色，不写默认为亮色
    }
}

fun ImmersionBarDelegate.keyboardListener(@NotNull keyboardListener: (Boolean, Int) -> Unit) {
    immersionBar.apply {
        keyboardEnable(true)
        keyboardMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)  //单独指定软键盘模式
        setOnKeyboardListener(keyboardListener)
    }
}
