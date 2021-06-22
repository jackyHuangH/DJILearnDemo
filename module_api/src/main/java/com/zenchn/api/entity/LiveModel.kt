package com.zenchn.api.entity

/**
 * @author:Hzj
 * @date  :2021/6/22
 * desc  ：无人机直播上传数据
 * record：
 */
data class LiveUploadEntity(
    val lat:Double,//纬度
    val lon:Double,//经度
    val altitude:Float,//高度
    val velocityZ:Float,//垂直速度
    val velocityX:Float,//X轴速度,前进后腿
    val velocityY:Float,//Y轴速度，左右
    val currentTime:Long,//时间戳，取当前上传时间
    val flightTimeInSeconds:Int,//起飞时间，单位s，从起飞起算
    val aircraftBattery:Int,//飞机电量
)