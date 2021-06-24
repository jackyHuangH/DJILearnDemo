package com.zenchn.djilearndemo.task

import android.util.Log
import com.zenchn.api.entity.LiveAircraftInfoEntity
import com.zenchn.common.utils.LoggerKit
import dji.common.error.DJIError
import dji.common.util.CommonCallbacks
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import java.util.*

/**
 * @author:Hzj
 * @date  :2021/6/22
 * desc  ：TimerTask 上报飞机信息
 * record：todo 接口上报飞行数据
 */
class LiveUploadAircraftInfoTask : TimerTask() {
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
            //获取无人机设备序列号
            this.flightController?.getSerialNumber(object : CommonCallbacks.CompletionCallbackWith<String> {
                override fun onSuccess(p0: String?) {
                    //回调子线程
                    LoggerKit.d("serialNum:$p0")
                }

                override fun onFailure(error: DJIError?) {
                    Log.e("Main", error?.description.orEmpty())
                }

            })
            //获取遥控器电量
            this.remoteController?.setChargeRemainingCallback { batteryState ->
                //回调当前线程
                LoggerKit.d("remoteControllerBattery:${batteryState.remainingChargeInPercent}")
            }
        }
    }

    override fun run() {
        val aircraftBaseInfo = getAircraftBaseInfoAndUpload()?.apply {
            aircraftBattery = mAircraftBattery
        }
        LoggerKit.d("开始上报任务了:$aircraftBaseInfo")
        //todo 调用上传信息接口
    }

    override fun cancel(): Boolean {
        mAircraft?.battery?.setStateCallback(null)
        return super.cancel()
    }

    // 主动获取无人机传感器信息
    private fun getAircraftBaseInfoAndUpload(): LiveAircraftInfoEntity? {
        mAircraft?.apply {
            val state = this.flightController.state
            return LiveAircraftInfoEntity(
                isFlying = state.isFlying,
                lat = state.aircraftLocation.latitude,
                lon = state.aircraftLocation.longitude,
                altitude = state.aircraftLocation.altitude,
                velocityX = state.velocityX,
                velocityY = state.velocityY,
                velocityZ = state.velocityZ,
                flightTimeInSeconds = state.flightTimeInSeconds,
                currentTime = Date()
            )
        }
        return null
    }
}
