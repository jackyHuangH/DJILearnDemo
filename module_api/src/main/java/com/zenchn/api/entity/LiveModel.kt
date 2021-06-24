package com.zenchn.api.entity

import java.util.*

/**
 * @author:Hzj
 * @date  :2021/6/22
 * desc  ：无人机直播上传数据
 * record：
 */
data class LiveAircraftInfoEntity(
    var isFlying: Boolean,//飞机是否已起飞
    var lat: Double,//纬度
    var lon: Double,//经度
    var altitude: Float,//高度
    var velocityZ: Float,//垂直速度
    var velocityX: Float,//X轴速度,前进后腿
    var velocityY: Float,//Y轴速度，左右
    var flightTimeInSeconds: Int,//起飞时间，单位s，从起飞起算
    var currentTime: Date,//取当前上传时间
    var aircraftBattery: Int = 0//飞机电量
)