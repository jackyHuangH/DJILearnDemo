package com.zenchn.djilearndemo.ui

import android.app.Application
import android.content.Intent
import android.util.Log
import android.widget.Button
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.zenchn.api.entity.LiveFlowInfo
import com.zenchn.api.service.liveUploadService
import com.zenchn.common.utils.LoggerKit
import com.zenchn.common.utils.PreferenceUtil
import com.zenchn.djilearndemo.BuildConfig
import com.zenchn.djilearndemo.R
import com.zenchn.djilearndemo.app.ApplicationKit
import com.zenchn.djilearndemo.base.BaseVMActivity
import com.zenchn.djilearndemo.base.BaseViewModel
import com.zenchn.djilearndemo.base.httpRequest
import com.zenchn.djilearndemo.base.shareLiveFlowInfo
import com.zenchn.djilearndemo.task.LiveUploadAircraftInfoTask
import com.zenchn.widget.*
import dji.common.error.DJIError
import dji.common.util.CommonCallbacks
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import dji.sdk.sdkmanager.LiveStreamManager
import dji.ux.widget.FPVWidget
import kotlinx.coroutines.*
import java.util.*


class MainActivity : BaseVMActivity<MainViewModel>() {

    private var mIsStartStream = false

    private val mLiveStreamManager by lazy { DJISDKManager.getInstance().liveStreamManager }
    private var mLiveInfoTask: TimerTask? = null
    private val mLiveTimer: Timer by lazy { Timer() }
    private val listener: LiveStreamManager.OnLiveChangeListener =
        LiveStreamManager.OnLiveChangeListener { status -> LoggerKit.d("liveStatus:$status") }

    private var mUploadFrequency by PreferenceUtil(PreferenceUtil.AIRCRAFT_UPLOAD_FREQ, 5L)

    override fun getLayoutId(): Int = R.layout.activity_main

    override fun onResume() {
        super.onResume()
        mLiveStreamManager?.stopStream()
        mLiveStreamManager?.registerListener(listener)
        //重新设置视频源，防止画面卡死
        viewExt<FPVWidget>(R.id.fpv) {
            this.videoSource = FPVWidget.VideoSource.AUTO
        }
    }

    override fun initWidget() {
        if (BuildConfig.DEBUG) {
            viewVisibleExt(R.id.btn_live_info, true)
            viewVisibleExt(R.id.btn_waypoint, true)
        }
        viewClickListenerExt(R.id.btn_mediaManager) {
            if (ApplicationKit.aircraftIsConnect().not()) {
                showMessage("飞机未连接")
                return@viewClickListenerExt
            }
            if (mIsStartStream) {
                showMessage("请先停止推流")
                return@viewClickListenerExt
            }
            jumpMediaCenter(false)
        }
        viewExt<Button>(R.id.btn_start_rtmp) {
            setOnAntiShakeClickListener {
                if (ApplicationKit.aircraftIsConnect().not()) {
                    showMessage("飞机未连接")
                    return@setOnAntiShakeClickListener
                }
                if (mIsStartStream) {
                    mLiveStreamManager?.stopStream()
                    showMessage("结束推流")
                    stopUploadInfoSchedule()
                } else {
                    if (mLiveStreamManager?.isStreaming == true) {
                        showMessage("已经在推流了")
                        return@setOnAntiShakeClickListener
                    }
                    //获取直播推流地址
                    viewModel.getLiveUrlInfo()
                }
                mIsStartStream = mIsStartStream.not()
                text = if (mIsStartStream) "关闭RTMP推流" else "开启RTMP推流"
                keepScreenOn = mIsStartStream
            }
        }
        viewClickListenerExt(R.id.btn_live_info) {
            showInfo()
        }
        viewClickListenerExt(R.id.btn_waypoint) {
            startActivity(Intent(this, WaypointActivity::class.java))
        }
        viewClickListenerExt(R.id.btn_set_freq) {
            if (mIsStartStream) {
                showMessage("请先停止推流")
                return@viewClickListenerExt
            }
            //设置上报信息频率
            val uploadFreq = listOf("1", "3", "5", "10", "20", "60")
            val initSelect = uploadFreq.indexOf(mUploadFrequency.toString())
            MaterialDialog(this).show {
                title(text = "定位上报频率（单位：s）")
                listItemsSingleChoice(items = uploadFreq, initialSelection = initSelect) { _, index, _ ->
                    // Invoked when the user selects an item
                    mUploadFrequency = uploadFreq[index].toLong()
                }
                lifecycleOwner(owner = this@MainActivity)
            }
        }
    }

