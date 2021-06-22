package com.zenchn.widget

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.TextUtils
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

/**
 * 描    述：圆形图片，支持文字
 * 修订记录：
 *
 * @author hzj
 */
class CircleTextImageView : AppCompatImageView {
    private val mDrawableRect = RectF()
    private val mBorderRect = RectF()
    private val mShaderMatrix = Matrix()
    private val mBitmapPaint = Paint()
    private val mBorderPaint = Paint()
    private val mFillPaint = Paint()
    private val mTextPaint = Paint()
    private var mBorderColor = DEFAULT_BORDER_COLOR
    private var mBorderWidth = DEFAULT_BORDER_WIDTH
    private var mFillColor = DEFAULT_FILL_COLOR
    private var textString: String? = null
    private var mTextColor = DEFAULT_TEXT_COLOR
    private var mTextSize = DEFAULT_TEXT_SIZE
    private var mTextPadding = DEFAULT_TEXT_PADDING
    private var mBitmap: Bitmap? = null
    private var mBitmapShader: BitmapShader? = null
    private var mBitmapWidth = 0
    private var mBitmapHeight = 0
    private var mDrawableRadius = 0f
    private var mBorderRadius = 0f
    private var mColorFilter: ColorFilter? = null
    private var mReady = false
    private var mSetupPending = false
    private var mBorderOverlay = false

