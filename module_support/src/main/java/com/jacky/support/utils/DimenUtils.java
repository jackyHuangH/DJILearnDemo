package com.jacky.support.utils;

import android.content.res.Resources;
import android.util.DisplayMetrics;

/**
 * @author:Hzj
 * @date :2020/6/15
 * desc  ：Java Dimens方法
 * record：
 */
public class DimenUtils {
    public static final float DENSITY = Resources.getSystem().getDisplayMetrics().density;
    public static final float SCALED_DENSITY = Resources.getSystem().getDisplayMetrics().scaledDensity;

    /**
     * 获取DisplayMetrics对象
     *
     * @return
     */
    public static DisplayMetrics getDisPlayMetrics() {
        return Resources.getSystem().getDisplayMetrics();
    }

    /**
     * 获取屏幕的宽度（像素）
     *
     * @return
     */
    public static int getScreenWidth() {
        return getDisPlayMetrics().widthPixels;
    }

    /**
     * 获取屏幕的高（像素）
     *
     * @return
     */
    public static int getScreenHeight() {
        return getDisPlayMetrics().heightPixels;
    }

    /**
     * dp转px，保证尺寸大小不变
     *
     * @param dpValue
     * @return
     */
    public static int dp2px(int dpValue) {
        return Math.round(dpValue * DENSITY);
    }

    /**
     * px转dp，保证尺寸大小不变
     *
     * @param pxValue
     * @return
     */
    public static int px2dp(int pxValue) {
        return Math.round(pxValue / DENSITY);
    }

    /**
     * px转sp，保证尺寸大小不变
     *
     * @param pxValue
     * @return
     */
    public static int px2sp(float pxValue) {
        return Math.round(pxValue / SCALED_DENSITY);
    }

    /**
     * sp转px，保证尺寸大小不变
     *
     * @param spValue
     * @return
     */
    public static int sp2px(float spValue) {
        return Math.round(spValue * SCALED_DENSITY);
    }

    /**
     * 获取状态栏高度——方法1
     */
    public static int getStatusBarHeight() {
        int statusBarHeight = 0;
        //获取status_bar_height资源的ID
        int resourceId = Resources.getSystem().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            //根据资源ID获取响应的尺寸值
            statusBarHeight = Resources.getSystem().getDimensionPixelSize(resourceId);
        }
        return statusBarHeight;
    }
}
