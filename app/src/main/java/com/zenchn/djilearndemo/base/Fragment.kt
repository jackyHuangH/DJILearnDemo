package com.zenchn.djilearndemo.base

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.StyleRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.zenchn.common.ext.checkNotNull
import com.zenchn.common.ext.safelyRun
import com.zenchn.djilearndemo.R
import com.zenchn.widget.DefaultUiDelegate
import com.zenchn.widget.IUiDelegate
import org.jetbrains.annotations.NotNull

abstract class BaseFragment : Fragment(), IContract.Fragment {

    private var rootView: View? = null
    protected open var uiDelegate: IUiDelegate? = null
    override var hostActivity: IContract.Activity? = null
    override var instanceState: Bundle? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        uiDelegate = context as? IUiDelegate ?: DefaultUiDelegate(context)
        hostActivity = activity as? IContract.Activity
    }

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
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onNewInstanceState(savedInstanceState)
        initWidget()
        initLifecycleObserver()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        onNewInstanceState(savedInstanceState)
    }

    @CallSuper
    protected open fun onNewInstanceState(savedInstanceState: Bundle?) {
        this.instanceState = savedInstanceState
    }

    override fun initWidget() {}

    open fun initLifecycleObserver() {}

    override fun showMessage(msg: String?, msgResId: Int?) = uiDelegate?.showMessage(msg, msgResId)
            ?: Unit

    override fun showProgress(cancelable: Boolean) = uiDelegate?.showProgress(cancelable) ?: Unit

    override fun hideProgress() = uiDelegate?.hideProgress() ?: Unit

    override fun <T : View> findViewWithId(@IdRes id: Int): T? = safelyRun {
        hostRootView().findViewById(id)
    }

    @CallSuper
    override fun onNewIntent(intent: Intent?) {}

}

abstract class BaseVMFragment<VM : BaseViewModel> : BaseFragment(), IContract.VMFragment<VM> {

    override lateinit var viewModel: VM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = getViewModelClass()?.let { ViewModelProviders.of(this).get(it) }
                .checkNotNull { "${BaseVMFragment::class.java.canonicalName} error : Create viewModel exception." }
                .apply {
                    msgChannel.observe(this@BaseVMFragment, Observer { showMessage(msg = it) })
                    loadingChannel.observe(this@BaseVMFragment, Observer { show ->
                        if (show) showProgress()
                        else hideProgress()
                    })
                }
    }

    @CallSuper
    override fun initLifecycleObserver() {
        super.initLifecycleObserver()
        lifecycle.addObserver(viewModel)
    }

    @CallSuper
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        viewModel.onNewIntent(intent)
    }
}

abstract class BaseDialogFragment : DialogFragment(), IContract.Fragment {

    protected open var uiDelegate: IUiDelegate? = null
    override var hostActivity: IContract.Activity? = null
    override var instanceState: Bundle? = null
    private var decorView: View? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        context.let {
            uiDelegate = it as? IUiDelegate ?: DefaultUiDelegate(it)
            hostActivity = activity as? IContract.Activity
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BaseDialog(activity.checkNotNull(), getLayoutId(), getDialogThemeResId(), dialogStyle).apply {
            this@BaseDialogFragment.onDialogCreated(this, savedInstanceState)
            window?.decorView?.let { view -> onViewCreated(view, savedInstanceState) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.decorView = view
        onNewInstanceState(savedInstanceState)
        initWidget()
        initLifecycleObserver()
    }

    override fun initWidget() {}

    open val dialogStyle: Dialog.(WindowManager.LayoutParams) -> Unit = { layoutParams ->
        layoutParams.gravity = Gravity.CENTER
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
    }

    @CallSuper
    open fun onDialogCreated(dialog: Dialog, savedInstanceState: Bundle?) {

    }

    @StyleRes
    open fun getDialogThemeResId(): Int = R.style.DialogStyle_DimAmount

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        onNewInstanceState(savedInstanceState)
    }

    @CallSuper
    protected open fun onNewInstanceState(savedInstanceState: Bundle?) {
        this.instanceState = savedInstanceState
    }

    @CallSuper
    override fun onNewIntent(intent: Intent?) {}

    open fun initLifecycleObserver() {}

    override fun showMessage(msg: String?, msgResId: Int?) = uiDelegate?.showMessage(msg, msgResId)
            ?: Unit

    override fun showProgress(cancelable: Boolean) = uiDelegate?.showProgress(cancelable) ?: Unit

    override fun hideProgress() = uiDelegate?.hideProgress() ?: Unit

    open fun show(view: IView, tag: String? = null) {
        view.getSupportFragmentManager()?.let { show(it, tag) }
    }

    override fun <T : View> findViewWithId(@IdRes id: Int): T? = safelyRun {
        decorView?.findViewById(id)
    }
}

class BaseDialog(
        @NotNull context: Context,
        @LayoutRes layoutResId: Int,
        @StyleRes themeResId: Int,
        @NotNull style: Dialog.(WindowManager.LayoutParams) -> Unit
) : Dialog(context, themeResId) {

    init {
        // 加载布局
        setContentView(layoutResId)
        // 设置Dialog参数
        style.invoke(this, window?.attributes ?: WindowManager.LayoutParams())
    }

//    override fun onAttachedToWindow() {
//        super.onAttachedToWindow()
//        设置Dialog参数
//        style.invoke(this, window?.attributes ?: WindowManager.LayoutParams())
//        window?.let {
//            it.attributes = (it.attributes ?: WindowManager.LayoutParams()).apply(attributes)
//        }
//    }

}

abstract class BaseVMDialogFragment<VM : BaseViewModel> : BaseDialogFragment(), IContract.VMFragment<VM> {

    override lateinit var viewModel: VM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = getViewModelClass()?.let { ViewModelProviders.of(this).get(it) }
                .checkNotNull { "${BaseVMDialogFragment::class.java.canonicalName} error : Create viewModel exception." }
                .apply {
                    msgChannel.observe(this@BaseVMDialogFragment, Observer { showMessage(msg = it) })
                    loadingChannel.observe(this@BaseVMDialogFragment, Observer { show ->
                        if (show) showProgress()
                        else hideProgress()
                    })
                }
    }

    @CallSuper
    override fun initLifecycleObserver() {
        super.initLifecycleObserver()
        lifecycle.addObserver(viewModel)
    }

}