    constructor(context: Context) : super(context) {
        init()
    }

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int = 0) : super(context, attrs, defStyle) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.CircleTextImageView, defStyle, 0)
        mBorderWidth = a.getDimensionPixelSize(R.styleable.CircleTextImageView_citv_border_width, DEFAULT_BORDER_WIDTH)
        mBorderColor = a.getColor(R.styleable.CircleTextImageView_citv_border_color, DEFAULT_BORDER_COLOR)
        mBorderOverlay = a.getBoolean(R.styleable.CircleTextImageView_citv_border_overlay, DEFAULT_BORDER_OVERLAY)
        mFillColor = a.getColor(R.styleable.CircleTextImageView_citv_fill_color, DEFAULT_FILL_COLOR)
        textString = a.getString(R.styleable.CircleTextImageView_citv_text_text)
        mTextColor = a.getColor(R.styleable.CircleTextImageView_citv_border_color, DEFAULT_TEXT_COLOR)
        mTextSize = a.getDimensionPixelSize(R.styleable.CircleTextImageView_citv_text_size, DEFAULT_TEXT_SIZE)
        mTextPadding = a.getDimensionPixelSize(R.styleable.CircleTextImageView_citv_text_padding, DEFAULT_TEXT_PADDING)
        a.recycle()
        init()
    }

    private fun init() {
        super.setScaleType(SCALE_TYPE)
        mReady = true
        if (mSetupPending) {
            setup()
            mSetupPending = false
        }
    }

    override fun getScaleType(): ScaleType {
        return SCALE_TYPE
    }

    override fun setScaleType(scaleType: ScaleType) {
        require(scaleType == SCALE_TYPE) {
            String.format(
                "ScaleType %s not supported.",
                scaleType
            )
        }
    }

    override fun setAdjustViewBounds(adjustViewBounds: Boolean) {
        require(!adjustViewBounds) { "adjustViewBounds not supported." }
    }

    override fun onDraw(canvas: Canvas) {
        if (mBitmap == null && TextUtils.isEmpty(textString)) {
            return
        }
        if (mFillColor != Color.TRANSPARENT) {
            canvas.drawCircle(width / 2.0f, height / 2.0f, mDrawableRadius, mFillPaint)
        }
        if (mBitmap != null) {
            canvas.drawCircle(width / 2.0f, height / 2.0f, mDrawableRadius, mBitmapPaint)
        }
        if (mBorderWidth != 0) {
            canvas.drawCircle(width / 2.0f, height / 2.0f, mBorderRadius, mBorderPaint)
        }
        if (!TextUtils.isEmpty(textString)) {
            val fm = mTextPaint.fontMetricsInt
            canvas.drawText(
                textString!!,
                width / 2 - mTextPaint.measureText(textString) / 2,
                height / 2 - fm.descent + (fm.bottom - fm.top) / 2.toFloat(), mTextPaint
            )
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setup()
    }

    fun setText(TextResId: Int) {
        setText(resources.getString(TextResId))
    }

    fun setText(textString: String) {
        this.textString = textString
        invalidate()
    }

    var textColor: Int
        get() = mTextColor
        set(mTextColor) {
            this.mTextColor = mTextColor
            mTextPaint.color = mTextColor
            invalidate()
        }

    fun setTextColorResource(colorResource: Int) {
        textColor = resources.getColor(colorResource)
    }

    var textSize: Int
        get() = mTextSize
        set(textSize) {
            mTextSize = textSize
            mTextPaint.textSize = textSize.toFloat()
            invalidate()
        }
    var textPadding: Int
        get() = mTextPadding
        set(mTextPadding) {
            this.mTextPadding = mTextPadding
            invalidate()
        }
    var borderColor: Int
        get() = mBorderColor
        set(borderColor) {
            if (borderColor == mBorderColor) {
                return
            }
            mBorderColor = borderColor
            mBorderPaint.color = mBorderColor
            invalidate()
        }

    fun setBorderColorResource(borderColorRes: Int) {
        borderColor = context.resources.getColor(borderColorRes)
    }

    var fillColor: Int
        get() = mFillColor
        set(fillColor) {
            if (fillColor == mFillColor) {
                return
            }
            mFillColor = fillColor
            mFillPaint.color = fillColor
            invalidate()
        }

    fun setFillColorResource(fillColorRes: Int) {
        fillColor = context.resources.getColor(fillColorRes)
    }

    var borderWidth: Int
        get() = mBorderWidth
        set(borderWidth) {
            if (borderWidth == mBorderWidth) {
                return
            }
            mBorderWidth = borderWidth
            setup()
        }
    var isBorderOverlay: Boolean
        get() = mBorderOverlay
        set(borderOverlay) {
            if (borderOverlay == mBorderOverlay) {
                return
            }
            mBorderOverlay = borderOverlay
            setup()
        }

    override fun setImageBitmap(bm: Bitmap) {
        super.setImageBitmap(bm)
        mBitmap = bm
        setup()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        mBitmap = getBitmapFromDrawable(drawable)
        setup()
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        mBitmap = getBitmapFromDrawable(drawable)
        setup()
    }

    override fun setImageURI(uri: Uri?) {
        super.setImageURI(uri)
        mBitmap = if (uri != null) getBitmapFromDrawable(drawable) else null
        setup()
    }

    override fun setColorFilter(cf: ColorFilter) {
        if (cf === mColorFilter) {
            return
        }
        mColorFilter = cf
        mBitmapPaint.colorFilter = mColorFilter
        invalidate()
    }

    private fun getBitmapFromDrawable(drawable: Drawable?): Bitmap? {
        if (drawable == null) {
            return null
        }
        return if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else try {
            val bitmap: Bitmap = if (drawable is ColorDrawable) {
                Bitmap.createBitmap(COLOR_DRAWABLE_DIMENSION, COLOR_DRAWABLE_DIMENSION, BITMAP_CONFIG)
            } else {
                Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, BITMAP_CONFIG)
            }
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun setup() {
        if (!mReady) {
            mSetupPending = true
            return
        }
        if (width == 0 && height == 0) {
            return
        }
        if (mBitmap == null && TextUtils.isEmpty(textString)) {
            invalidate()
            return
        }
        mTextPaint.isAntiAlias = true
        mTextPaint.color = mTextColor
        mTextPaint.textSize = mTextSize.toFloat()
        mBorderPaint.style = Paint.Style.STROKE
        mBorderPaint.isAntiAlias = true
        mBorderPaint.color = mBorderColor
        mBorderPaint.strokeWidth = mBorderWidth.toFloat()
        mFillPaint.style = Paint.Style.FILL
        mFillPaint.isAntiAlias = true
        mFillPaint.color = mFillColor
        mBorderRect[0f, 0f, width.toFloat()] = height.toFloat()
        mBorderRadius =
            Math.min((mBorderRect.height() - mBorderWidth) / 2.0f, (mBorderRect.width() - mBorderWidth) / 2.0f)
        mDrawableRect.set(mBorderRect)
        if (!mBorderOverlay) {
            mDrawableRect.inset(mBorderWidth.toFloat(), mBorderWidth.toFloat())
        }
        mDrawableRadius = Math.min(mDrawableRect.height() / 2.0f, mDrawableRect.width() / 2.0f)
        if (mBitmap != null) {
            mBitmapShader = BitmapShader(mBitmap!!, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            mBitmapHeight = mBitmap!!.height
            mBitmapWidth = mBitmap!!.width
            mBitmapPaint.isAntiAlias = true
            mBitmapPaint.shader = mBitmapShader
            updateShaderMatrix()
        }
        invalidate()
    }

    private fun updateShaderMatrix() {
        val scale: Float
        var dx = 0f
        var dy = 0f
        mShaderMatrix.set(null)
        if (mBitmapWidth * mDrawableRect.height() > mDrawableRect.width() * mBitmapHeight) {
            scale = mDrawableRect.height() / mBitmapHeight.toFloat()
            dx = (mDrawableRect.width() - mBitmapWidth * scale) * 0.5f
        } else {
            scale = mDrawableRect.width() / mBitmapWidth.toFloat()
            dy = (mDrawableRect.height() - mBitmapHeight * scale) * 0.5f
        }
        mShaderMatrix.setScale(scale, scale)
        mShaderMatrix.postTranslate((dx + 0.5f).toInt() + mDrawableRect.left, (dy + 0.5f).toInt() + mDrawableRect.top)
        mBitmapShader!!.setLocalMatrix(mShaderMatrix)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthMeasureSpecMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthMeasureSpecSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMeasureSpecMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightMeasureSpecSize = MeasureSpec.getSize(heightMeasureSpec)
        if (!TextUtils.isEmpty(textString)) {
            var textMeasuredSize = mTextPaint.measureText(textString).toInt()
            textMeasuredSize += 2 * mTextPadding
            if (widthMeasureSpecMode == MeasureSpec.AT_MOST && heightMeasureSpecMode == MeasureSpec.AT_MOST) {
                if (textMeasuredSize > measuredWidth || textMeasuredSize > measuredHeight) {
                    setMeasuredDimension(textMeasuredSize, textMeasuredSize)
                }
            }
        }
    }

    companion object {
        private val SCALE_TYPE = ScaleType.CENTER_CROP
        private val BITMAP_CONFIG = Bitmap.Config.ARGB_8888
        private const val COLOR_DRAWABLE_DIMENSION = 2
        private const val DEFAULT_BORDER_WIDTH = 0
        private const val DEFAULT_BORDER_COLOR = Color.BLACK
        private const val DEFAULT_FILL_COLOR = Color.TRANSPARENT
        private const val DEFAULT_TEXT_COLOR = Color.BLACK
        private const val DEFAULT_TEXT_SIZE = 22
        private const val DEFAULT_TEXT_PADDING = 4
        private const val DEFAULT_BORDER_OVERLAY = false
    }
}