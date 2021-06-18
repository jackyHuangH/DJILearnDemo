package com.zenchn.djilearndemo.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.gyf.immersionbar.ImmersionBar
import com.gyf.immersionbar.OnKeyboardListener
import com.jacky.support.base.DefaultUiController
import com.jacky.support.base.IUiController
import com.jacky.support.utils.KeyboardUtils
import com.zenchn.djilearndemo.R

/**
 * @author:Hzj
 * @date  :2020/5/26
 * desc  ：
 * record：
 */

abstract class BaseFragment : Fragment(), IView {

    protected lateinit var mImmersionBar: ImmersionBar
    protected var mUiDelegate: IUiController? = null
    private var rootView: View? = null
    protected var instanceState: Bundle? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mUiDelegate = context as? IUiController ?: DefaultUiController(context, this)
    }

    override fun <V : View> findViewWithId(viewId: Int): V = activity!!.findViewById<V>(viewId)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            getLayoutId().takeIf { it > 0 }?.let {
                rootView = inflater.inflate(it, null)
            }
        }
        //懒加载配置
        mIsViewPrepare = true
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onNewInstanceState(savedInstanceState)
        //注意：懒加载模式时不要使用initWidget()，容易出现重复初始化问题，
        // 使用onFragmentFirstVisible()
        initWidget()
        if (isStatusBarEnabled()) {
            initStatusBar()
        }
        initLifecycleObserver()
    }

    override fun initWidget() {
    }

    open fun initLifecycleObserver() {}

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        onNewInstanceState(savedInstanceState)
    }

    @CallSuper
    protected open fun onNewInstanceState(savedInstanceState: Bundle?) {
        this.instanceState = savedInstanceState
    }

    /**
     * 是否在Fragment使用沉浸式
     *
     * @return the boolean
     */
    protected open fun isStatusBarEnabled(): Boolean = true

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

    /**
     * 模拟activity的onBackPressed()事件
     * 方便fragment 调用
     */
    protected fun onFragmentBackPressed() {
        activity?.onBackPressed()
    }

    override fun onApiGrantRefuse() {
//        ApplicationKit.instance.navigateToLogin(true)
    }

    override fun onApiFailure(msg: String) {
        showMessage(msg)
    }

    override fun showProgress() {
        mUiDelegate?.showProgress()
    }

    override fun showProgress(msg: CharSequence?) {
        mUiDelegate?.showProgress(msg)
    }

    override fun hideProgress() {
        mUiDelegate?.hideProgress()
    }

    override fun showMessage(msg: CharSequence) {
        mUiDelegate?.showMessage(msg)
    }

    override fun showResMessage(resId: Int) {
        mUiDelegate?.showResMessage(resId)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            mImmersionBar.init()
        }
    }

    override fun onResume() {
        super.onResume()
        //懒加载配置
        if (!isHidden && !mIsFragmentVisible && isResumed) {
            onFragmentVisibleChange(true)
        }
    }

    override fun onPause() {
        activity?.let { KeyboardUtils.hideSoftInput(it) }
        super.onPause()
        //懒加载配置
        if (mIsFragmentVisible && !isResumed) {
            onFragmentVisibleChange(false)
        }
    }

    //========================懒加载核心===========================
    /**
     * 视图是否加载完毕
     */
    private var mIsViewPrepare = false

    /**
     * 视图是否第一次展现
     */
    private var mFirstTimeVisible = false

    /**
     * 当前fragment是否可见，防止viewPager缓存机制导致回调函数触发
     */
    private var mIsFragmentVisible = false

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (rootView == null) {
            return
        }
        if (isVisibleToUser) {
            onFragmentVisibleChange(true)
            mIsFragmentVisible = true
            return
        }
        if (mIsFragmentVisible) {
            onFragmentVisibleChange(false)
            mIsFragmentVisible = false
        }
    }

    protected open fun onFragmentVisibleChange(isFragmentVisible: Boolean) {
        if (isResumed && mIsViewPrepare && !mFirstTimeVisible) {
            lazyLoad()
            mFirstTimeVisible = true
        }
        if (isFragmentVisible) {
            onSupportVisible()
        }
    }

    /**
     * fragment每次对用户可见时
     */
    open fun onSupportVisible() {}

    /**
     * 懒加载(此方法，在View的一次生命周期中只执行一次)
     */
    open fun lazyLoad() {}
}


/**
 * @author:Hzj
 * @date  :2019/6/28/028
 * desc  ：ViewModel封装基类Fragment
 * record：
 */
abstract class BaseVMFragment<VM : BaseViewModel> : BaseFragment(), IVMView<VM> {
    override lateinit var mViewModel: VM

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initViewModel()
        super.onViewCreated(view, savedInstanceState)
    }

    //初始化ViewModel绑定
    private fun initViewModel() {
        provideViewModelClass()?.let { clazz ->
            mViewModel = ViewModelProviders.of(this).get(clazz).apply {
                mErrorMsg.observe(this@BaseVMFragment, Observer { showMessage(msg = it) })
                mShowLoadingProgress.observe(this@BaseVMFragment, Observer { show ->
                    if (show) showProgress() else hideProgress()
                })
            }
        }
    }

    override fun initLifecycleObserver() {
        super.initLifecycleObserver()
        mViewModel.let(lifecycle::addObserver)
    }
}