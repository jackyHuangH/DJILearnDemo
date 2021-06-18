package com.jacky.support.base

import android.view.View
import androidx.annotation.LayoutRes

/**
 * 作   者： by Hzj on 2017/12/13/013.
 * 描   述：
 * 修订记录：
 */
interface IActivity : IUiController {
    @LayoutRes
    fun getLayoutId(): Int

    fun initWidget()

    fun <V : View> findViewWithId(viewId: Int): V
}