package com.zenchn.api.entity

import com.google.gson.annotations.SerializedName
import java.io.File

/**
 * @author:Hzj
 * @date  :2021-06-28
 * desc  ：获取直播推流url和推流id
 * record：
 */
data class LiveFlowInfo(
    var pushFlowPrefixUrl: String,//直播推流url前缀
    var pushFlowId: String//直播推流Id
)


/**
 * @author:Hzj
 * @date  :2021/6/22
 * desc  ：无人机直播上传数据
 * record：
 */
data class AircraftLiveInfoEntity(
    var status: Int,//飞机是否已起飞状态，1起飞，0未起飞
    @SerializedName("gcjLatitude")
    var lat: Double,//纬度
    @SerializedName("gcjLongitude")
    var lon: Double,//经度
    var altitude: Float,//高度
    @SerializedName("upDownVelocity")
    var velocityZ: Float,//垂直速度
    @SerializedName("forwardVelocity")
    var velocityX: Float,//X轴速度,前进后退
    @SerializedName("leftRightVelocity")
    var velocityY: Float,//Y轴速度，左右
    @SerializedName("continueDuration")
    var flightTimeInSeconds: Int,//起飞时间，单位s，从起飞起算
    @SerializedName("trackTime")
    var currentTime: String,//取当前上传时间
    var pushFlowId: String = "",//直播推流id
    var battery: Int = 0//飞机电量
)


/**
 * @author:Hzj
 * @date  :2021/6/22
 * desc  ：无人机照片上传
 * record：
 */
data class AircraftPhotoUploadEntity(
    var photoTime: Long,//照片的时间
    var file: File,//照片文件
)