package com.zenchn.djilearndemo.base

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.gyf.immersionbar.ImmersionBar
import com.gyf.immersionbar.OnKeyboardListener
import com.jacky.support.base.DefaultUiController
import com.jacky.support.base.IUiController
import com.jacky.support.utils.KeyboardUtils
import com.zenchn.djilearndemo.R
import com.zenchn.djilearndemo.app.GlobalLifecycleObserver

/**
 * @author:Hzj
 * @date  :2020/5/26
 * desc  ：
 * record：
 */
abstract class BaseActivity : AppCompatActivity(), IView {

    protected lateinit var mImmersionBar: ImmersionBar
    protected var instanceState: Bundle? = null
    protected val mUiDelegate: IUiController by lazy {
        DefaultUiController(this, this)
    }

    override fun <V : View> findViewWithId(viewId: Int): V = findViewById<V>(viewId)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onNewInstanceState(savedInstanceState)
        getLayoutId().takeIf { it > 0 }?.let { setContentView(it) }
        initWidget()
        initStatusBar()
        initLifecycleObserver()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        onNewInstanceState(savedInstanceState)
    }

    @CallSuper
    protected open fun onNewInstanceState(savedInstanceState: Bundle?) {
        this.instanceState = savedInstanceState
    }

    @CallSuper
    open fun initLifecycleObserver() {
        lifecycle.addObserver(GlobalLifecycleObserver.INSTANCE)
    }

    protected open fun initStatusBar() {
        mImmersionBar = ImmersionBar.with(this).apply {
            fitsSystemWindows(true)
            statusBarColor(R.color.white)
            statusBarDarkFont(true, 0.2f)
            //是否需要监听键盘
            if (addOnKeyboardListener() != null) {
                keyboardEnable(true)
                //单独指定软键盘模式
                keyboardMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                setOnKeyboardListener(addOnKeyboardListener())
            }
            init()
        }
    }

    protected open fun addOnKeyboardListener(): OnKeyboardListener? = null

    override fun onApiFailure(msg: String) {
        showMessage(msg)
    }

    override fun onApiGrantRefuse() {
//        MyApplication.navigateToLogin(true)
    }

    override fun onPause() {
        KeyboardUtils.hideSoftInput(this)
        super.onPause()
    }

    override fun showProgress() {
        mUiDelegate.showProgress()
    }

    override fun showProgress(msg: CharSequence?) {
        mUiDelegate.showProgress(msg)
    }

    override fun hideProgress() {
        mUiDelegate.hideProgress()
    }

    override fun showMessage(msg: CharSequence) {
        mUiDelegate.showMessage(msg)
    }

    override fun showResMessage(resId: Int) {
        mUiDelegate.showResMessage(resId)
    }
}

abstract class BaseVMActivity<VM : BaseViewModel> : BaseActivity(), IVMView<VM> {

    override lateinit var mViewModel: VM

    override fun onCreate(savedInstanceState: Bundle?) {
        initViewModel()
        super.onCreate(savedInstanceState)
    }

    //初始化ViewModel绑定
    private fun initViewModel() {
        provideViewModelClass()?.let { clazz ->
            mViewModel = ViewModelProviders.of(this).get(clazz).apply {
                mErrorMsg.observe(this@BaseVMActivity, Observer { showMessage(msg = it) })
                mShowLoadingProgress.observe(this@BaseVMActivity, Observer { show ->
                    if (show) showProgress() else hideProgress()
                })
            }
        }
    }

    override fun initLifecycleObserver() {
        super.initLifecycleObserver()
        mViewModel.let(lifecycle::addObserver)
    }

    override fun onApiFailure(msg: String) {
        hideProgress()
        super.onApiFailure(msg)
    }
}