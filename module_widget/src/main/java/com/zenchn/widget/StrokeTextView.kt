package com.zenchn.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat


/**
 * @author:Hzj
 * @date  :2020/9/30
 * desc  ：TextView描边效果
 * record：
 */
class StrokeTextView : AppCompatTextView {
    private val outLineTextView: AppCompatTextView

    @ColorRes
    private var outLineColor: Int = R.color.white//默认白色描边

    constructor(context: Context) : super(context) {
        outLineTextView = AppCompatTextView(context)
        init()
    }

    constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs) {
        outLineTextView = AppCompatTextView(context, attrs)
        init()
    }

    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        outLineTextView = AppCompatTextView(context, attrs, defStyleAttr)
        init()
    }

    private fun init() {
        val paint = outLineTextView.paint
        paint.strokeWidth = 4f
        paint.style = Paint.Style.STROKE
        outLineTextView.setTextColor(ContextCompat.getColor(context, outLineColor))
        outLineTextView.gravity = gravity
        outLineTextView.textSize = 10F
    }

    override fun setLayoutParams(params: ViewGroup.LayoutParams?) {
        super.setLayoutParams(params)
        outLineTextView.layoutParams = params
    }

    override fun setTextSize(size: Float) {
        super.setTextSize(size)
        outLineTextView.textSize = size
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val outLineText = outLineTextView.text
        if (outLineText == null || outLineText != this.text) {
            outLineTextView.text = this.text
            postInvalidate()
        }
        outLineTextView.measure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        outLineTextView.layout(left, top, right, bottom)
    }

    override fun onDraw(canvas: Canvas?) {
        //描边文字先绘制在底部
        outLineTextView.draw(canvas)
        super.onDraw(canvas)
    }

    //----------public-------------
    /**
     * 设置描边字体大小
     */
    fun setOutLineTextSize(size: Float) {
        outLineTextView.textSize = size
    }

    /**
     * 设置描边字体颜色，默认白色
     */
    fun setOutLineColor(@ColorRes colorRes: Int) {
        outLineColor = colorRes
        outLineTextView.setTextColor(ContextCompat.getColor(context, outLineColor))
    }

    /**
     * 设置描边线条宽度
     */
    fun setStrokeWidth(width: Float) {
        val paint = outLineTextView.paint
        paint.strokeWidth = width
    }
}