package com.zenchn.djilearndemo.ui

import android.content.Intent
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import com.jacky.support.setOnAntiShakeClickListener
import com.jacky.support.utils.LoggerKit
import com.zenchn.djilearndemo.R
import com.zenchn.djilearndemo.app.ApplicationKit
import com.zenchn.djilearndemo.base.BaseActivity
import com.zenchn.djilearndemo.base.viewClickListener
import com.zenchn.djilearndemo.base.viewExt
import dji.common.camera.SettingsDefinitions
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import dji.sdk.sdkmanager.LiveStreamManager
import dji.ux.widget.FPVWidget
import kotlinx.coroutines.*


class MainActivity : BaseActivity() {

    private var mIsStartStream = false

    private val mLiveStreamManager by lazy { DJISDKManager.getInstance().liveStreamManager }
    private val listener: LiveStreamManager.OnLiveChangeListener =
        LiveStreamManager.OnLiveChangeListener { status -> LoggerKit.d("liveStatus:$status") }

    private var mCurrentMode = SettingsDefinitions.CameraMode.SHOOT_PHOTO

    override fun getLayoutId(): Int = R.layout.activity_main

    override fun initWidget() {
        getAircraftLocation()
        viewClickListener(R.id.btn_mediaManager) {
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
                    if (mLiveStreamManager.isStreaming) {
                        showMessage("已经在推流了")
                        return@setOnAntiShakeClickListener
                    }
                    startLive()
                    showMessage("开始推流")
                }
                mIsStartStream = mIsStartStream.not()
                LoggerKit.d("isStreaming:${mLiveStreamManager.isStreaming}")
                text = if (mIsStartStream) "关闭RTMP推流" else "开启RTMP推流"
            }
        }
        viewClickListener(R.id.btn_info) {
            showInfo()
        }
        viewClickListener(R.id.btn_waypoint) {
            startActivity(Intent(this, WaypointActivity::class.java))
        }
        viewClickListener(R.id.btn_set_mode) {
            mCurrentMode = if (mCurrentMode == SettingsDefinitions.CameraMode.SHOOT_PHOTO) {
                SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD
            } else {
                SettingsDefinitions.CameraMode.SHOOT_PHOTO
            }
            ApplicationKit.getCameraInstance()?.setMode(mCurrentMode) { error ->
                showMessage("set mode " + if (error == null) "Successfully" else error.description)
                LoggerKit.d("camera Mode:$mCurrentMode")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mLiveStreamManager.registerListener(listener)
        //重新设置视频源，防止画面卡死
        viewExt<FPVWidget>(R.id.fpv) {
            this.videoSource = FPVWidget.VideoSource.AUTO
        }
    }

    private fun startLive() {
        lifecycleScope.launch(Dispatchers.IO) {
            val rtmpUrl = "rtmp://192.168.1.208:1935/hls/nb"
            mLiveStreamManager.apply {
                liveUrl = rtmpUrl
            }
            val startStream = mLiveStreamManager.startStream()
            LoggerKit.d("startStream:$startStream")
            mLiveStreamManager.setStartTime()
        }
    }

    private fun stopLive() {
        mLiveStreamManager.stopStream()
    }

    private fun showInfo() {
        val sb = StringBuilder()
        sb.append("Video BitRate:").append(mLiveStreamManager.liveVideoBitRate)
            .append(" kpbs\n")
        sb.append("Audio BitRate:").append(mLiveStreamManager.liveAudioBitRate)
            .append(" kpbs\n")
        sb.append("Video FPS:").append(mLiveStreamManager.liveVideoFps).append("\n")
        sb.append("Video Cache size:").append(mLiveStreamManager.liveVideoCacheSize)
            .append(" frame")
        showMessage(sb.toString())
    }

    // 主动获取无人机定位信息
    private fun getAircraftLocation() {
        lifecycleScope.launch(Dispatchers.IO) {
            for (i in 0 until 11) {
                val aircraft = DJISDKManager.getInstance().product as? Aircraft
                val state = aircraft?.flightController?.state
                LoggerKit.d(
                    "$i aircraft Info ---altitude:${state?.aircraftLocation?.altitude}" + "latitude:${state?.aircraftLocation?.latitude}" +
                            "longitude:${state?.aircraftLocation?.longitude}"
                )
            }
        }
    }

    override fun onDestroy() {
        mLiveStreamManager.apply {
            if (isStreaming) {
                stopStream()
            }
            unregisterListener(listener)
        }
        super.onDestroy()
    }

}