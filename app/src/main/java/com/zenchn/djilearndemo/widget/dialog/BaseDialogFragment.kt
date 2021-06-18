package com.zenchn.djilearndemo.widget.dialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment

/**
 * @author:Hzj
 * @date  :2021/6/4
 * desc  ：DialogFragment封装
 * record：
 */
abstract class BaseDialogFragment : DialogFragment() {
    private var mDefaultWidth = WindowManager.LayoutParams.MATCH_PARENT //宽
    private var mDefaultHeight = WindowManager.LayoutParams.WRAP_CONTENT //高
    private var mDefaultGravity = Gravity.CENTER //位置
    private var mCancelable = false //是否可取消
    private var mCanceledOnTouchOutside = false //点击外部是否可取消
    private lateinit var mDialog: Dialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val mView = inflater.inflate(layoutId, container, false)
        initViews(mView)
        return mView
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mDialog = super.onCreateDialog(savedInstanceState)
        //初始化
        mDialog.apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCanceledOnTouchOutside(mCanceledOnTouchOutside)
            setCancelable(mCancelable)
        }
        val window = mDialog.window
        if (null != window) {
            window.decorView.setPadding(0, 0, 0, 0)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val lp = window.attributes
            lp.width = mDefaultWidth
            lp.height = mDefaultHeight
            lp.gravity = mDefaultGravity
            lp.windowAnimations = android.R.style.Animation_InputMethod
            window.attributes = lp
        }
        mDialog.setOnKeyListener { dialog, keyCode, event -> !mCancelable }
        return mDialog
    }

    /**
     * 设置位置
     *
     * @param gravity
     */
    fun setGravity(gravity: Int): BaseDialogFragment {
        mDefaultGravity = gravity
        return this
    }

    /**
     * 设置宽
     *
     * @param width
     */
    fun setWidth(width: Int): BaseDialogFragment {
        mDefaultWidth = width
        return this
    }

    /**
     * 设置高
     *
     * @param height
     */
    fun setHeight(height: Int): BaseDialogFragment {
        mDefaultHeight = height
        return this
    }

    /**
     * 设置点击返回按钮是否可取消
     *
     * @param cancelable
     */
    fun setIsCancelable(cancelable: Boolean): BaseDialogFragment {
        super.setCancelable(cancelable)
        mCancelable = cancelable
        return this
    }

    /**
     * 设置点击外部是否可取消
     *
     * @param canceledOnTouchOutside
     */
    fun setCanceledOnTouchOutside(canceledOnTouchOutside: Boolean): BaseDialogFragment {
        mCanceledOnTouchOutside = canceledOnTouchOutside
        return this
    }

    /**
     * 设置布局
     *
     * @return
     */
    protected abstract val layoutId: Int

    /**
     * 初始化Views
     *
     * @param v
     */
    protected abstract fun initViews(v: View)
}
