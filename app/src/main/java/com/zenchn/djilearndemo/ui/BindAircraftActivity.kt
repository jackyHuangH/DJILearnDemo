package com.zenchn.djilearndemo.ui

import android.Manifest
import android.app.Application
import android.content.Intent
import android.util.Log
import android.widget.TextView
import com.hjq.toast.ToastUtils
import com.zenchn.common.frame.IPermission
import com.zenchn.common.frame.applySelfPermissions
import com.zenchn.djilearndemo.R
import com.zenchn.djilearndemo.app.ApplicationKit
import com.zenchn.djilearndemo.base.BaseVMActivity
import com.zenchn.djilearndemo.base.BaseViewModel
import com.zenchn.djilearndemo.event.AircraftConnectEvent
import com.zenchn.widget.viewClickListenerExt
import com.zenchn.widget.viewEnabledExt
import com.zenchn.widget.viewExt
import dji.common.error.DJIError
import dji.common.realname.AircraftBindingState.AircraftBindingStateListener
import dji.common.realname.AppActivationState.AppActivationStateListener
import dji.common.useraccount.UserAccountState
import dji.common.util.CommonCallbacks.CompletionCallbackWith
import dji.sdk.realname.AppActivationManager
import dji.sdk.sdkmanager.DJISDKManager
import dji.sdk.useraccount.UserAccountManager
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


/**
 * @author:Hzj
 * @date  :2021/6/3
 * desc  ：无人机账户绑定
 * record：
 */
class BindAircraftActivity : BaseVMActivity<BindAircraftViewModel>(), IPermission {

    private var appActivationManager: AppActivationManager? = null
    private val activationStateListener: AppActivationStateListener = AppActivationStateListener { appActivationState ->
        runOnUiThread {
            Log.d("Bind", "activateState:$appActivationState")
            viewExt<TextView>(R.id.tv_activation_state_info) { text = "$appActivationState" }
        }
    }
    private var bindingStateListener: AircraftBindingStateListener = AircraftBindingStateListener { bindingState ->
        runOnUiThread {
            Log.d("Bind", "bindState:$bindingState")
            viewExt<TextView>(R.id.tv_binding_state_info) { text = "$bindingState" }
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_bind_aircraft

    override fun initWidget() {
        EventBus.getDefault().register(this)
        checkPermissions()
        initData()
    }

    private fun initData() {
        viewClickListenerExt(R.id.btn_login) {
            loginAccount()
        }
        viewClickListenerExt(R.id.btn_logout) {
            logoutAccount()
        }
        viewClickListenerExt(R.id.btn_go_main) {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onConnectivityChange(event: AircraftConnectEvent?) {
        event?.let {
            appActivationManager = DJISDKManager.getInstance().appActivationManager?.apply {
                addAppActivationStateListener(activationStateListener)
                addAircraftBindingStateListener(bindingStateListener)
                viewExt<TextView>(R.id.tv_binding_state_info) { text = "$aircraftBindingState" }
                viewExt<TextView>(R.id.tv_activation_state_info) { text = "$appActivationState" }
                viewEnabledExt(R.id.btn_go_main, true)
            }
        }
    }

    private fun tearDownListener() {
        appActivationManager?.apply {
            removeAppActivationStateListener(activationStateListener)
            removeAircraftBindingStateListener(bindingStateListener)
        }
        viewExt<TextView>(R.id.tv_activation_state_info) { text = "Unknown" }
        viewExt<TextView>(R.id.tv_binding_state_info) { text = "Unknown" }
    }

    private fun loginAccount() {
        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
            object : CompletionCallbackWith<UserAccountState?> {
                override fun onSuccess(userAccountState: UserAccountState?) {
                    ToastUtils.show("Login Success")
                }

                override fun onFailure(error: DJIError) {
                    ToastUtils.show(
                        "Login Error:"
                                + error.description
                    )
                }
            })
    }

    private fun logoutAccount() {
        UserAccountManager.getInstance().logoutOfDJIUserAccount { error ->
            if (null == error) {
                ToastUtils.show("Logout Success")
            } else {
                showMessage(
                    ("Logout Error:"
                            + error.description)
                )
            }
        }
    }

    private fun checkPermissions() {
        // When the compile and target version is higher than 22, please request the
        // following permissions at runtime to ensure the
        // SDK work well.
        applySelfPermissions(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO
        ) { list, granted ->
            ApplicationKit.registerDji()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tearDownListener()
        EventBus.getDefault().unregister(this)
    }

    override val onViewModelStartup: BindAircraftViewModel.() -> Unit = {

    }
}

class BindAircraftViewModel(application: Application) : BaseViewModel(application) {

}