    private fun startUploadInfoSchedule() {
        mLiveInfoTask =
            LiveUploadAircraftInfoTask(viewModel.shareLiveFlowInfo.value?.pushFlowId.orEmpty(), lifecycleScope)
        mLiveTimer.schedule(mLiveInfoTask, Date(), mUploadFrequency * 1000)
    }

    private fun stopUploadInfoSchedule() {
        mLiveInfoTask?.cancel()
        lifecycleScope.launch(Dispatchers.IO) {
            delay(500)
            //结束推流后，开启后台任务下载并上传图片
            jumpMediaCenter(true)
        }
    }

    private fun jumpMediaCenter(isUploadFile: Boolean) {
        startActivity(Intent(this, MediaManageActivity::class.java).apply {
            putExtra(MediaManageActivity.EXTRA_UPLOAD_FLAG, isUploadFile)
        })
    }

    private fun startLive() {
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.shareLiveFlowInfo.value?.let { liveInfo ->
                val rtmpUrl = "${liveInfo.pushFlowPrefixUrl}/${liveInfo.pushFlowId}"
                LoggerKit.d("liveUrl:$rtmpUrl")
                mLiveStreamManager?.apply {
                    liveUrl = rtmpUrl
                    val startStream = startStream()
                    setStartTime()
                    LoggerKit.d("startStream:$startStream")
                }
            }
        }
        showMessage("开始推流")
        startUploadInfoSchedule()
    }

    private fun showInfo() {
        val sb = StringBuilder()
        sb.append("Video BitRate:").append(mLiveStreamManager?.liveVideoBitRate)
            .append(" kpbs\n")
        sb.append("Audio BitRate:").append(mLiveStreamManager?.liveAudioBitRate)
            .append(" kpbs\n")
        sb.append("Video FPS:").append(mLiveStreamManager?.liveVideoFps).append("\n")
        sb.append("Video Cache size:").append(mLiveStreamManager?.liveVideoCacheSize)
            .append(" frame")
        showMessage(sb.toString())
    }

    override fun onDestroy() {
        mLiveStreamManager?.apply {
            stopStream()
            unregisterListener(listener)
        }
        mLiveInfoTask?.cancel()
        mLiveTimer.apply {
            cancel()
            purge()
        }
        super.onDestroy()
    }

    override val onViewModelStartup: MainViewModel.() -> Unit = {
        mLiveFlowInfo.observe(this@MainActivity) {
            startLive()
        }
    }

}

class MainViewModel(application: Application) : BaseViewModel(application) {
    val mLiveFlowInfo: MutableLiveData<LiveFlowInfo> = MutableLiveData()
    private var mSerialNumCache: String? = null

    fun getLiveUrlInfo() {
        if (mSerialNumCache.isNullOrEmpty()) {
            //获取无人机设备序列号
            (DJISDKManager.getInstance().product as? Aircraft)?.apply {
                this.flightController?.getSerialNumber(object : CommonCallbacks.CompletionCallbackWith<String> {
                    override fun onSuccess(serialNum: String?) {
                        //回调子线程
                        mSerialNumCache = serialNum
                        LoggerKit.d("serialNum:$serialNum")
                        serialNum?.let {
                            requestLiveUrl(it)
                        }
                    }

                    override fun onFailure(error: DJIError?) {
                        Log.e("Bind", error?.description.orEmpty())
                    }
                })
            }
        } else {
            mSerialNumCache?.let {
                requestLiveUrl(it)
            }
        }

    }

    private fun requestLiveUrl(serialNum: String) {
        httpRequest(
            request = { liveUploadService.askPushFlowInfo(serialNum) },
            callback = { ok, data, _ ->
                if (ok) {
                    mLiveFlowInfo.value = data.also {
                        shareLiveFlowInfo.value = it
                    }
                }
            })
    }
}