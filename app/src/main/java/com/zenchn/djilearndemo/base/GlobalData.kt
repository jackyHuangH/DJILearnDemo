package com.zenchn.djilearndemo.base

import androidx.lifecycle.MutableLiveData

/**
 * @author:Hzj
 * @date  :2020/9/23
 * desc  ：全局共享数据管理
 * record：
 */

//--------------------全局共享的数据------------------------------
/**
 * 是否展示设备marker,默认显示
 */
val BaseViewModel.showDeviceMarker: MutableLiveData<Boolean> by lazy { MutableLiveData(true) }