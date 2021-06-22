@file:Suppress("DEPRECATION")

package com.zenchn.widget

import android.annotation.TargetApi
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Outline
import android.os.Build
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.ViewStub
import android.view.inputmethod.InputMethodManager
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.core.graphics.BitmapCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.textfield.TextInputLayout
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable


inline fun View.setOnAntiShakeClickListener(
    intervalMillis: Long = 200L,
    crossinline interceptor: ((View) -> Boolean) = { true },
    crossinline listener: (View) -> Unit
) {
    var lastClickTime: Long = 0
    setOnClickListener {
        if (interceptor.invoke(it)) {
            val currentTime = SystemClock.elapsedRealtime()
            val varTime = currentTime - lastClickTime
            if (varTime >= intervalMillis) {
                lastClickTime = currentTime
                listener(it)
            }
        }
    }
}

inline fun CompoundButton.setOnCheckedChangeListener(
    crossinline listener: (View, Boolean, Boolean) -> Unit
) {
    setOnCheckedChangeListener { bt, isChecked ->
        listener.invoke(bt, isChecked, bt.isPressed)
    }
}

fun View.restartRunnable(
    @NotNull runnable: Runnable,
    delayMillis: Long = 0L
) = apply {
    removeCallbacks(runnable)
    if (delayMillis > 0L) postDelayed(runnable, delayMillis)
    else post(runnable)
}

fun SwipeRefreshLayout.hideRefreshing() =
    this.takeIf { it.isRefreshing }?.let { it.isRefreshing = false }

fun ViewStub.isInflated(): Boolean = parent == null

fun ViewStub.inflateSafely(): View? = this.takeUnless { it.isInflated() }?.inflate()

fun TextView.setTextColorRes(@ColorRes colorRes: Int) {
    setTextColor(resources.getColor(colorRes))
}

fun TextView.setTextExt(
    @StringRes resId: Int? = null,
    @Nullable text: CharSequence? = null
) {
    val titleString = if (resId != null) {
        context.resources.getString(resId)
    } else {
        text?.toString().orEmpty()
    }
    setText(titleString)
}

fun EditText.inputText(trim: Boolean = true) = this.text?.let {
    val temp = it.toString()
    if (trim) temp.trim()
    temp
}

fun TextInputLayout.requestInputFocus(selectionEnd: Boolean = false) =
    this.editText?.requestInputFocus(selectionEnd)

fun EditText.requestInputFocus(selectionEnd: Boolean = false, delayMillis: Long = 200) {
    apply {
        isFocusable = true
        isFocusableInTouchMode = true
        if (selectionEnd) setSelection(text.length)
        postDelayed({
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.apply {
                showSoftInput(this@requestInputFocus, 0)
            }
        }, delayMillis)
        requestFocus()
    }
}

inline fun EditText.addTextWatcher(
    intervalMillis: Long = 200,
    watcherKey: Int? = null,
    @Nullable noinline before: ((s: CharSequence?, start: Int, count: Int, after: Int) -> Unit)? = null,
    @Nullable noinline after: ((s: Editable?) -> Unit)? = null,
    @NotNull crossinline onTextChanged: (s: CharSequence?, start: Int, before: Int, count: Int) -> Unit
) {
    val newTextWatcher = object : TextWatcher {

        private var lastChangedTime: Long = 0

        private var runnable: Runnable? = null

        override fun afterTextChanged(s: Editable?) = after?.invoke(s) ?: Unit

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) =
            before?.invoke(s, start, count, after) ?: Unit

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            runnable?.let { removeCallbacks(it) }
            runnable = Runnable { onTextChanged.invoke(editableText, start, before, count) }
            postDelayed(runnable, intervalMillis)
        }

    }
    watcherKey?.let {
        (getTag(it) as? TextWatcher)?.let { preTextWatcher ->
            removeTextChangedListener(preTextWatcher)
        }
        setTag(it, newTextWatcher)
    }
    addTextChangedListener(newTextWatcher)
}

fun EditText.removeTextWatcher(watcherKey: Int) {
    (getTag(watcherKey) as? TextWatcher)?.let { preTextWatcher ->
        removeTextChangedListener(preTextWatcher)
    }
}

