package com.zenchn.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.jetbrains.annotations.Nullable
import kotlin.math.abs


interface IRefreshLayout : ILayout

fun IRefreshLayout.initRefreshLayout(
        @IdRes refreshLayoutId: Int,
        @ColorInt colors: Array<Int> = arrayOf(
                ContextCompat.getColor(hostActivity()!!, R.color.color_Primary)
        ),
        @Nullable listener: (() -> Unit)? = null
) {
    findViewWithId<RefreshLayout>(refreshLayoutId)?.init(colors, listener)
}

fun RefreshLayout.init(
        @ColorInt colors: Array<Int>,
        @Nullable listener: (() -> Unit)?
) {
    setColorSchemeColors(*colors.toIntArray())
    listener?.let { setOnRefreshListener { it.invoke() } }
}

fun IRefreshLayout.hideRefreshing(@IdRes refreshLayoutId: Int) {
    findViewWithId<RefreshLayout>(refreshLayoutId)?.hideRefreshing()
}

fun IRefreshLayout.bindRefreshingStatus(@IdRes refreshLayoutId: Int, isRefreshing: Boolean) {
    findViewWithId<RefreshLayout>(refreshLayoutId)?.isRefreshing = isRefreshing
}


open class RefreshLayout : SwipeRefreshLayout {

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

}

class VerticalSwipeRefreshLayout : RefreshLayout {

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    private var startX: Float = 0.toFloat()
    private var startY: Float = 0.toFloat()

    private var isXMove: Boolean = false// 是否横向拖拽

    // getScaledTouchSlop()得来的一个距离，表示滑动的时候，手势移动要大于这个距离才开始移动控件，ViewPager就是用这个距离来判断用户是否翻页
    private val touchSlop: Int = ViewConfiguration.get(context).scaledTouchSlop

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                isXMove = false
                startX = ev.x
                startY = ev.y
            }
            MotionEvent.ACTION_MOVE -> {
                // 如果横向移动则不拦截，直接return false；
                if (isXMove) {
                    return false
                }
                val endX = ev.x
                val endY = ev.y
                val distanceX = abs(endX - startX)
                val distanceY = abs(endY - startY)
                // 如果dx>xy，则认定为左右滑动，将事件交给viewPager处理，return false
                if (distanceX > touchSlop && distanceX > distanceY) {
                    isXMove = true
                    return false
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isXMove = false
        }
        // 如果dy>dx，则认定为下拉事件，交给swipeRefreshLayout处理并拦截
        return super.onInterceptTouchEvent(ev)
    }
}

