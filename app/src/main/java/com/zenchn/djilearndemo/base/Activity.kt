package com.zenchn.djilearndemo.base

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.zenchn.common.ext.checkNotNull
import com.zenchn.common.ext.reverseForEach
import com.zenchn.common.ext.safelyRun
import com.zenchn.djilearndemo.R
import com.zenchn.djilearndemo.app.GlobalLifecycleObserver
import com.zenchn.widget.DefaultUiDelegate
import com.zenchn.widget.IImmersionBar
import com.zenchn.widget.IUiDelegate
import com.zenchn.widget.initImmersionBar

abstract class BaseActivity : AppCompatActivity(), IContract.Activity, IImmersionBar,
    IBackPressedOwner by BackPressedOwner() {

    protected open val uiDelegate: IUiDelegate by lazy { DefaultUiDelegate(this) }

    override var instanceState: Bundle? = null

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onNewInstanceState(savedInstanceState)
        initContentView()
        initStatusBar()
        initWidget()
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

    override fun getContext(): Context = this.applicationContext

    private fun initContentView() = getLayoutId().takeIf { it > 0 }?.let { setContentView(it) }

    @CallSuper
    open fun initLifecycleObserver() {
        lifecycle.apply {
            addObserver(GlobalLifecycleObserver.INSTANCE)
        }
    }

    override fun initStatusBar() = initImmersionBar()

    override fun showMessage(msg: String?, msgResId: Int?) = uiDelegate.showMessage(msg, msgResId)

    override fun showProgress(cancelable: Boolean) = uiDelegate.showProgress(cancelable)

    override fun hideProgress() = uiDelegate.hideProgress()

    override fun <T : View> findViewWithId(id: Int): T? {
        return safelyRun { delegate.findViewById(id) }
    }

    /**
     * 建议在onBackPressedEvent()处理back事件
     */
    @CallSuper
    override fun onBackPressed() {
        if (dispatchBackPressedEvent(this)) return
        onBackPressedEvent()
    }

    open fun onBackPressedEvent() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

}

interface IBackPressedOwner {

    fun dispatchBackPressedEvent(activity: Activity): Boolean

    fun addBackPressedObserver(observer: IBackPressedObserver)

    fun removeBackPressedObserver(observer: IBackPressedObserver)

}

interface IBackPressedObserver {

    fun onBackPressedEvent(activity: Activity): Boolean

}

class BackPressedOwner : IBackPressedOwner {

    private var observers: ArrayList<IBackPressedObserver>? = null

    override fun addBackPressedObserver(observer: IBackPressedObserver) {
        if (observers == null) observers = ArrayList()
        observers?.remove(observer)
        observers?.add(observer)
    }

    override fun removeBackPressedObserver(observer: IBackPressedObserver) {
        observers?.remove(observer)
    }

    override fun dispatchBackPressedEvent(activity: Activity): Boolean {
        observers?.reverseForEach {
            if (it.onBackPressedEvent(activity)) {
                return true
            }
        }
        return false
    }
}

abstract class BaseNfcActivity : BaseActivity() {

    private var nfcAdapter: NfcAdapter? = null

    private val pendingIntent: PendingIntent by lazy {
        PendingIntent.getActivity(this, 0, Intent(this, this::class.java), 0)
    }

    @CallSuper
    override fun onStart() {
        super.onStart()
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        when {
            nfcAdapter == null -> showMessage(msgResId = R.string.common_msg_unsupport_nfc)

            // 支持NFC，但未开启，根据包名打开对应的设置界面
            nfcAdapter?.isEnabled != true -> {
                showMessage(msgResId = R.string.common_msg_request_enable_nfc)
                val intent = Intent(Settings.ACTION_NFC_SETTINGS)
                startActivity(intent)
            }

            //设置处理优于所有其他NFC的处理
            else -> {
                nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
            }

        }
    }

    @CallSuper
    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    @CallSuper
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        safelyRun {
            intent?.getParcelableExtra<Tag>(BaseNFCViewModel.NFC_EXTRA_TAG)?.let { tag -> onNfcReceived(tag) }
        }
    }

    abstract fun onNfcReceived(tag: Tag)

}

abstract class BaseVMActivity<VM : BaseViewModel> : BaseActivity(), IContract.VMActivity<VM> {

    override lateinit var viewModel: VM

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel = getViewModelClass()?.let { ViewModelProviders.of(this).get(it) }
            .checkNotNull { "${BaseVMActivity::class.java.canonicalName} error : Create viewModel exception." }
            .apply {
                msgChannel.observe(this@BaseVMActivity, Observer { showMessage(msg = it) })
                loadingChannel.observe(this@BaseVMActivity, Observer { show ->
                    if (show) showProgress()
                    else hideProgress()
                })
            }
        super.onCreate(savedInstanceState)
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
        supportFragmentManager.fragments.forEach {
            if (it is IContract.Fragment) it.onNewIntent(intent)
        }
    }

}