//解决ScrollView嵌套edittext内部滑动冲突
fun EditText.solveScrollViewClash() {
    /**
     * EditText竖直方向能否够滚动
     * @param editText  须要推断的EditText
     * @return  true：能够滚动   false：不能够滚动
     */
    fun canVerticalScroll(): Boolean {
        //滚动的距离
        val scrollY = scrollY
        //控件内容的总高度
        val scrollRange = layout.height
        //控件实际显示的高度
        val scrollExtent = height - compoundPaddingTop - compoundPaddingBottom
        //控件内容总高度与实际显示高度的差值
        val scrollDifference = scrollRange - scrollExtent
        if (scrollDifference == 0) {
            return false
        }
        return (scrollY > 0) || (scrollY < scrollDifference - 1)
    }

    setOnTouchListener { view, event ->
        //触摸的是EditText而且当前EditText能够滚动则将事件交给EditText处理。否则将事件交由其父类处理
        if ((view.id == id && canVerticalScroll())) {
            view.parent.requestDisallowInterceptTouchEvent(true)
            if (event.action == MotionEvent.ACTION_UP) {
                view.parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        false
    }
}

fun <T : View> View.childViewExt(
    @IdRes childViewId: Int,
    extra: T.() -> Unit
) = findViewById<T>(childViewId)?.run {
    extra.invoke(this)
}

fun View.childViewStubExt(
    @IdRes childViewId: Int,
    visible: Boolean,
    extra: (View.() -> Unit)? = null
) {
    findViewById<View>(childViewId)?.run {
        if (visible) {
            if (this is ViewStub) {
                inflateSafely()?.apply {
                    id = childViewId
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

inline fun View.childViewClickListenerExt(
    @IdRes childViewId: Int,
    crossinline interceptor: ((View) -> Boolean) = { true },
    crossinline listener: (View) -> Unit
) = findViewById<View>(childViewId)?.setOnAntiShakeClickListener(
    interceptor = interceptor,
    listener = listener
)

fun View.childViewVisibleExt(@IdRes childViewId: Int, visible: Boolean) {
    findViewById<View>(childViewId)?.visibility = (if (visible) View.VISIBLE else View.GONE)
}

fun <V1 : View, V2 : View> View.childViewLinkExt(
    @IdRes viewId1: Int,
    @IdRes viewId2: Int,
    watch: (V1, V2) -> Unit
) = findViewById<V1>(viewId1)?.let { v1 ->
    findViewById<V2>(viewId2)?.let { v2 ->
        watch.invoke(v1, v2)
    }
}

fun View.viewVisibleExt(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}

//fun Dialog.viewVisibleExt(
//        @IdRes viewId: Int,
//        visible: Boolean
//) {
//    findViewById<View>(viewId)?.visibility = (if (visible) View.VISIBLE else View.GONE)
//}

inline fun Dialog.viewClickListenerExt(
    @IdRes viewId: Int,
    crossinline interceptor: ((View) -> Boolean) = { true },
    crossinline listener: (View) -> Unit
) = findViewById<View>(viewId)?.setOnAntiShakeClickListener(
    interceptor = interceptor,
    listener = listener
)

/**
 * 设置视图裁剪的圆角半径
 * @param radiusInPx
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
fun View.setClipViewCornerRadius(radiusInPx: Int) {
    if (radiusInPx <= 0) return
    this.apply {
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radiusInPx.toFloat())
            }
        }
        clipToOutline = true
    }
}

/**
 *View 转Bitmap
 */
fun View.convertMeasuredViewToBitmap(): Bitmap {
    this.measure(
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    )
    this.layout(0, 0, this.measuredWidth, this.measuredHeight)
    this.isDrawingCacheEnabled = true
    this.buildDrawingCache() //启用DrawingCache并创建位图
    val bitmap = Bitmap.createBitmap(this.drawingCache) //创建一个DrawingCache的拷贝，因为DrawingCache得到的位图在禁用后会被回收
    this.isDrawingCacheEnabled = false //禁用DrawingCahce否则会影响性能
    return bitmap
}