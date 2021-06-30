package com.zenchn.djilearndemo.task

import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.zenchn.api.entity.AircraftLiveInfoEntity
import com.zenchn.api.service.liveUploadService
import com.zenchn.api.service.submitLiveInfo
import com.zenchn.common.utils.DateUtils
import com.zenchn.common.utils.LoggerKit
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import kotlinx.coroutines.launch
import java.util.*

/**
 * @author:Hzj
 * @date  :2021/6/22
 * desc  ：TimerTask 上报飞机信息,TimerTask每次启动需要重新创建，TimerTask和Timer每次cancel()后需要就废弃
 * record：
 */
class LiveUploadAircraftInfoTask(
    private val liveId: String,
    private val lifecycleScope: LifecycleCoroutineScope
) : TimerTask() {
    private val TAG = this.javaClass.simpleName
    private var mAircraftBattery = 0//缓存飞机电量
    private val mAircraft by lazy { DJISDKManager.getInstance().product as? Aircraft }

    init {
        mAircraft?.apply {
            //获取飞行器电量
            this.battery?.setStateCallback { batteryState ->
                //回调子线程
                Log.d(TAG, "aircraftBattery:${batteryState.chargeRemainingInPercent}")
                mAircraftBattery = batteryState.chargeRemainingInPercent
            }
            //获取遥控器电量
            this.remoteController?.setChargeRemainingCallback { batteryState ->
                //回调当前线程
                LoggerKit.d("remoteControllerBattery:${batteryState.remainingChargeInPercent}")
            }
        }
    }

    override fun run() {
        val aircraftBaseInfo = getAircraftBaseInfoAndUpload()?.apply {
            battery = mAircraftBattery
        }
        if (aircraftBaseInfo?.lat?.isNaN() == true || aircraftBaseInfo?.lon?.isNaN() == true) {
            //坐标无效就不上传
            return
        }
        LoggerKit.d("开始上报任务了:$aircraftBaseInfo")
        //调用上传信息接口
        lifecycleScope.launch {
            aircraftBaseInfo?.let {
                liveUploadService.submitLiveInfo(aircraftBaseInfo)
            }
        }
    }

    override fun cancel(): Boolean {
        mAircraft?.battery?.setStateCallback(null)
        return super.cancel()
    }

    // 主动获取无人机传感器信息
    private fun getAircraftBaseInfoAndUpload(): AircraftLiveInfoEntity? {
        mAircraft?.apply {
            val state = this.flightController.state
            return AircraftLiveInfoEntity(
                status = if (state.isFlying) 1 else 0,
                lat = state.aircraftLocation.latitude,
                lon = state.aircraftLocation.longitude,
                altitude = state.aircraftLocation.altitude,
                velocityX = state.velocityX,
                velocityY = state.velocityY,
                velocityZ = state.velocityZ,
                flightTimeInSeconds = state.flightTimeInSeconds,
                currentTime = DateUtils.dateFormat(Date()).orEmpty(),
                pushFlowId = liveId
            )
        }
        return null
    }
}
