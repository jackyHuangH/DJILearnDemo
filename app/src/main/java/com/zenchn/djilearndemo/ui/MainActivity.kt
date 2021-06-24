package com.zenchn.djilearndemo.ui

import android.content.Intent
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import com.zenchn.common.utils.LoggerKit
import com.zenchn.djilearndemo.R
import com.zenchn.djilearndemo.app.ApplicationKit
import com.zenchn.djilearndemo.base.BaseActivity
import com.zenchn.djilearndemo.task.LiveUploadAircraftInfoTask
import com.zenchn.widget.setOnAntiShakeClickListener
import com.zenchn.widget.viewClickListenerExt
import com.zenchn.widget.viewExt
import dji.sdk.sdkmanager.DJISDKManager
import dji.sdk.sdkmanager.LiveStreamManager
import dji.ux.widget.FPVWidget
import kotlinx.coroutines.*
import java.util.*


class MainActivity : BaseActivity() {

    private var mIsStartStream = false

    private val mLiveStreamManager by lazy { DJISDKManager.getInstance().liveStreamManager }
    private val mLiveInfoTask by lazy { LiveUploadAircraftInfoTask() }
    private val listener: LiveStreamManager.OnLiveChangeListener =
        LiveStreamManager.OnLiveChangeListener { status -> LoggerKit.d("liveStatus:$status") }

    override fun getLayoutId(): Int = R.layout.activity_main

    override fun initWidget() {
        viewClickListenerExt(R.id.btn_mediaManager) {
            startActivity(Intent(this, MediaManageActivity::class.java))
        }
        viewExt<Button>(R.id.btn_start_rtmp) {
            setOnAntiShakeClickListener {
                if (ApplicationKit.aircraftIsConnect().not()) {
                    showMessage("飞机未连接")
                    return@setOnAntiShakeClickListener
                }
                if (mIsStartStream) {
                    stopLive()
                    showMessage("结束推流")
                } else {
                    if (mLiveStreamManager?.isStreaming == true) {
                        showMessage("已经在推流了")
                        return@setOnAntiShakeClickListener
                    }
                    startLive()
                    showMessage("开始推流")
                }
                mIsStartStream = mIsStartStream.not()
                text = if (mIsStartStream) "关闭RTMP推流" else "开启RTMP推流"
            }
        }
        viewClickListenerExt(R.id.btn_live_info) {
            showInfo()
        }
        viewClickListenerExt(R.id.btn_waypoint) {
            startActivity(Intent(this, WaypointActivity::class.java))
        }
        viewClickListenerExt(R.id.btn_sensor_info) {
            Timer().schedule(mLiveInfoTask, Date(), 5000)
        }
    }

    override fun onResume() {
        super.onResume()
        mLiveStreamManager?.registerListener(listener)
        //重新设置视频源，防止画面卡死
        viewExt<FPVWidget>(R.id.fpv) {
            this.videoSource = FPVWidget.VideoSource.AUTO
        }
    }

    private fun startLive() {
        lifecycleScope.launch(Dispatchers.IO) {
            val rtmpUrl = "rtmp://192.168.1.184:1935/hls/nb"
            mLiveStreamManager?.apply {
                liveUrl = rtmpUrl
            }
            val startStream = mLiveStreamManager?.startStream()
            LoggerKit.d("startStream:$startStream")
            mLiveStreamManager?.setStartTime()
        }
    }

    private fun stopLive() {
        mLiveStreamManager?.stopStream()
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
            if (isStreaming) {
                stopStream()
            }
            unregisterListener(listener)
        }
        mLiveInfoTask.cancel()
        super.onDestroy()
    }

}