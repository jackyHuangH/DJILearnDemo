package com.zenchn.djilearndemo.app

import android.Manifest
import android.app.Activity
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.core.content.ContextCompat
import androidx.multidex.MultiDexApplication
import com.hjq.toast.ToastUtils
import com.jacky.support.base.ICrashCallback
import com.jacky.support.crash.DefaultUncaughtHandler
import com.secneo.sdk.Helper
import com.zenchn.djilearndemo.model.event.AircraftConnectEvent
import com.zenchn.djilearndemo.ui.MainActivity
import dji.common.error.DJIError
import dji.common.error.DJISDKError
import dji.common.useraccount.UserAccountState
import dji.common.util.CommonCallbacks.CompletionCallbackWith
import dji.sdk.base.BaseComponent
import dji.sdk.base.BaseProduct
import dji.sdk.camera.Camera
import dji.sdk.sdkmanager.DJISDKInitEvent
import dji.sdk.sdkmanager.DJISDKManager
import dji.sdk.useraccount.UserAccountManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

/**
 * @author:Hzj
 * @date  :2021/6/2
 * desc  ：
 * record：
 */
class MyApplication : MultiDexApplication() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        //init DJI SDK
        Helper.install(this)
    }
}


object ApplicationKit {
    const val FLAG_CONNECTION_CHANGE = "uxsdk_demo_connection_change"
    const val TAG = "ApplicationKit"
    private var mProduct: BaseProduct? = null
    lateinit var mApplicationContext: Context

    fun initKit(context: Context) {
        mApplicationContext = context
        (context as Application).apply {
            clearNotify(this)
            initCrashHandler(this)
        }
        registerDji()
    }

    /**
     * 初始化crash异常处理
     */
    @CallSuper
    fun initCrashHandler(application: Application) {
        DefaultUncaughtHandler.getInstance().init(application, object : ICrashCallback {
            override fun onCrash(thread: Thread?, ex: Throwable?) {
                GlobalLifecycleObserver.INSTANCE.exitApp()
            }
        })
    }

    /**
     * 清理通知栏
     */
    private fun clearNotify(application: Application) {
        val nm = application.getSystemService(Activity.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancelAll()
    }

    private fun registerDji() {
        //Check the permissions before registering the application for android system 6.0 above.
        val permissionCheck =
            ContextCompat.checkSelfPermission(mApplicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val permissionCheck2 =
            ContextCompat.checkSelfPermission(mApplicationContext, Manifest.permission.READ_PHONE_STATE)
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || permissionCheck == 0 && permissionCheck2 == 0) {
                //This is used to start SDK services and initiate SDK.
                DJISDKManager.getInstance().registerApp(mApplicationContext, mDJISDKManagerCallback)
            } else {
                Toast.makeText(mApplicationContext, "Please check if the permission is granted.", Toast.LENGTH_LONG)
                    .show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MyApplication", e.message.orEmpty())
        }
    }

    /**
     * This function is used to get the instance of DJIBaseProduct.
     * If no product is connected, it returns null.
     */
    @Synchronized
    fun getProductInstance(): BaseProduct? {
        if (null == mProduct) {
            mProduct = DJISDKManager.getInstance().product
        }
        return mProduct
    }

    /**
     * This function is used to get the instance of DJIBaseProduct.
     * If no product is connected, it returns null.
     */
    @Synchronized
    fun getCameraInstance(): Camera? {
        return mProduct?.camera
    }

    /**
     * 飞机是否已连接
     */
    fun aircraftIsConnect(): Boolean {
        val mProduct = getProductInstance()
        if (null != mProduct && mProduct.isConnected) {
            return true
        }
        return false
    }

    private val mDJISDKManagerCallback by lazy {
        object : DJISDKManager.SDKManagerCallback {
            override fun onRegister(error: DJIError?) {
                if (error == DJISDKError.REGISTRATION_SUCCESS) {
                    //注册成功后开始连接飞机
                    DJISDKManager.getInstance().startConnectionToProduct()
                    GlobalScope.launch(Dispatchers.Main) {
                        ToastUtils.show("Register Success")
                    }
                    //登录账户
                    loginAccount()
                    notifyStatusChange()
                } else {
                    GlobalScope.launch(Dispatchers.Main) {
                        ToastUtils.show("Register Failed, check network is available")
                    }
                }
                Log.e("TAG", error.toString())
            }

            override fun onProductDisconnect() {
                Log.d("TAG", "onProductDisconnect")
                notifyStatusChange()
            }

            override fun onProductConnect(baseProduct: BaseProduct?) {
                Log.d("TAG", String.format("onProductConnect newProduct:%s", baseProduct))
                notifyStatusChange()
            }

            override fun onProductChanged(baseProduct: BaseProduct?) {
                Log.d("TAG", String.format("onProductChanged newProduct:%s", baseProduct))
                notifyStatusChange()
            }

            override fun onComponentChange(
                componentKey: BaseProduct.ComponentKey?,
                oldComponent: BaseComponent?,
                newComponent: BaseComponent?
            ) {
                newComponent?.setComponentListener { isConnected ->
                    Log.d("TAG", "onComponentConnectivityChanged: $isConnected")
                    notifyStatusChange()
                }
                Log.d(
                    "TAG", String.format(
                        "onComponentChange key:%s, oldComponent:%s, newComponent:%s",
                        componentKey,
                        oldComponent,
                        newComponent
                    )
                )
            }

            override fun onInitProcess(p0: DJISDKInitEvent?, p1: Int) {
            }

            override fun onDatabaseDownloadProgress(p0: Long, p1: Long) {
            }

        }
    }

    //登录账户
    private fun loginAccount() {
        UserAccountManager.getInstance().logIntoDJIUserAccount(
            mApplicationContext,
            object : CompletionCallbackWith<UserAccountState?> {
                override fun onSuccess(userAccountState: UserAccountState?) {
                    Log.d("TAG", "Login Success")
                }

                override fun onFailure(error: DJIError) {
                    Log.d("TAG", "Login Error:" + error.description)
                }
            })
    }

    //通知更新连接状态
    private fun notifyStatusChange() {
        EventBus.getDefault().postSticky(AircraftConnectEvent())
    }

    fun navigateToLogin(grantRefuse: Boolean) {
        if (grantRefuse) {
            ToastUtils.show("授权失败")
        }
        val topActivity = GlobalLifecycleObserver.INSTANCE.getTopActivity()
        if (topActivity != null) {
//                MainActivity.launch(topActivity)
        } else {
            mApplicationContext.let {
                val intent = Intent(it, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                it.startActivity(intent)
            }
        }
    }
}